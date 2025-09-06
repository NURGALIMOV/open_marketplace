package com.openmarket.service;

import com.openmarket.dto.Role;
import com.openmarket.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for JWT token generation and validation
 */
@Service
@Slf4j
public class JwtService {

    private final SecretKey secretKey;
    private final long expirationTime;

    public JwtService(@Value("${app.security.jwt.secret}") String secret,
                     @Value("${app.security.jwt.expiration}") long expirationTime) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.expirationTime = expirationTime;
    }

    /**
     * Generate JWT token for user
     * @param user The user to generate token for
     * @return JWT token string
     */
    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", user.getEmail());
        claims.put("role", user.getRole().name());
        Instant now = Instant.now();
        Instant expiration = now.plus(expirationTime, ChronoUnit.MILLIS);
        return Jwts.builder()
                .claims(claims)
                .subject(user.getId().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Extract user ID from JWT token
     * @param token JWT token
     * @return User ID
     */
    public UUID extractUserId(String token) {
        String userIdString = extractClaim(token, Claims::getSubject);
        return UUID.fromString(userIdString);
    }

    /**
     * Extract email from JWT token
     * @param token JWT token
     * @return User email
     */
    public String extractEmail(String token) {
        return extractClaim(token, claims -> claims.get("email", String.class));
    }

    /**
     * Extract role from JWT token
     * @param token JWT token
     * @return User role
     */
    public Role extractRole(String token) {
        String roleString = extractClaim(token, claims -> claims.get("role", String.class));
        return Role.valueOf(roleString);
    }

    /**
     * Extract expiration date from JWT token
     * @param token JWT token
     * @return Expiration date
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Check if JWT token is expired
     * @param token JWT token
     * @return true if expired, false otherwise
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = extractExpiration(token);
            return expiration.before(new Date());
        } catch (Exception e) {
            log.error("Token is expired: {}", e.getMessage());
            return true;
        }
    }

    /**
     * Validate JWT token
     * @param token JWT token
     * @return true if valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token);
            return !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extract specific claim from JWT token
     * @param token JWT token
     * @param claimsResolver Function to extract claim
     * @param <T> Type of claim
     * @return Extracted claim
     */
    private <T> T extractClaim(String token, java.util.function.Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extract all claims from JWT token
     * @param token JWT token
     * @return Claims object
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Get token expiration time in seconds
     * @return Expiration time in seconds
     */
    public long getExpirationTimeSeconds() {
        return expirationTime / 1000;
    }
}
