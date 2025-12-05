// Game Viewer JavaScript
class GameViewer {
    constructor() {
        this.gameHistory = [];
        this.currentTurnIndex = 0;
        this.isPlaying = false;
        this.playbackInterval = null;
        this.playbackSpeed = 1000; // milliseconds

        // Canvas and rendering
        this.canvas = document.getElementById('maze-canvas');
        this.ctx = this.canvas.getContext('2d');
        this.cellSize = 80;

        // Player colors
        this.playerColors = [
            'rgb(255, 100, 100)', // Player 1 - Red
            'rgb(100, 100, 255)', // Player 2 - Blue
            'rgb(100, 255, 100)', // Player 3 - Green
            'rgb(255, 255, 100)'  // Player 4 - Yellow
        ];

        // Form colors (matching Java implementation)
        this.formColors = {
            'A': 'rgb(224, 60, 60)',
            'B': 'rgb(255, 255, 0)',
            'C': 'rgb(196, 106, 255)',
            'D': 'rgb(101, 187, 255)',
            'E': 'rgb(255, 0, 255)',
            'F': 'rgb(0, 255, 255)',
            'G': 'rgb(255, 165, 0)',
            'H': 'rgb(128, 0, 128)',
            'I': 'rgb(255, 192, 203)',
            'J': 'rgb(165, 42, 42)',
            'K': 'rgb(255, 215, 0)',
            'L': 'rgb(0, 128, 0)',
            'M': 'rgb(128, 0, 0)',
            'N': 'rgb(0, 0, 128)',
            'O': 'rgb(255, 140, 0)',
            'P': 'rgb(75, 0, 130)',
            'Q': 'rgb(240, 128, 128)',
            'R': 'rgb(50, 205, 50)',
            'S': 'rgb(255, 69, 0)',
            'T': 'rgb(255, 20, 147)',
            'U': 'rgb(64, 224, 208)',
            'V': 'rgb(220, 20, 60)',
            'W': 'rgb(255, 255, 224)',
            'X': 'rgb(152, 251, 152)',
            'Y': 'rgb(238, 130, 238)',
            'Z': 'rgb(70, 130, 180)'
        };

        this.initializeControls();
    }

    initializeControls() {
        // Timeline slider
        const slider = document.getElementById('timeline-slider');
        slider.addEventListener('input', (e) => {
            this.currentTurnIndex = parseInt(e.target.value);
            this.updateDisplay();
        });

        // Playback controls
        document.getElementById('play-pause-btn').addEventListener('click', () => this.togglePlayback());
        document.getElementById('prev-btn').addEventListener('click', () => this.previousTurn());
        document.getElementById('next-btn').addEventListener('click', () => this.nextTurn());

        // Speed control
        document.getElementById('speed-control').addEventListener('change', (e) => {
            this.playbackSpeed = parseInt(e.target.value);
            if (this.isPlaying) {
                this.stopPlayback();
                this.startPlayback();
            }
        });
    }

    loadGameData(gameData) {
        this.gameHistory = gameData.gameHistory;
        this.mazeName = gameData.mazeName || 'Unknown Maze';

        if (this.gameHistory.length === 0) {
            console.error('Game history is empty');
            return;
        }

        // Update title
        document.getElementById('maze-title').textContent = `Maze Runner - ${this.mazeName}`;

        // Setup timeline
        const slider = document.getElementById('timeline-slider');
        slider.max = this.gameHistory.length - 1;
        slider.value = 0;

        // Setup canvas
        const firstState = this.gameHistory[0];
        this.setupCanvas(firstState.mazeWidth, firstState.mazeHeight);

        // Create player log panels
        this.createPlayerLogPanels(firstState.players);

        // Initial display
        this.updateDisplay();
    }

    setupCanvas(width, height) {
        // Calculate cell size based on available space
        const maxWidth = this.canvas.parentElement.clientWidth - 40;
        const maxHeight = this.canvas.parentElement.clientHeight - 40;

        this.cellSize = Math.min(
            Math.floor(maxWidth / width),
            Math.floor(maxHeight / height),
            80 // Max cell size
        );

        this.canvas.width = width * this.cellSize;
        this.canvas.height = height * this.cellSize;
    }

    createPlayerLogPanels(players) {
        const logsGrid = document.getElementById('logs-grid');
        logsGrid.innerHTML = '';

        const playerIds = Object.keys(players).sort();

        for (let i = 0; i < 4; i++) {
            if (i < playerIds.length) {
                const playerId = playerIds[i];
                const panel = this.createPlayerLogPanel(parseInt(playerId));
                logsGrid.appendChild(panel);
            } else {
                // Empty panel
                const emptyPanel = document.createElement('div');
                emptyPanel.className = 'player-log-panel empty';
                logsGrid.appendChild(emptyPanel);
            }
        }
    }

    createPlayerLogPanel(playerId) {
        const panel = document.createElement('div');
        panel.className = `player-log-panel player-${playerId}`;
        panel.innerHTML = `
            <div class="log-title">Player ${playerId}</div>
            <div class="log-section stdout-section">
                <div class="log-label stdout">Standard Output</div>
                <div class="log-content" id="stdout-${playerId}"></div>
            </div>
            <div class="log-section stderr-section">
                <div class="log-label stderr">Standard Error</div>
                <div class="log-content" id="stderr-${playerId}"></div>
            </div>
        `;
        return panel;
    }

    updateDisplay() {
        if (this.gameHistory.length === 0) return;

        const state = this.gameHistory[this.currentTurnIndex];

        // Update turn info
        const lastTurn = this.gameHistory[this.gameHistory.length - 1].turnNumber;
        document.getElementById('turn-info').textContent = `Turn ${state.turnNumber} / ${lastTurn}`;
        document.getElementById('current-turn').textContent = `Turn ${state.turnNumber}`;
        document.getElementById('total-turns').textContent = `/ ${lastTurn}`;

        // Update maze
        this.renderMaze(state);

        // Update player stats
        this.updatePlayerStats(state);

        // Update player logs
        this.updatePlayerLogs(state);
    }

    renderMaze(state) {
        const ctx = this.ctx;
        const cellSize = this.cellSize;

        // Clear canvas
        ctx.fillStyle = '#1e2228';
        ctx.fillRect(0, 0, this.canvas.width, this.canvas.height);

        // Draw cells
        for (let x = 0; x < state.mazeWidth; x++) {
            for (let y = 0; y < state.mazeHeight; y++) {
                const cell = state.cells[x][y];
                this.drawCell(ctx, cell, x, y, cellSize);
            }
        }

        // Draw players on top
        Object.values(state.players).forEach(player => {
            if (player.active) {
                this.drawPlayer(ctx, player, cellSize);
            }
        });
    }

    drawCell(ctx, cell, x, y, cellSize) {
        const px = x * cellSize;
        const py = y * cellSize;

        // Fill background
        let bgColor;
        switch (cell.type) {
            case 'WALL':
                bgColor = 'rgb(120, 20, 20)';
                break;
            case 'FINISH':
                const playerColor = this.playerColors[cell.finishPlayerId - 1];
                bgColor = this.darkenColor(playerColor);
                break;
            case 'FLOOR':
            default:
                if (cell.form) {
                    bgColor = this.formColors[cell.form] || 'rgb(128, 128, 128)';
                } else if (cell.hasSheet) {
                    bgColor = 'rgb(255, 152, 0)';
                } else {
                    bgColor = 'rgb(245, 245, 245)';
                }
                break;
        }

        ctx.fillStyle = bgColor;
        ctx.fillRect(px, py, cellSize, cellSize);

        // Draw grid lines
        ctx.strokeStyle = 'rgb(90, 94, 102)';
        ctx.lineWidth = 1;
        ctx.strokeRect(px, py, cellSize, cellSize);

        // Draw form if present
        if (cell.form) {
            ctx.fillStyle = 'black';
            ctx.font = 'bold 12px Arial';
            ctx.textAlign = 'center';
            ctx.textBaseline = 'middle';
            const formText = cell.form + (cell.formOwner || '');
            ctx.fillText(formText, px + cellSize / 2, py + cellSize / 2);
        }

        // Draw sheet indicator if present (and form is also present)
        if (cell.hasSheet && cell.form) {
            ctx.fillStyle = 'rgb(255, 152, 0)';
            ctx.beginPath();
            ctx.arc(px + cellSize - 10, py + 10, 5, 0, Math.PI * 2);
            ctx.fill();
        }
    }

    drawPlayer(ctx, player, cellSize) {
        const px = player.x * cellSize;
        const py = player.y * cellSize;
        const margin = 2;

        // Draw player circle
        ctx.fillStyle = this.playerColors[player.id - 1];
        ctx.beginPath();
        ctx.arc(
            px + cellSize / 2,
            py + cellSize / 2,
            (cellSize - 2 * margin) / 2,
            0,
            Math.PI * 2
        );
        ctx.fill();

        // Draw player number
        ctx.fillStyle = 'black';
        ctx.font = 'bold 16px Arial';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.fillText(player.id.toString(), px + cellSize / 2, py + cellSize / 2);
    }

    updatePlayerStats(state) {
        const statsContainer = document.getElementById('player-stats');
        statsContainer.innerHTML = '';

        Object.values(state.players).forEach(player => {
            const card = document.createElement('div');
            card.className = `player-card player-${player.id}`;

            const statusClass = player.finished ? 'finished' : (player.active ? 'active' : 'inactive');
            const statusText = player.finished ? 'FINISHED' : (player.active ? 'Active' : 'Inactive');

            card.innerHTML = `
                <div class="player-name">Player ${player.id}</div>
                <div class="stat">Score: ${player.score}</div>
                <div class="stat">Forms: ${player.formsCollected}/${player.formsRequired}</div>
                <div class="status ${statusClass}">${statusText}</div>
            `;

            statsContainer.appendChild(card);
        });
    }

    updatePlayerLogs(state) {
        Object.entries(state.playerLogs || {}).forEach(([playerId, log]) => {
            const stdoutEl = document.getElementById(`stdout-${playerId}`);
            const stderrEl = document.getElementById(`stderr-${playerId}`);

            if (stdoutEl) {
                stdoutEl.textContent = log.stdout || '';
                stdoutEl.scrollTop = 0;
            }

            if (stderrEl) {
                stderrEl.textContent = log.stderr || '';
                stderrEl.scrollTop = 0;
            }
        });
    }

    togglePlayback() {
        if (this.isPlaying) {
            this.stopPlayback();
        } else {
            this.startPlayback();
        }
    }

    startPlayback() {
        this.isPlaying = true;
        document.getElementById('play-pause-btn').textContent = '⏸';

        this.playbackInterval = setInterval(() => {
            if (this.currentTurnIndex < this.gameHistory.length - 1) {
                this.currentTurnIndex++;
                document.getElementById('timeline-slider').value = this.currentTurnIndex;
                this.updateDisplay();
            } else {
                this.stopPlayback();
            }
        }, this.playbackSpeed);
    }

    stopPlayback() {
        this.isPlaying = false;
        document.getElementById('play-pause-btn').textContent = '▶';

        if (this.playbackInterval) {
            clearInterval(this.playbackInterval);
            this.playbackInterval = null;
        }
    }

    previousTurn() {
        if (this.currentTurnIndex > 0) {
            this.currentTurnIndex--;
            document.getElementById('timeline-slider').value = this.currentTurnIndex;
            this.updateDisplay();
        }
    }

    nextTurn() {
        if (this.currentTurnIndex < this.gameHistory.length - 1) {
            this.currentTurnIndex++;
            document.getElementById('timeline-slider').value = this.currentTurnIndex;
            this.updateDisplay();
        }
    }

    darkenColor(rgb) {
        const match = rgb.match(/\d+/g);
        if (!match) return rgb;

        const [r, g, b] = match.map(n => Math.floor(parseInt(n) * 0.6));
        return `rgb(${r}, ${g}, ${b})`;
    }
}

// Initialize the game viewer
const viewer = new GameViewer();

// Example: Load game data from a JSON file or API
// This would typically be loaded from your Java backend
function loadGameFromJSON(jsonData) {
    viewer.loadGameData(jsonData);
}

// For demonstration, you can expose this globally
window.gameViewer = viewer;
window.loadGameFromJSON = loadGameFromJSON;

// Example of expected data format:
/*
const exampleGameData = {
    mazeName: "Test Maze",
    gameHistory: [
        {
            turnNumber: 1,
            mazeWidth: 20,
            mazeHeight: 20,
            players: {
                1: {
                    id: 1,
                    x: 0,
                    y: 0,
                    score: 0,
                    formsCollected: 0,
                    formsRequired: 4,
                    active: true,
                    finished: false
                }
            },
            cells: [], // 2D array of cell objects
            playerLogs: {
                1: {
                    stdout: "Player output",
                    stderr: "Error messages"
                }
            }
        }
    ]
};
*/
