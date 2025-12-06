// Check authentication on page load
if (!checkAuth()) {
    // Will redirect to index.html
}

// Set username
document.getElementById('username').textContent = localStorage.getItem('username');

// Load user data
loadBots();
loadGameHistory();

// Bot Upload
async function handleBotUpload(event) {
    event.preventDefault();

    const botName = document.getElementById('botName').value;
    const botFile = document.getElementById('botFile').files[0];

    if (!botFile) {
        showNotification('Please select a JAR file', 'error');
        return;
    }

    const formData = new FormData();
    formData.append('botName', botName);
    formData.append('file', botFile);

    try {
        const response = await fetch(`${API_BASE}/upload-bot`, {
            method: 'POST',
            headers: {
                'Authorization': 'Bearer ' + localStorage.getItem('token')
            },
            body: formData
        });

        const data = await response.json();

        if (response.ok && data.success) {
            showNotification('Bot uploaded successfully!');
            document.getElementById('uploadForm').reset();
            loadBots();
        } else {
            showNotification(data.error || 'Upload failed', 'error');
        }
    } catch (error) {
        showNotification('Network error: ' + error.message, 'error');
    }
}

// Load user's bots
async function loadBots() {
    try {
        const response = await fetch(`${API_BASE}/user/bots`, {
            headers: getAuthHeaders()
        });

        const data = await response.json();
        const botsList = document.getElementById('botsList');

        if (response.ok && data.bots && data.bots.length > 0) {
            botsList.innerHTML = data.bots.map(bot => `
                <div class="bot-item ${bot.isDefault ? 'default' : 'inactive'}" onclick="setDefaultBot(${bot.id})">
                    <div class="bot-info">
                        <h4>${bot.name} ${bot.isDefault ? '<span class="badge">Default</span>' : ''}</h4>
                        <p>Uploaded: ${new Date(bot.uploadedAt).toLocaleString()}</p>
                    </div>
                    <button class="btn-delete" onclick="deleteBot(event, ${bot.id})" title="Delete Bot">
                        <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor">
                            <path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"/>
                        </svg>
                    </button>
                </div>
            `).join('');
        } else {
            botsList.innerHTML = '<p class="loading">No bots uploaded yet. Upload your first bot above!</p>';
        }
    } catch (error) {
        document.getElementById('botsList').innerHTML = '<p class="loading">Error loading bots</p>';
    }
}

// Set default bot
async function setDefaultBot(botId) {
    try {
        const response = await fetch(`${API_BASE}/bot/default`, {
            method: 'POST',
            headers: getAuthHeaders(),
            body: JSON.stringify({ botId: botId })
        });

        const data = await response.json();

        if (response.ok && data.success) {
            loadBots(); // Reload to update UI
        } else {
            showNotification(data.error || 'Failed to set default bot', 'error');
        }
    } catch (error) {
        showNotification('Network error: ' + error.message, 'error');
    }
}

// Delete bot
async function deleteBot(event, botId) {
    event.stopPropagation(); // Prevent triggering selection

    if (!confirm('Are you sure you want to delete this bot?')) {
        return;
    }

    try {
        const response = await fetch(`${API_BASE}/bot/delete`, {
            method: 'POST',
            headers: getAuthHeaders(),
            body: JSON.stringify({ botId: botId })
        });

        const data = await response.json();

        if (response.ok && data.success) {
            showNotification('Bot deleted successfully');
            loadBots();
        } else {
            showNotification(data.error || 'Failed to delete bot', 'error');
        }
    } catch (error) {
        showNotification('Network error: ' + error.message, 'error');
    }
}

// Play game
async function playGame() {
    const playButton = document.getElementById('playButton');
    const gameResult = document.getElementById('gameResult');
    const difficulty = document.getElementById('difficultySelect').value;

    playButton.disabled = true;
    playButton.textContent = 'Running game...';
    gameResult.className = 'game-result';
    gameResult.innerHTML = '';

    try {
        const response = await fetch(`${API_BASE}/play?difficulty=${difficulty}`, {
            method: 'POST',
            headers: getAuthHeaders()
        });

        const data = await response.json();

        if (response.ok && data.success) {
            gameResult.className = 'game-result success show';
            gameResult.innerHTML = `
                <h4>Game Complete!</h4>
                <div class="game-stats">
                    <div class="stat-row">
                        <span>Maze:</span>
                        <strong>${data.mazeName} (${data.difficulty})</strong>
                    </div>
                    <div class="stat-row">
                        <span>Steps Taken:</span>
                        <strong>${data.stepsTaken}</strong>
                    </div>
                    <div class="stat-row">
                        <span>Minimum Steps:</span>
                        <strong>${data.minSteps}</strong>
                    </div>
                    <div class="stat-row">
                        <span>Score:</span>
                        <strong>${data.score.toFixed(2)}%</strong>
                    </div>
                    <div class="stat-row">
                        <span>Status:</span>
                        <strong>${data.completed ? 'Completed' : 'Incomplete'}</strong>
                    </div>
                </div>
                <a href="viewer.html?game=${data.gameId}" class="btn btn-primary" style="margin-top: 15px; display: inline-block;">
                    View Replay
                </a>
            `;

            // Reload game history
            loadGameHistory();
        } else {
            gameResult.className = 'game-result error show';
            gameResult.innerHTML = `<p>Game execution failed: ${data.error || 'Unknown error'}</p>`;
        }
    } catch (error) {
        gameResult.className = 'game-result error show';
        gameResult.innerHTML = `<p>Network error: ${error.message}</p>`;
    } finally {
        playButton.disabled = false;
        playButton.textContent = 'Play Game';
    }
}

// Load game history
async function loadGameHistory() {
    try {
        const response = await fetch(`${API_BASE}/user/history`, {
            headers: getAuthHeaders()
        });

        const data = await response.json();
        const historyDiv = document.getElementById('gameHistory');

        if (response.ok && data.games && data.games.length > 0) {
            historyDiv.innerHTML = data.games.map(game => `
                <div class="game-item">
                    <div class="game-info">
                        <h4>${game.mazeName} (${game.difficulty})</h4>
                        <p>${new Date(game.playedAt).toLocaleString()} • ${game.stepsTaken} steps</p>
                    </div>
                    <div class="game-actions">
                        <div class="game-score ${game.completed ? 'completed' : 'failed'}">
                            ${game.completed ? game.score.toFixed(1) + '%' : 'Failed'}
                        </div>
                        <a href="viewer.html?game=${game.id}" class="btn-view">View Replay</a>
                    </div>
                </div>
            `).join('');
        } else {
            historyDiv.innerHTML = '<p class="loading">No games played yet. Click "Play Game" to start!</p>';
        }
    } catch (error) {
        document.getElementById('gameHistory').innerHTML = '<p class="loading">Error loading game history</p>';
    }
}
