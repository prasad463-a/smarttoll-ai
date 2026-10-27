
package com.smartToll.jwt;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {

    private static final String SECRET =
            "SmartTollProjectSecretKeyForJwtAuthentication123456";

    private final SecretKey key =
            Keys.hmacShaKeyFor(SECRET.getBytes());

    // Token validity: 24 hours
    private static final long EXPIRATION_TIME =
            1000L * 60 * 60 * 24;

    // Generate JWT token
    public String generateToken(String email) {

        long currentTime = System.currentTimeMillis();

        Date issuedAt = new Date(currentTime);
        Date expiration = new Date(currentTime + EXPIRATION_TIME);

        return Jwts.builder()
                .subject(email)
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

    // Extract email from token
    public String extractEmail(String token) {

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    // Check whether token is expired
    public boolean isTokenExpired(String token) {

        Date expiration = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration();

        return expiration.before(new Date());
    }

    // Validate token
    public boolean validateToken(String token, String email) {

        String extractedEmail = extractEmail(token);

        return extractedEmail.equals(email)
                && !isTokenExpired(token);
    }
}