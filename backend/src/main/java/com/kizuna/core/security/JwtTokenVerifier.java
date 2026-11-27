package com.kizuna.core.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Verifies existing auth tokens; never issues tokens or provides a default signing secret. */
@Component
public class JwtTokenVerifier {
    private final String secret;
    public JwtTokenVerifier(@Value("${app.jwt.secret:${JWT_SECRET:}}") String secret) { this.secret=secret; }
    public Authentication verify(String token) {
        if(secret==null || secret.getBytes(StandardCharsets.UTF_8).length<32 || token==null || token.isBlank()) return null;
        try {
            var key=Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
            var claims=Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
            if(claims.getExpiration()==null || claims.getSubject()==null || claims.getSubject().isBlank()) return null;
            // Database role/account checks remain authoritative on every FE-11 request.
            return new UsernamePasswordAuthenticationToken(claims.getSubject(),null,List.of());
        } catch(JwtException|IllegalArgumentException ex) { return null; }
    }
}
