// Check authentication on page load
if (!checkAuth()) {
    // Will redirect to index.html
}

// Load leaderboards for all difficulties
loadLeaderboards();

async function loadLeaderboards() {
    const difficulties = ['EASY', 'MEDIUM', 'HARD'];
    const leaderboardDiv = document.getElementById('leaderboard');

    try {
        let html = '';

        for (const difficulty of difficulties) {
            const response = await fetch(`${API_BASE}/leaderboard?difficulty=${difficulty}&limit=10`);
            const data = await response.json();

            html += `<div class="difficulty-section">`;
            html += `<h3>${difficulty.charAt(0) + difficulty.slice(1).toLowerCase()} Difficulty</h3>`;

            if (response.ok && data.leaderboard && data.leaderboard.length > 0) {
                html += `
                    <table>
                        <thead>
                            <tr>
                                <th>Rank</th>
                                <th>Player</th>
                                <th>Games</th>
                                <th>Avg Score</th>
                                <th>Best</th>
                                <th>Last Played</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${data.leaderboard.map((entry, index) => `
                                <tr>
                                    <td><span class="rank rank-${index + 1}">#${index + 1}</span></td>
                                    <td><strong>${entry.username}</strong></td>
                                    <td>${entry.gamesPlayed}</td>
                                    <td>${entry.avgScore.toFixed(2)}%</td>
                                    <td>${entry.bestScore.toFixed(2)}%</td>
                                    <td>${entry.lastPlayed ? new Date(entry.lastPlayed).toLocaleDateString() : 'N/A'}</td>
                                </tr>
                            `).join('')}
                        </tbody>
                    </table>
                `;
            } else {
                html += '<p class="loading">No games played yet in this difficulty.</p>';
            }

            html += `</div>`;
        }

        leaderboardDiv.innerHTML = html;
    } catch (error) {
        leaderboardDiv.innerHTML = '<p class="loading">Error loading leaderboards</p>';
    }
}

// Auto-refresh every 30 seconds
setInterval(loadLeaderboards, 30000);
