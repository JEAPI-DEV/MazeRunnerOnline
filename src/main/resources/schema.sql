-- Maze Runner Server Mode Database Schema
-- Users table
CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
-- Player bots table
CREATE TABLE IF NOT EXISTS player_bots (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    bot_name TEXT NOT NULL,
    jar_path TEXT NOT NULL,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_default BOOLEAN DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE(user_id, bot_name)
);
-- Mazes table
CREATE TABLE IF NOT EXISTS mazes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT UNIQUE NOT NULL,
    file_path TEXT NOT NULL,
    min_steps INTEGER NOT NULL,
    forms INTEGER NOT NULL,
    size INTEGER NOT NULL,
    difficulty TEXT NOT NULL CHECK(difficulty IN ('EASY', 'MEDIUM', 'HARD')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    active BOOLEAN DEFAULT 1
);
-- Game results table
CREATE TABLE IF NOT EXISTS game_results (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    bot_id INTEGER NOT NULL,
    maze_id INTEGER NOT NULL,
    steps_taken INTEGER NOT NULL,
    score_percentage REAL NOT NULL,
    completed BOOLEAN NOT NULL,
    game_data_path TEXT,
    played_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (bot_id) REFERENCES player_bots(id) ON DELETE CASCADE,
    FOREIGN KEY (maze_id) REFERENCES mazes(id) ON DELETE CASCADE
);
-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_game_results_user ON game_results(user_id);
CREATE INDEX IF NOT EXISTS idx_game_results_maze ON game_results(maze_id);
CREATE INDEX IF NOT EXISTS idx_game_results_played_at ON game_results(played_at DESC);
CREATE INDEX IF NOT EXISTS idx_mazes_active ON mazes(active);
-- Leaderboard view
CREATE VIEW IF NOT EXISTS leaderboard AS
SELECT u.id as user_id,
    u.username,
    COUNT(gr.id) as games_played,
    ROUND(AVG(gr.score_percentage), 2) as avg_score,
    ROUND(MIN(gr.score_percentage), 2) as worst_score,
    ROUND(MAX(gr.score_percentage), 2) as best_score,
    MAX(gr.played_at) as last_played
FROM users u
    LEFT JOIN game_results gr ON u.id = gr.user_id
GROUP BY u.id,
    u.username
ORDER BY avg_score DESC;
-- User statistics view
CREATE VIEW IF NOT EXISTS user_stats AS
SELECT u.id as user_id,
    u.username,
    COUNT(DISTINCT pb.id) as total_bots,
    COUNT(DISTINCT gr.id) as total_games,
    COUNT(
        DISTINCT CASE
            WHEN gr.completed = 1 THEN gr.id
        END
    ) as completed_games,
    ROUND(
        AVG(
            CASE
                WHEN gr.completed = 1 THEN gr.score_percentage
            END
        ),
        2
    ) as avg_score,
    ROUND(MAX(gr.score_percentage), 2) as best_score,
    MAX(gr.played_at) as last_played
FROM users u
    LEFT JOIN player_bots pb ON u.id = pb.user_id
    LEFT JOIN game_results gr ON u.id = gr.user_id
GROUP BY u.id,
    u.username;