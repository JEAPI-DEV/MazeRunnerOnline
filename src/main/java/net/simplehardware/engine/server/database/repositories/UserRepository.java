package net.simplehardware.engine.server.database.repositories;

import net.simplehardware.engine.server.database.models.User;

import java.sql.SQLException;
import java.util.List;

/**
 * Repository interface for User entity operations
 */
public interface UserRepository {
    /**
     * Create a new user
     */
    User createUser(String username, String passwordHash) throws SQLException;

    /**
     * Get user by ID
     */
    User getUserById(int id) throws SQLException;

    /**
     * Get user by username
     */
    User getUserByUsername(String username) throws SQLException;

    /**
     * Search users by username (partial match)
     */
    List<User> searchUsersByUsername(String query) throws SQLException;
}
