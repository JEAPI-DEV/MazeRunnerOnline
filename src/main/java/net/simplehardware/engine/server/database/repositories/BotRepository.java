package net.simplehardware.engine.server.database.repositories;

import net.simplehardware.engine.server.database.models.PlayerBot;

import java.sql.SQLException;
import java.util.List;

/**
 * Repository interface for PlayerBot entity operations
 */
public interface BotRepository {
    /**
     * Create a new player bot
     */
    PlayerBot createPlayerBot(int userId, String botName, String jarPath) throws SQLException;

    /**
     * Get player bot by ID
     */
    PlayerBot getPlayerBotById(int id) throws SQLException;

    /**
     * Get all bots for a user
     */
    List<PlayerBot> getUserBots(int userId) throws SQLException;

    /**
     * Get user's most recent bot
     */
    PlayerBot getUserLatestBot(int userId) throws SQLException;

    /**
     * Set a bot as default for a user
     */
    void setUserDefaultBot(int userId, int botId) throws SQLException;

    /**
     * Get user's default bot
     */
    PlayerBot getUserDefaultBot(int userId) throws SQLException;

    /**
     * Delete a player bot
     */
    String deletePlayerBot(int userId, int botId) throws SQLException;

    /**
     * Check if bot name exists for user
     */
    boolean checkBotNameExists(int userId, String botName) throws SQLException;
}
