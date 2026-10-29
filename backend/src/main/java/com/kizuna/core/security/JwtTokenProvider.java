package com.kizuna.core.security;

import com.kizuna.core.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Tiện ích tạo, phân tích và kiểm tra tính hợp lệ của JSON Web Token (JWT).
 * Hỗ trợ HMAC-SHA256 bảo mật chuẩn công nghiệp.
 */
@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${app.jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms:86400000}")
    private long jwtExpirationMs;

    private Key getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Tạo JWT Token mới cho người dùng sau khi xác thực.
     */
    public String generateToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        String roleName = (user.getRole() != null) ? user.getRole().getRoleName() : "ROLE_LEARNER";

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getUserId() != null ? user.getUserId().toString() : "");
        claims.put("email", user.getEmail());
        claims.put("fullName", user.getFullName());
        claims.put("role", roleName);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getEmail())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Trích xuất Email (Subject) từ JWT Token.
     */
    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    /**
     * Trích xuất Vai trò (Role) từ JWT Token.
     */
    public String getRoleFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return (String) claims.get("role");
    }

    /**
     * Kiểm tra JWT Token có hợp lệ, chưa hết hạn và đúng chữ ký hay không.
     */
    public boolean validateToken(String authToken) {
        if (authToken == null || authToken.isBlank()) {
            return false;
        }
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(authToken);
            return true;
        } catch (SignatureException ex) {
            log.warn("Chữ ký JWT không hợp lệ: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.warn("Cấu trúc JWT Token không đúng định dạng: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.warn("JWT Token đã hết hạn sử dụng: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.warn("JWT Token không được hỗ trợ: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.warn("Chuỗi JWT claim trống: {}", ex.getMessage());
        }
        return false;
    }
}
