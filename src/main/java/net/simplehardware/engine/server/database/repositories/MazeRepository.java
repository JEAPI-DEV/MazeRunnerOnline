package net.simplehardware.engine.server.database.repositories;

import net.simplehardware.engine.server.database.models.Maze;

import java.sql.SQLException;
import java.util.List;

/**
 * Repository interface for Maze entity operations
 */
public interface MazeRepository {
    /**
     * Create a new maze
     */
    Maze createMaze(String name, String filePath, int minSteps, int forms, int size, Maze.Difficulty difficulty)
            throws SQLException;

    /**
     * Get maze by ID
     */
    Maze getMazeById(int id) throws SQLException;

    /**
     * Get maze name by ID
     */
    String getMazeName(int mazeId) throws SQLException;

    /**
     * Get all active mazes
     */
    List<Maze> getActiveMazes() throws SQLException;

    /**
     * Get all mazes
     */
    List<Maze> getAllMazes() throws SQLException;

    /**
     * Get unplayed mazes for a user
     */
    List<Maze> getUnplayedMazes(int userId, String difficulty) throws SQLException;

    /**
     * Get mazes by difficulty
     */
    List<Maze> getMazesByDifficulty(Maze.Difficulty difficulty) throws SQLException;

    /**
     * Get a random active maze for specified difficulty
     */
    Maze getRandomMaze(Maze.Difficulty difficulty) throws SQLException;

    /**
     * Deactivate old mazes (based on creation date)
     */
    int deactivateOldMazes(int daysOld) throws SQLException;

    /**
     * Update maze active status
     */
    void setMazeActive(int mazeId, boolean active) throws SQLException;
}
