package net.simplehardware.engine.server.database.repositories.impl;

import net.simplehardware.engine.server.database.models.Maze;
import net.simplehardware.engine.server.database.repositories.MazeRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of MazeRepository for SQLite
 */
public class MazeRepositoryImpl implements MazeRepository {
    private final Connection connection;

    public MazeRepositoryImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Maze createMaze(String name, String filePath, int minSteps, int forms, int size, Maze.Difficulty difficulty)
            throws SQLException {
        String sql = "INSERT INTO mazes (name, file_path, min_steps, forms, size, difficulty) VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, filePath);
            pstmt.setInt(3, minSteps);
            pstmt.setInt(4, forms);
            pstmt.setInt(5, size);
            pstmt.setString(6, difficulty.name());
            pstmt.executeUpdate();

            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                if (rs.next()) {
                    return getMazeById(rs.getInt(1));
                }
            }
        }
        return null;
    }

    @Override
    public Maze getMazeById(int id) throws SQLException {
        String sql = "SELECT * FROM mazes WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToMaze(rs);
            }
        }
        return null;
    }

    @Override
    public String getMazeName(int mazeId) throws SQLException {
        String sql = "SELECT name FROM mazes WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, mazeId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("name");
            }
        }
        return "Unknown Maze";
    }

    @Override
    public List<Maze> getActiveMazes() throws SQLException {
        String sql = "SELECT * FROM mazes WHERE active = 1 ORDER BY created_at DESC";
        List<Maze> mazes = new ArrayList<>();

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                mazes.add(mapResultSetToMaze(rs));
            }
        }
        return mazes;
    }

    @Override
    public List<Maze> getAllMazes() throws SQLException {
        String sql = "SELECT * FROM mazes ORDER BY created_at DESC";
        List<Maze> mazes = new ArrayList<>();

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                mazes.add(mapResultSetToMaze(rs));
            }
        }
        return mazes;
    }

    @Override
    public List<Maze> getUnplayedMazes(int userId, String difficulty) throws SQLException {
        String sql = "SELECT m.* FROM mazes m " +
                "LEFT JOIN game_results gr ON m.id = gr.maze_id AND gr.user_id = ? " +
                "WHERE m.active = 1 AND gr.id IS NULL";

        if (difficulty != null) {
            sql += " AND m.difficulty = ?";
        }

        List<Maze> mazes = new ArrayList<>();

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            if (difficulty != null) {
                pstmt.setString(2, difficulty);
            }
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                mazes.add(mapResultSetToMaze(rs));
            }
        }
        return mazes;
    }

    @Override
    public List<Maze> getMazesByDifficulty(Maze.Difficulty difficulty) throws SQLException {
        String sql = "SELECT * FROM mazes WHERE active = 1 AND difficulty = ? ORDER BY created_at DESC";
        List<Maze> mazes = new ArrayList<>();

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, difficulty.name());
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                mazes.add(mapResultSetToMaze(rs));
            }
        }
        return mazes;
    }

    @Override
    public Maze getRandomMaze(Maze.Difficulty difficulty) throws SQLException {
        String sql = "SELECT * FROM mazes WHERE active = 1 AND difficulty = ? ORDER BY RANDOM() LIMIT 1";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, difficulty.name());
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToMaze(rs);
            }
        }
        return null;
    }

    @Override
    public int deactivateOldMazes(int daysOld) throws SQLException {
        String sql = "UPDATE mazes SET active = 0 WHERE created_at < datetime('now', '-' || ? || ' days')";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, daysOld);
            return pstmt.executeUpdate();
        }
    }

    @Override
    public void setMazeActive(int mazeId, boolean active) throws SQLException {
        String sql = "UPDATE mazes SET active = ? WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setBoolean(1, active);
            pstmt.setInt(2, mazeId);
            pstmt.executeUpdate();
        }
    }

    private Maze mapResultSetToMaze(ResultSet rs) throws SQLException {
        return new Maze(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("file_path"),
                rs.getInt("min_steps"),
                rs.getInt("forms"),
                rs.getInt("size"),
                Maze.Difficulty.valueOf(rs.getString("difficulty")),
                rs.getTimestamp("created_at"),
                rs.getBoolean("active")
        );
    }
}
