package com.SocializerAI.config;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String header = request.getHeader("Authorization");
            logger.debug("Authorization header: {}", header);

            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring(7);
                logger.debug("Extracted token: {}", token.substring(0, Math.min(20, token.length())) + "...");

                // Extract subject (could be UUID or username/email)
                String subject = jwtUtil.extractSubject(token);
                logger.debug("Token validated for subject: {}", subject);

                // Extract roles claim and build authorities
                String rolesCsv = jwtUtil.extractRoles(token);
                var authorities = Collections.<SimpleGrantedAuthority>emptyList();
                if (rolesCsv != null && !rolesCsv.isBlank()) {
                    var list = new java.util.ArrayList<SimpleGrantedAuthority>();
                    for (String r : rolesCsv.split(",")) {
                        String role = r.trim();
                        if (!role.isEmpty()) {
                            if (!role.startsWith("ROLE_")) role = "ROLE_" + role;
                            list.add(new SimpleGrantedAuthority(role));
                        }
                    }
                    authorities = list;
                }

                // Build authentication object with authorities
                var auth = new UsernamePasswordAuthenticationToken(subject, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
                logger.debug("Authentication set for subject: {}", subject);
            } else {
                logger.debug("No Bearer token found in header");
            }
        } catch (JwtException | IllegalArgumentException e) {
            logger.error("JWT validation failed", e);
            // Clear context on failure and continue.
            // Public endpoints should still be reachable even if a stale token is sent.
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
