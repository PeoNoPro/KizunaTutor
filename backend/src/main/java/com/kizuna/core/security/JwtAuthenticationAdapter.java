package com.kizuna.core.security;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.context.SecurityContextHolder;
import java.io.IOException;
import java.util.List;
public class JwtAuthenticationAdapter extends OncePerRequestFilter {
    private final JwtTokenVerifier verifier;
    private final String devLoginEmail;
    public JwtAuthenticationAdapter(JwtTokenVerifier verifier) { this(verifier,null); }
    /**
     * Phiên phát triển được SecurityConfig giới hạn bằng profile, địa chỉ và DB cục bộ riêng.
     * Chỉ nhận request loopback không mang token tới API vận hành; quyền vẫn được kiểm tra trong DB.
     */
    public JwtAuthenticationAdapter(JwtTokenVerifier verifier,String devLoginEmail) {
        this.verifier=verifier; this.devLoginEmail=devLoginEmail==null || devLoginEmail.isBlank() ? null : devLoginEmail.trim();
    }
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
        if(SecurityContextHolder.getContext().getAuthentication()==null) {
            String suppliedToken=token(request);
            var authentication=verifier.verify(suppliedToken);
            String remote=request.getRemoteAddr();
            boolean loopback="127.0.0.1".equals(remote) || "::1".equals(remote) || "0:0:0:0:0:0:0:1".equals(remote);
            if(authentication==null && suppliedToken==null && devLoginEmail!=null && loopback
                    && (request.getRequestURI().startsWith("/api/admin/operations/")
                        || request.getRequestURI().equals("/api/v1/tickets")
                        || request.getRequestURI().startsWith("/api/v1/tickets/"))) {
                authentication=new UsernamePasswordAuthenticationToken(devLoginEmail,null,List.of());
            }
            if(authentication!=null) SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        chain.doFilter(request,response);
    }
    private String token(HttpServletRequest request) {
        String header=request.getHeader("Authorization");
        if(header!=null && header.startsWith("Bearer ")) return header.substring(7);
        if(request.getCookies()!=null) for(var cookie:request.getCookies()) {
            if("access_token".equals(cookie.getName()) || "jwt_token".equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }
}
