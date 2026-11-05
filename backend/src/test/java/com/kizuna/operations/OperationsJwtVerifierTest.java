package com.kizuna.operations;
import com.kizuna.core.security.JwtTokenVerifier;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
class OperationsJwtVerifierTest {
    @Test void missingKeyInvalidSignatureAndExpiredTokensAreRejected() {
        String secret=java.util.UUID.randomUUID().toString()+java.util.UUID.randomUUID();
        String token=Jwts.builder().setSubject("admin@test.invalid").setExpiration(new Date(System.currentTimeMillis()+60000))
            .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))).compact();
        assertNull(new JwtTokenVerifier("").verify(token)); assertNull(new JwtTokenVerifier(secret+"different").verify(token));
        String expired=Jwts.builder().setSubject("admin@test.invalid").setExpiration(new Date(1))
            .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))).compact();
        assertNull(new JwtTokenVerifier(secret).verify(expired));
    }
    @Test void verifiedIdentityDoesNotTrustRoleClaimOrNonExpiringTokens() {
        String secret=java.util.UUID.randomUUID().toString()+java.util.UUID.randomUUID();
        var key=Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        String token=Jwts.builder().setSubject("learner@test.invalid").claim("role","ROLE_ADMIN")
            .setExpiration(new Date(System.currentTimeMillis()+60000)).signWith(key).compact();
        var authentication=new JwtTokenVerifier(secret).verify(token);
        assertNotNull(authentication); assertEquals("learner@test.invalid",authentication.getName()); assertTrue(authentication.getAuthorities().isEmpty());
        assertNull(new JwtTokenVerifier(secret).verify(Jwts.builder().setSubject("learner@test.invalid").signWith(key).compact()));
    }
}
