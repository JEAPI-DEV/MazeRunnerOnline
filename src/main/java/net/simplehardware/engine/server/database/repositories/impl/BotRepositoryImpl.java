package net.simplehardware.engine.server.database.repositories.impl;

import net.simplehardware.engine.server.database.models.PlayerBot;
import net.simplehardware.engine.server.database.repositories.BotRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BotRepositoryImpl implements BotRepository {
    private final Connection connection;

    public BotRepositoryImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public PlayerBot createPlayerBot(int userId, String botName, String jarPath) throws SQLException {
        boolean isFirstBot = getUserBots(userId).isEmpty();

        String sql = "INSERT INTO player_bots (user_id, bot_name, jar_path, is_default) VALUES (?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, botName);
            pstmt.setString(3, jarPath);
            pstmt.setBoolean(4, isFirstBot);
            pstmt.executeUpdate();

            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                if (rs.next()) {
                    return getPlayerBotById(rs.getInt(1));
                }
            }
        }
        return null;
    }

    @Override
    public PlayerBot getPlayerBotById(int id) throws SQLException {
        String sql = "SELECT * FROM player_bots WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToPlayerBot(rs);
            }
        }
        return null;
    }

    @Override
    public List<PlayerBot> getUserBots(int userId) throws SQLException {
        String sql = "SELECT * FROM player_bots WHERE user_id = ? ORDER BY uploaded_at DESC";
        List<PlayerBot> bots = new ArrayList<>();

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                bots.add(mapResultSetToPlayerBot(rs));
            }
        }
        return bots;
    }

    @Override
    public PlayerBot getUserLatestBot(int userId) throws SQLException {
        String sql = "SELECT * FROM player_bots WHERE user_id = ? ORDER BY uploaded_at DESC LIMIT 1";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToPlayerBot(rs);
            }
        }
        return null;
    }

    @Override
    public void setUserDefaultBot(int userId, int botId) throws SQLException {
        boolean autoCommit = connection.getAutoCommit();
        try {
            connection.setAutoCommit(false);

            String resetSql = "UPDATE player_bots SET is_default = 0 WHERE user_id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(resetSql)) {
                pstmt.setInt(1, userId);
                pstmt.executeUpdate();
            }

            String setSql = "UPDATE player_bots SET is_default = 1 WHERE id = ? AND user_id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(setSql)) {
                pstmt.setInt(1, botId);
                pstmt.setInt(2, userId);
                pstmt.executeUpdate();
            }

            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(autoCommit);
        }
    }

    @Override
    public PlayerBot getUserDefaultBot(int userId) throws SQLException {
        String sql = "SELECT * FROM player_bots WHERE user_id = ? AND is_default = 1";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToPlayerBot(rs);
            }
        }
        return null;
    }

    @Override
    public String deletePlayerBot(int userId, int botId) throws SQLException {
        PlayerBot bot = getPlayerBotById(botId);
        if (bot == null || bot.getUserId() != userId) {
            return null;
        }

        boolean wasDefault = bot.isDefault();

        String sql = "DELETE FROM player_bots WHERE id = ? AND user_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, botId);
            pstmt.setInt(2, userId);
            int affected = pstmt.executeUpdate();

            if (affected > 0) {
                if (wasDefault) {
                    PlayerBot latest = getUserLatestBot(userId);
                    if (latest != null) {
                        setUserDefaultBot(userId, latest.getId());
                    }
                }
                return bot.getJarPath();
            }
        }
        return null;
    }

    @Override
    public boolean checkBotNameExists(int userId, String botName) throws SQLException {
        String sql = "SELECT 1 FROM player_bots WHERE user_id = ? AND bot_name = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, botName);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private PlayerBot mapResultSetToPlayerBot(ResultSet rs) throws SQLException {
        return new PlayerBot(
                rs.getInt("id"),
                rs.getInt("user_id"),
                rs.getString("bot_name"),
                rs.getString("jar_path"),
                rs.getTimestamp("uploaded_at"),
                rs.getBoolean("is_default")
        );
    }
}
