// Check authentication on page load
if (!checkAuth()) {
    // Will redirect to index.html
}

// Set username
document.getElementById('username').textContent = localStorage.getItem('username');

let currentHistoryTab = 'SINGLEPLAYER';

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
        const response = await fetch(`${API_BASE}/bot/upload`, {
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
        const response = await fetch(`${API_BASE}/bot/list`, {
            headers: getAuthHeaders()
        });

        const data = await response.json();
        const botsList = document.getElementById('botsList');

        if (response.ok && data.bots && data.bots.length > 0) {
            botsList.innerHTML = '';
            data.bots.forEach(bot => {
                const botCard = document.createElement('div');
                botCard.className = `bot-item ${bot.isDefault ? 'default' : 'inactive'}`;
                botCard.innerHTML = `
                    <div class="bot-info">
                        <h4>${bot.botName}</h4>
                        <p class="bot-date">Uploaded: ${new Date(bot.uploadedAt).toLocaleDateString()}</p>
                        ${bot.isDefault ? '<span class="badge">Default</span>' : ''}
                    </div>
                    <div class="bot-actions">
                        ${!bot.isDefault ? `<button class="btn btn-sm" onclick="setDefaultBot(${bot.id})">Set Default</button>` : ''}
                        <button class="btn btn-sm btn-danger" onclick="deleteBot(${bot.id})">Delete</button>
                    </div>
                `;
                botsList.appendChild(botCard);
            });
        } else {
            botsList.innerHTML = '<p class="empty-state">No bots uploaded yet. Upload your first bot above!</p>';
        }
    } catch (error) {
        console.error('Error loading bots:', error);
        document.getElementById('botsList').innerHTML = '<p class="error">Failed to load bots. Please refresh the page.</p>';
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
async function deleteBot(botId) {
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
        const response = await fetch(`${API_BASE}/game/play?difficulty=${difficulty}`, {
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

        }
    } catch (error) {
        gameResult.className = 'game-result error show';
        gameResult.innerHTML = `<p>Network error: ${error.message}</p>`;
    } finally {
        playButton.disabled = false;
        playButton.textContent = 'Play Game';
    }
}

function switchHistoryTab(tab) {
    currentHistoryTab = tab;

    // Update UI
    document.querySelectorAll('.tab-btn').forEach(btn => {
        if (btn.textContent.toUpperCase() === tab) {
            btn.classList.add('active');
        } else {
            btn.classList.remove('active');
        }
    });

    loadGameHistory();
}

// Load game history
async function loadGameHistory() {
    const historyContainer = document.getElementById('gameHistory');

    try {
        const response = await fetch(`${API_BASE}/user/history?type=${currentHistoryTab}`, {
            headers: getAuthHeaders()
        });

        const data = await response.json();

        if (response.ok && data.games) {
            if (data.games.length === 0) {
                historyContainer.innerHTML = '<p class="no-data">No games played yet.</p>';
                return;
            }

            historyContainer.innerHTML = data.games.map(game => `
                <div class="game-item ${game.completed ? 'completed' : 'failed'}" onclick="window.location.href='viewer.html?game=${game.id}'" style="cursor: pointer;">
                    <div class="game-info">
                        <span class="maze-name">${game.mazeName}</span>
                        <span class="game-date">${new Date(game.playedAt).toLocaleString()}</span>
                    </div>
                    <div class="game-stats">
                        <span class="difficulty ${game.difficulty.toLowerCase()}">${game.difficulty}</span>
                        <span class="steps">${game.stepsTaken} steps</span>
                    </div>
                    <div class="game-score-container">
                         <span class="score">${game.score.toFixed(1)}%</span>
                    </div>
                </div>
            `).join('');
        } else {
            historyContainer.innerHTML = '<p class="error">Failed to load history</p>';
        }
    } catch (error) {
        historyContainer.innerHTML = `<p class="error">Error: ${error.message}</p>`;
    }
}
