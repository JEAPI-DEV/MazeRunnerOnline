package net.simplehardware.engine.server.database.repositories;

import net.simplehardware.engine.server.database.models.GameResult;

import java.sql.SQLException;
import java.util.List;

/**
 * Repository interface for GameResult operations
 */
public interface GameRepository {
    /**
     * Create a new game result
     */
    GameResult createGameResult(int userId, int botId, int mazeId, int stepsTaken,
                                double scorePercentage, boolean completed, String gameDataPath) throws SQLException;

    /**
     * Get game result by ID
     */
    GameResult getGameResultById(int id) throws SQLException;

    /**
     * Get user's game history
     */
    List<GameResult> getUserGameHistory(int userId, int limit, String type) throws SQLException;

    /**
     * Get user's game history by difficulty
     */
    List<GameResult> getUserGameHistoryByDifficulty(int userId, String difficulty) throws SQLException;

    /**
     * Get the best game result for a user on a specific maze
     */
    GameResult getBestScoreForMaze(int userId, int mazeId) throws SQLException;

    /**
     * Delete a game result
     */
    String deleteGameResult(int gameResultId) throws SQLException;
}
