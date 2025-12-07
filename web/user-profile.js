// API Base URL
const API_BASE = window.location.origin + '/api';

let currentProfile = null;
let currentDifficulty = 'EASY';

// Search for users
async function searchUsers() {
    const query = document.getElementById('searchInput').value.trim();
    
    if (!query) {
        return;
    }

    try {
        const response = await fetch(`${API_BASE}/user/search?q=${encodeURIComponent(query)}`);
        const data = await response.json();

        if (data.success) {
            displaySearchResults(data.users);
        } else {
            alert('Error: ' + data.error);
        }
    } catch (error) {
        console.error('Search error:', error);
        alert('Failed to search users');
    }
}

// Display search results
function displaySearchResults(users) {
    const resultsContainer = document.getElementById('searchResults');
    
    if (users.length === 0) {
        resultsContainer.innerHTML = '<div class="no-games">No users found</div>';
        return;
    }

    resultsContainer.innerHTML = users.map(user => `
        <div class="user-item" onclick="loadUserProfile('${user.username}')">
            <strong>${user.username}</strong>
            <div style="font-size: 12px; color: #666; margin-top: 5px;">
                Member since: ${new Date(user.createdAt).toLocaleDateString()}
            </div>
        </div>
    `).join('');
}

// Load user profile
async function loadUserProfile(username) {
    try {
        document.getElementById('searchContainer').style.display = 'none';
        document.getElementById('profileContainer').style.display = 'block';
        document.getElementById('gamesContainer').innerHTML = '<div class="loading">Loading...</div>';

        const response = await fetch(`${API_BASE}/user/profile/${encodeURIComponent(username)}`);
        const data = await response.json();

        if (data.success) {
            currentProfile = data;
            displayProfile(data);
        } else {
            alert('Error: ' + data.error);
            showSearch();
        }
    } catch (error) {
        console.error('Profile load error:', error);
        alert('Failed to load user profile');
        showSearch();
    }
}

// Display user profile
function displayProfile(data) {
    document.getElementById('profileUsername').textContent = data.user.username;
    document.getElementById('profileInfo').textContent = 
        `Member since ${new Date(data.user.createdAt).toLocaleDateString()}`;

    // Display games for current difficulty
    displayGames(currentDifficulty);
}

// Switch difficulty tab
function switchTab(difficulty) {
    currentDifficulty = difficulty;

    // Update active tab
    document.querySelectorAll('.tab-button').forEach(btn => {
        btn.classList.remove('active');
    });
    event.target.classList.add('active');

    // Display games
    displayGames(difficulty);
}

// Display games for a difficulty
function displayGames(difficulty) {
    const gamesContainer = document.getElementById('gamesContainer');
    
    if (!currentProfile) {
        return;
    }

    const games = currentProfile.games[difficulty];

    if (!games || games.length === 0) {
        gamesContainer.innerHTML = `<div class="no-games">No ${difficulty.toLowerCase()} games played yet</div>`;
        return;
    }

    gamesContainer.innerHTML = games.map(game => `
        <div class="game-card">
            <div class="game-maze-name">${game.mazeName}</div>
            
            <div class="game-stat">
                <div class="game-stat-label">Score</div>
                <div class="game-stat-value ${game.completed ? 'completed' : 'failed'}">
                    ${game.scorePercentage.toFixed(1)}%
                </div>
            </div>

            <div class="game-stat">
                <div class="game-stat-label">Steps</div>
                <div class="game-stat-value">${game.stepsTaken}</div>
            </div>

            <div class="game-stat">
                <div class="game-stat-label">Status</div>
                <div class="game-stat-value ${game.completed ? 'completed' : 'failed'}">
                    ${game.completed ? 'Completed' : 'Failed'}
                </div>
            </div>

            <button class="game-view-button" onclick="viewGame(${game.gameId})">
                View Replay
            </button>
        </div>
    `).join('');
}

// View game replay
function viewGame(gameId) {
    window.location.href = `game-viewer.html?game=${gameId}`;
}

// Logout function
function logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('userId');
    localStorage.removeItem('username');
    window.location.href = 'index.html';
}

// Show search view
function showSearch() {
    document.getElementById('searchContainer').style.display = 'block';
    document.getElementById('profileContainer').style.display = 'none';
    currentProfile = null;
}

// Handle Enter key in search
document.addEventListener('DOMContentLoaded', () => {
    const searchInput = document.getElementById('searchInput');
    searchInput.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') {
            searchUsers();
        }
    });

    // Check if username is in URL
    const urlParams = new URLSearchParams(window.location.search);
    const username = urlParams.get('user');
    if (username) {
        loadUserProfile(username);
    }
});
