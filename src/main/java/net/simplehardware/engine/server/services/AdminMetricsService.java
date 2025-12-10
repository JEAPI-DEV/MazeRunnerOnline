package net.simplehardware.engine.server.services;

import net.simplehardware.engine.server.database.DatabaseManager;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;

public class AdminMetricsService {
    private final DatabaseManager db;
    private final ScheduledExecutorService scheduler;
    private final Map<String, Long> requestCounts;
    private final Map<String, Long> cacheHits;
    private final Map<String, Long> cacheMisses;
    private final Map<String, List<Long>> responseTimes;
    private final Object metricsLock = new Object();
    private volatile long lastRecordTime = System.currentTimeMillis();

    public AdminMetricsService(DatabaseManager db) {
        this.db = db;
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.requestCounts = new ConcurrentHashMap<>();
        this.cacheHits = new ConcurrentHashMap<>();
        this.cacheMisses = new ConcurrentHashMap<>();
        this.responseTimes = new ConcurrentHashMap<>();
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::collectSystemMetrics, 0, 30, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(this::flushMetrics, 5, 60, TimeUnit.SECONDS);
    }

    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }

    public void recordRequest(String endpoint, long responseTimeMs) {
        synchronized (metricsLock) {
            requestCounts.merge(endpoint, 1L, Long::sum);
            responseTimes.computeIfAbsent(endpoint, k -> new ArrayList<>()).add(responseTimeMs);
        }
    }

    public void recordCacheHit(String cacheType) {
        cacheHits.merge(cacheType, 1L, Long::sum);
    }

    public void recordCacheMiss(String cacheType) {
        cacheMisses.merge(cacheType, 1L, Long::sum);
    }

    private void collectSystemMetrics() {
        try {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

            double cpuLoad = osBean.getSystemLoadAverage();
            if (cpuLoad >= 0) {
                db.recordMetric("cpu_load", cpuLoad, null);
            }

            long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
            long maxMemory = memoryBean.getHeapMemoryUsage().getMax();
            double memoryPercent = maxMemory > 0 ? (double) usedMemory / maxMemory * 100 : 0;
            db.recordMetric("memory_usage_percent", memoryPercent, null);
            db.recordMetric("memory_usage_bytes", usedMemory, null);

        } catch (SQLException e) {
            System.err.println("Failed to record system metrics: " + e.getMessage());
        }
    }

    private void flushMetrics() {
        try {
            long now = System.currentTimeMillis();
            long timeDiff = now - lastRecordTime;
            lastRecordTime = now;

            synchronized (metricsLock) {
                long totalRequests = requestCounts.values().stream().mapToLong(Long::longValue).sum();
                if (totalRequests > 0) {
                    double rps = (double) totalRequests / (timeDiff / 1000.0);
                    db.recordMetric("requests_per_second", rps, null);
                }

                for (Map.Entry<String, List<Long>> entry : responseTimes.entrySet()) {
                    if (!entry.getValue().isEmpty()) {
                        double avgTime = entry.getValue().stream()
                                .mapToLong(Long::longValue)
                                .average()
                                .orElse(0);
                        db.recordMetric("avg_response_time", avgTime, entry.getKey());
                    }
                }

                long totalCacheHits = cacheHits.values().stream().mapToLong(Long::longValue).sum();
                long totalCacheMisses = cacheMisses.values().stream().mapToLong(Long::longValue).sum();
                long totalCacheAccess = totalCacheHits + totalCacheMisses;

                if (totalCacheAccess > 0) {
                    double hitRate = (double) totalCacheHits / totalCacheAccess * 100;
                    db.recordMetric("cache_hit_rate", hitRate, null);
                }

                requestCounts.clear();
                responseTimes.clear();
            }

        } catch (SQLException e) {
            System.err.println("Failed to flush metrics: " + e.getMessage());
        }
    }

    public Map<String, Object> getCurrentMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        try {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

            metrics.put("cpu_load", osBean.getSystemLoadAverage());
            metrics.put("available_processors", osBean.getAvailableProcessors());
            
            long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
            long maxMemory = memoryBean.getHeapMemoryUsage().getMax();
            metrics.put("memory_used", usedMemory);
            metrics.put("memory_max", maxMemory);
            metrics.put("memory_percent", maxMemory > 0 ? (double) usedMemory / maxMemory * 100 : 0);

            synchronized (metricsLock) {
                long totalRequests = requestCounts.values().stream().mapToLong(Long::longValue).sum();
                metrics.put("active_requests", totalRequests);

                long totalCacheHits = cacheHits.values().stream().mapToLong(Long::longValue).sum();
                long totalCacheMisses = cacheMisses.values().stream().mapToLong(Long::longValue).sum();
                long totalCacheAccess = totalCacheHits + totalCacheMisses;
                
                if (totalCacheAccess > 0) {
                    metrics.put("cache_hit_rate", (double) totalCacheHits / totalCacheAccess * 100);
                } else {
                    metrics.put("cache_hit_rate", 0.0);
                }
            }

        } catch (Exception e) {
            System.err.println("Error getting current metrics: " + e.getMessage());
        }

        return metrics;
    }
}
