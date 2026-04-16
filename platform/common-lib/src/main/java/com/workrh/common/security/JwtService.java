package com.workrh.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtService {

    @Value("${security.jwt.secret}")
    private String secret;

    @Value("${security.jwt.expiration:86400}")
    private long expirationInSeconds;

    public String generateToken(String username, String tenantId, List<String> roles) {
        return generateToken(username, tenantId, roles, null);
    }

    public String generateToken(String username, String tenantId, List<String> roles, Long employeeId) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .subject(username)
                .claim("tenantId", tenantId)
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirationInSeconds)))
                .signWith(signingKey());
        if (employeeId != null) {
            builder.claim("employeeId", employeeId);
        }
        return builder.compact();
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser().verifyWith((javax.crypto.SecretKey) signingKey()).build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public boolean isTokenValid(String token) {
        Claims claims = extractAllClaims(token);
        return claims.getExpiration().after(new Date());
    }

    private Key signingKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }
}
