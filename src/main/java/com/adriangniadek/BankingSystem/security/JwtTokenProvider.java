package com.adriangniadek.BankingSystem.security;

import com.adriangniadek.BankingSystem.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;

@Component
public class JwtTokenProvider {

    private final JwtProperties properties;
    private final SecretKey signingKey;
    private final JwtParser jwtParser;

    public JwtTokenProvider(JwtProperties properties) {
        this.properties = properties;
        this.signingKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.jwtParser = Jwts.parser().verifyWith(signingKey).build();
    }

    public String generateToken(String username) {
        Instant now = Instant.now();
        Instant expiration = now.plus(properties.expiration());

        return Jwts.builder()
                .subject(username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(signingKey)
                .compact();
    }

    public String getUsernameFromJWT(String token) {
        Claims claims = jwtParser.parseSignedClaims(token).getPayload();
        return claims.getSubject();
    }

    public boolean validateToken(String token) {
        try {
            jwtParser.parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }
}

