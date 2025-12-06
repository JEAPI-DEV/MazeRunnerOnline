let currentLobbyId = null;
let isHost = false;
let pollInterval = null;

document.addEventListener('DOMContentLoaded', () => {
    checkAuth();
    loadLobbies();
    setupEventListeners();
});

function setupEventListeners() {
    document.getElementById('create-lobby-btn').addEventListener('click', showCreateLobbyModal);
    document.getElementById('cancel-create-btn').addEventListener('click', hideCreateLobbyModal);
    document.getElementById('create-lobby-form').addEventListener('submit', handleCreateLobby);
    document.getElementById('back-btn').addEventListener('click', () => window.location.href = 'dashboard.html');
    document.getElementById('leave-lobby-btn').addEventListener('click', leaveLobby);
    document.getElementById('start-game-btn').addEventListener('click', startGame);
}

function checkAuth() {
    const token = localStorage.getItem('token');
    if (!token) {
        window.location.href = 'index.html';
    }
}

async function loadLobbies() {
    try {
        const response = await fetch(`${API_BASE}/lobby/list`);
        const data = await response.json();

        const container = document.getElementById('lobbies-container');
        container.innerHTML = '';

        if (data.lobbies && data.lobbies.length > 0) {
            data.lobbies.forEach(lobbyData => {
                const lobbyCard = createLobbyCard(lobbyData);
                container.appendChild(lobbyCard);
            });
        } else {
            container.innerHTML = '<p class="empty-state">No active lobbies. Create one to get started!</p>';
        }
    } catch (error) {
        console.error('Error loading lobbies:', error);
        showError('Failed to load lobbies');
    }
}

function createLobbyCard(lobbyData) {
    const { lobby, playerCount } = lobbyData;
    const card = document.createElement('div');
    card.className = 'lobby-card';
    card.innerHTML = `
        <h3>${lobby.name}</h3>
        <p>Players: ${playerCount}/${lobby.maxPlayers}</p>
        <button class="btn btn-primary" onclick="joinLobby(${lobby.id})">Join</button>
    `;
    return card;
}

async function showCreateLobbyModal() {
    await loadMazes();
    document.getElementById('create-lobby-modal').style.display = 'flex';
}

function hideCreateLobbyModal() {
    document.getElementById('create-lobby-modal').style.display = 'none';
}

async function loadMazes() {
    try {
        const token = localStorage.getItem('token');
        const response = await fetch(`${API_BASE}/mazes`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        const data = await response.json();

        const select = document.getElementById('maze-select');
        select.innerHTML = '';
        data.mazes.forEach(maze => {
            const option = document.createElement('option');
            option.value = maze.id;
            option.textContent = `${maze.name} (${maze.difficulty})`;
            select.appendChild(option);
        });
    } catch (error) {
        console.error('Error loading mazes:', error);
    }
}

async function handleCreateLobby(e) {
    e.preventDefault();

    const name = document.getElementById('lobby-name-input').value;
    const mazeId = parseInt(document.getElementById('maze-select').value);
    const maxPlayers = parseInt(document.getElementById('max-players-input').value);

    try {
        const token = localStorage.getItem('token');
        const response = await fetch(`${API_BASE}/lobby/create`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({ name, mazeId, maxPlayers })
        });

        const data = await response.json();

        if (response.ok) {
            hideCreateLobbyModal();
            enterLobby(data.lobby.id);
        } else {
            showError(data.error || 'Failed to create lobby');
        }
    } catch (error) {
        console.error('Error creating lobby:', error);
        showError('Failed to create lobby');
    }
}

async function joinLobby(lobbyId) {
    try {
        const token = localStorage.getItem('token');
        const response = await fetch(`${API_BASE}/lobby/join`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({ lobbyId })
        });

        const data = await response.json();

        if (response.ok) {
            enterLobby(lobbyId);
        } else {
            showError(data.error || 'Failed to join lobby');
        }
    } catch (error) {
        console.error('Error joining lobby:', error);
        showError('Failed to join lobby');
    }
}

async function enterLobby(lobbyId) {
    currentLobbyId = lobbyId;
    document.getElementById('lobby-list-section').style.display = 'none';
    document.getElementById('lobby-room-section').style.display = 'block';

    await updateLobbyRoom(lobbyId);

    pollInterval = setInterval(() => updateLobbyRoom(lobbyId), 2000);
}

async function updateLobbyRoom(lobbyId) {
    try {
        const response = await fetch(`${API_BASE}/lobby/${lobbyId}`, {
            headers: getAuthHeaders()
        });

        if (response.ok) {
            const data = await response.json();
            const { lobby, players } = data;

            // Check if game is finished and we have a game ID to redirect to
            if (lobby.status === 'FINISHED' && lobby.lastGameId) {
                window.location.href = `viewer.html?game=${lobby.lastGameId}`;
                return;
            }

            const lobbyNameElement = document.getElementById('lobby-name');
            const lobbyStatusElement = document.getElementById('lobby-status');
            const playersListElement = document.getElementById('players-list');
            const startGameBtn = document.getElementById('start-game-btn');
            const lobbyMaxPlayersElement = document.getElementById('lobby-max-players');

            if (lobbyNameElement) lobbyNameElement.textContent = lobby.name;

            if (lobbyStatusElement) {
                lobbyStatusElement.textContent = lobby.status;
                lobbyStatusElement.className = `lobby-status status-${lobby.status.toLowerCase()}`;

                // Show loading overlay if game is in progress
                if (lobby.status === 'IN_PROGRESS') {
                    // You could add a loading overlay here if desired
                    lobbyStatusElement.textContent = 'Game in Progress...';
                }
            }

            if (lobbyMaxPlayersElement) {
                lobbyMaxPlayersElement.textContent = lobby.maxPlayers;
            }

            const userId = parseInt(localStorage.getItem('userId'));
            isHost = lobby.hostUserId === userId;

            if (playersListElement && players) {
                playersListElement.innerHTML = '<h3>Players</h3>'; // Clear and add header
                players.forEach(player => {
                    const playerDiv = document.createElement('div');
                    playerDiv.className = 'player-item';
                    playerDiv.innerHTML = `
                        <span>${player.botName}</span>
                        ${player.userId === lobby.hostUserId ? '<span class="badge">Host</span>' : ''}
                    `;
                    playersListElement.appendChild(playerDiv);
                });
            }

            // Show start button only for host and when waiting
            if (startGameBtn) {
                if (isHost && lobby.status === 'WAITING' && players.length >= 2) {
                    startGameBtn.style.display = 'block';
                } else {
                    startGameBtn.style.display = 'none';
                }
            }
        } else {
            // If lobby not found (e.g. deleted), go back to list
            if (response.status === 404) {
                currentLobbyId = null;
                document.getElementById('lobby-room-section').style.display = 'none';
                document.getElementById('lobby-list-section').style.display = 'block';
                loadLobbies();
            }
        }
    } catch (error) {
        console.error('Error updating lobby:', error);
    }
}

async function leaveLobby() {
    if (pollInterval) {
        clearInterval(pollInterval);
    }

    try {
        const token = localStorage.getItem('token');
        await fetch(`${API_BASE}/lobby/leave`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({ lobbyId: currentLobbyId })
        });
    } catch (error) {
        console.error('Error leaving lobby:', error);
    }

    currentLobbyId = null;
    document.getElementById('lobby-list-section').style.display = 'block';
    document.getElementById('lobby-room-section').style.display = 'none';
    loadLobbies();
}

async function startGame() {
    try {
        const token = localStorage.getItem('token');
        const response = await fetch(`${API_BASE}/lobby/start`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({ lobbyId: currentLobbyId })
        });

        const data = await response.json();

        if (response.ok) {
            showSuccess('Game started! Redirecting to results...');
            setTimeout(() => {
                window.location.href = 'dashboard.html';
            }, 2000);
        } else {
            showError(data.error || 'Failed to start game');
        }
    } catch (error) {
        console.error('Error starting game:', error);
        showError('Failed to start game');
    }
}

function showError(message) {
    alert('Error: ' + message);
}

function showSuccess(message) {
    alert(message);
}
