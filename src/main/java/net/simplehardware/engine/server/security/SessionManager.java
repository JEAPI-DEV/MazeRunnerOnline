package net.simplehardware.engine.server.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Session manager using JWT tokens
 */
public class SessionManager {
    private static final long SESSION_TIMEOUT_MS = 60 * 60 * 1000; // 1 hour
    private final SecretKey secretKey;

    public SessionManager(String secret) {
        // Generate a secure key from the secret
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * Create a session token for a user
     * 
     * @param userId   The user ID
     * @param username The username
     * @return JWT token string
     */
    public String createSession(int userId, String username) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + SESSION_TIMEOUT_MS);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);

        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    /**
     * Validate and parse a session token
     * 
     * @param token The JWT token
     * @return SessionData if valid, null otherwise
     */
    public SessionData validateSession(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            int userId = claims.get("userId", Integer.class);
            String username = claims.get("username", String.class);

            return new SessionData(userId, username);
        } catch (Exception e) {
            return null;
        }
    }

    /**
         * Session data class
         */
    public record SessionData(int userId, String username) { }
}
