package net.simplehardware.engine.server.database.repositories;

import net.simplehardware.engine.server.database.DatabaseManager;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Repository interface for Admin Metrics operations
 */
public interface MetricsRepository {
    /**
     * Record a metric
     */
    void recordMetric(String metricType, double value, String metadata) throws SQLException;

    /**
     * Get metrics history
     */
    List<Map<String, Object>> getMetricsHistory(String metricType, int hours) throws SQLException;

    /**
     * Get game statistics for a period
     */
    Map<String, Object> getGameStatistics(String period) throws SQLException;

    /**
     * Get user registration statistics for a period
     */
    Map<String, Object> getUserRegistrationStats(String period) throws SQLException;

    /**
     * Get database statistics
     */
    Map<String, Object> getDatabaseStats() throws SQLException;

    /**
     * Get average wait times by difficulty
     */
    List<Map<String, Object>> getAverageWaitTimesByDifficulty() throws SQLException;

    /**
     * Get leaderboard
     */
    List<DatabaseManager.LeaderboardEntry> getLeaderboard(int limit) throws SQLException;

    /**
     * Get leaderboard by difficulty
     */
    List<DatabaseManager.LeaderboardEntry> getLeaderboardByDifficulty(String difficulty, int limit) throws SQLException;

    /**
     * Cleanup old metrics
     */
    void cleanOldMetrics(int daysToKeep) throws SQLException;

    /**
     * Vacuum database
     */
    void vacuumDatabase() throws SQLException;
}
