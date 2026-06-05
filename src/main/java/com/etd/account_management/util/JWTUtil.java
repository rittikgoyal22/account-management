package com.etd.account_management.util;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Date;

@Component
public class JWTUtil {

    private static final Logger logger = LoggerFactory.getLogger(JWTUtil.class);
    private static final long TOKEN_VALIDITY_MS = 1000L * 60 * 60; // 1 hour

    private final SecretKey key;

    public JWTUtil() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("HmacSHA256");
            SecretKey sKey = keyGenerator.generateKey();
            String secretKey = Base64.getEncoder().encodeToString(sKey.getEncoded());
            this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
        } catch (NoSuchAlgorithmException e) {
            logger.error("Error generating JWT signing key: {}", e.getMessage());
            throw new IllegalStateException("Unable to initialize JWT signing key", e);
        }
    }

    public String generateToken(String username) {
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + TOKEN_VALIDITY_MS))
                .signWith(key)
                .compact();
    }

    public String extractUsername(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        Date expiration = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration();
        return expiration.before(new Date(System.currentTimeMillis()));
    }

    // extracts expiry even from already-expired tokens (catches ExpiredJwtException)
    // used by TokenBlacklistService to persist the expiry alongside the blacklisted token
    public LocalDateTime extractExpiration(String token) {
        try {
            Date expiration = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getExpiration();
            return expiration.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        } catch (ExpiredJwtException e) {
            // token is already expired but we can still read the expiry claim
            return e.getClaims().getExpiration()
                    .toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        }
    }

}
