package com.hb.cda.elec_business.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();
        log.info("🔍 Request: {} {}", request.getMethod(), path);

        String authHeader = request.getHeader("Authorization");

        // 🔹 1. Pas de header Bearer → on laisse passer
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.info("⚠️ No Bearer token found, continuing...");
            filterChain.doFilter(request, response);
            return;
        }

        // 🔹 2. On extrait le token
        String jwt = authHeader.substring(7);

        try {
            String username = jwtService.extractUsernameFromToken(jwt);
            log.info("✅ Token valid for user: {}", username);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.info("🔐 User authenticated: {}", username);
                }
            }
        } catch (Exception e) {
            log.warn("❌ JWT invalide: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
