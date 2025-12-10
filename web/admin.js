let dashboardData = null;
let currentPeriod = 'daily';
let refreshInterval = null;

async function loadAdminDashboard() {
    try {
        const response = await fetch('/api/admin/dashboard', {
            headers: {
                'Authorization': `Bearer ${getAuthToken()}`
            }
        });

        if (!response.ok) {
            throw new Error('Failed to load dashboard data');
        }

        dashboardData = await response.json();
        updateDashboard();
    } catch (error) {
        console.error('Error loading dashboard:', error);
        showError('Failed to load admin dashboard');
    }
}

async function loadSystemStatus() {
    try {
        const response = await fetch('/api/admin/system/status', {
            headers: {
                'Authorization': `Bearer ${getAuthToken()}`
            }
        });

        if (!response.ok) return;

        const data = await response.json();
        updateSystemStatus(data);
    } catch (error) {
        console.error('Error loading system status:', error);
    }
}

function updateDashboard() {
    if (!dashboardData) return;

    updateDatabaseStats(dashboardData.database_stats);
    updateGameStats(dashboardData.game_statistics[currentPeriod]);
    updateUserStats(dashboardData.user_statistics);
    updateWaitTimes(dashboardData.wait_times_by_difficulty);
    updateSystemStatus(dashboardData.current_metrics);
}

function updateSystemStatus(metrics) {
    const cpuLoad = metrics.cpu_load >= 0 ? metrics.cpu_load.toFixed(2) : 'N/A';
    document.getElementById('cpuLoad').textContent = cpuLoad;

    const memoryPercent = metrics.memory_percent ? metrics.memory_percent.toFixed(1) : 0;
    const memoryUsed = metrics.memory_used ? formatBytes(metrics.memory_used) : '0 MB';
    const memoryMax = metrics.memory_max ? formatBytes(metrics.memory_max) : '0 MB';
    document.getElementById('memoryUsage').textContent = `${memoryPercent}% (${memoryUsed} / ${memoryMax})`;

    const cacheHitRate = metrics.cache_hit_rate ? metrics.cache_hit_rate.toFixed(1) + '%' : 'N/A';
    document.getElementById('cacheHitRate').textContent = cacheHitRate;

    const activeRequests = metrics.active_requests || 0;
    document.getElementById('activeRequests').textContent = activeRequests;
}

function updateDatabaseStats(stats) {
    document.getElementById('totalUsers').textContent = stats.users_count || 0;
    document.getElementById('totalGames').textContent = stats.game_results_count || 0;
    document.getElementById('totalBots').textContent = stats.player_bots_count || 0;
    
    const dbSize = stats.db_size_bytes ? formatBytes(stats.db_size_bytes) : '0 MB';
    document.getElementById('dbSize').textContent = dbSize;
}

function updateGameStats(stats) {
    if (!stats) return;

    document.getElementById('periodTotalGames').textContent = stats.total_games || 0;
    document.getElementById('periodCompletedGames').textContent = stats.completed_games || 0;
    document.getElementById('periodAvgScore').textContent = 
        stats.avg_score ? stats.avg_score.toFixed(1) + '%' : 'N/A';
    document.getElementById('periodAvgSteps').textContent = 
        stats.avg_steps ? Math.round(stats.avg_steps) : 'N/A';

    const difficultyDiv = document.getElementById('difficultyBreakdown');
    if (stats.by_difficulty) {
        let html = '<h3>By Difficulty</h3><div class="difficulty-stats">';
        for (const [difficulty, data] of Object.entries(stats.by_difficulty)) {
            html += `
                <div class="difficulty-item">
                    <strong>${difficulty}</strong>: 
                    ${data.count} games, 
                    Avg: ${data.avg_score ? data.avg_score.toFixed(1) : 0}%
                </div>
            `;
        }
        html += '</div>';
        difficultyDiv.innerHTML = html;
    }
}

function updateUserStats(stats) {
    if (!stats) return;

    document.getElementById('weeklyUsers').textContent = 
        stats.weekly ? stats.weekly.new_users : 0;
    document.getElementById('monthlyUsers').textContent = 
        stats.monthly ? stats.monthly.new_users : 0;
    document.getElementById('totalRegistered').textContent = 
        stats.monthly ? stats.monthly.total_users : 0;
}

function updateWaitTimes(waitTimes) {
    const waitTimesDiv = document.getElementById('waitTimes');
    
    if (!waitTimes || waitTimes.length === 0) {
        waitTimesDiv.innerHTML = '<p>No wait time data available</p>';
        return;
    }

    let html = '';
    waitTimes.forEach(wt => {
        const seconds = wt.avg_wait_seconds || 0;
        const minutes = (seconds / 60).toFixed(1);
        html += `
            <div class="status-item">
                <span class="status-label">${wt.difficulty}:</span>
                <span class="status-value">${minutes} min (${wt.game_count} games)</span>
            </div>
        `;
    });
    waitTimesDiv.innerHTML = html;
}

async function performDatabaseAction(action, days = null) {
    try {
        const body = { action };
        if (days) body.days = days;

        const response = await fetch('/api/admin/database/manage', {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${getAuthToken()}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(body)
        });

        const result = await response.json();
        
        if (response.ok && result.success) {
            showActionResult(result.message, 'success');
            loadAdminDashboard();
        } else {
            showActionResult(result.error || 'Action failed', 'error');
        }
    } catch (error) {
        console.error('Error performing action:', error);
        showActionResult('Action failed: ' + error.message, 'error');
    }
}

function showActionResult(message, type) {
    const resultDiv = document.getElementById('actionResult');
    resultDiv.textContent = message;
    resultDiv.className = `action-result ${type}`;
    resultDiv.style.display = 'block';
    
    setTimeout(() => {
        resultDiv.style.display = 'none';
    }, 5000);
}

async function loadMetricsHistory() {
    const metricType = document.getElementById('metricTypeSelect').value;
    const hours = document.getElementById('timeRangeSelect').value;

    try {
        const response = await fetch(`/api/admin/metrics/history?type=${metricType}&hours=${hours}`, {
            headers: {
                'Authorization': `Bearer ${getAuthToken()}`
            }
        });

        if (!response.ok) return;

        const data = await response.json();
        renderChart(data, metricType);
    } catch (error) {
        console.error('Error loading metrics history:', error);
    }
}

function renderChart(data, metricType) {
    const canvas = document.getElementById('chartCanvas');
    const ctx = canvas.getContext('2d');
    
    canvas.width = canvas.offsetWidth;
    canvas.height = 300;

    ctx.clearRect(0, 0, canvas.width, canvas.height);

    if (!data || data.length === 0) {
        ctx.fillStyle = '#666';
        ctx.font = '16px Arial';
        ctx.textAlign = 'center';
        ctx.fillText('No data available', canvas.width / 2, canvas.height / 2);
        return;
    }

    data.reverse();

    const values = data.map(d => d.value);
    const maxValue = Math.max(...values);
    const minValue = Math.min(...values);
    const range = maxValue - minValue || 1;

    const padding = 40;
    const chartWidth = canvas.width - padding * 2;
    const chartHeight = canvas.height - padding * 2;

    ctx.strokeStyle = '#4CAF50';
    ctx.lineWidth = 2;
    ctx.beginPath();

    data.forEach((point, index) => {
        const x = padding + (index / (data.length - 1)) * chartWidth;
        const y = padding + chartHeight - ((point.value - minValue) / range) * chartHeight;
        
        if (index === 0) {
            ctx.moveTo(x, y);
        } else {
            ctx.lineTo(x, y);
        }
    });

    ctx.stroke();

    ctx.strokeStyle = '#ddd';
    ctx.lineWidth = 1;
    ctx.beginPath();
    ctx.moveTo(padding, padding);
    ctx.lineTo(padding, canvas.height - padding);
    ctx.lineTo(canvas.width - padding, canvas.height - padding);
    ctx.stroke();

    ctx.fillStyle = '#666';
    ctx.font = '12px Arial';
    ctx.textAlign = 'right';
    ctx.fillText(maxValue.toFixed(2), padding - 5, padding);
    ctx.fillText(minValue.toFixed(2), padding - 5, canvas.height - padding);

    ctx.textAlign = 'center';
    if (data.length > 0) {
        const firstTime = new Date(data[0].timestamp).toLocaleTimeString();
        const lastTime = new Date(data[data.length - 1].timestamp).toLocaleTimeString();
        ctx.fillText(firstTime, padding, canvas.height - padding + 15);
        ctx.fillText(lastTime, canvas.width - padding, canvas.height - padding + 15);
    }
}

function formatBytes(bytes) {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

function showError(message) {
    alert(message);
}

function getAuthToken() {
    return localStorage.getItem('token');
}

document.addEventListener('DOMContentLoaded', () => {
    // Authentication is now handled server-side
    // If we reach here, user is authenticated and authorized
    
    loadAdminDashboard();
    loadSystemStatus();
    loadMetricsHistory();

    refreshInterval = setInterval(() => {
        loadSystemStatus();
    }, 30000);

    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            currentPeriod = btn.dataset.period;
            updateGameStats(dashboardData.game_statistics[currentPeriod]);
        });
    });

    document.getElementById('cleanupMetricsBtn').addEventListener('click', () => {
        if (confirm('Clean metrics older than 30 days?')) {
            performDatabaseAction('cleanup_old_metrics', 30);
        }
    });

    document.getElementById('cleanupLobbiesBtn').addEventListener('click', () => {
        performDatabaseAction('cleanup_inactive_lobbies');
    });

    document.getElementById('vacuumDbBtn').addEventListener('click', () => {
        if (confirm('This will vacuum the database. Continue?')) {
            performDatabaseAction('vacuum_database');
        }
    });

    document.getElementById('refreshChartBtn').addEventListener('click', loadMetricsHistory);
    document.getElementById('metricTypeSelect').addEventListener('change', loadMetricsHistory);
    document.getElementById('timeRangeSelect').addEventListener('change', loadMetricsHistory);

    document.getElementById('logoutBtn').addEventListener('click', (e) => {
        e.preventDefault();
        localStorage.removeItem('authToken');
        window.location.href = 'index.html';
    });
});

window.addEventListener('beforeunload', () => {
    if (refreshInterval) {
        clearInterval(refreshInterval);
    }
});
