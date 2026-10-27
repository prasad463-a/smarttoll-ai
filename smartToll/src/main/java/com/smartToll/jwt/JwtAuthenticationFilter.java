
package com.smartToll.jwt;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.smartToll.entity.Toll;
import com.smartToll.entity.User;
import com.smartToll.repository.TollRepository;
import com.smartToll.repository.UserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final TollRepository tollRepository;

    public JwtAuthenticationFilter(
            JwtUtil jwtUtil,
            UserRepository userRepository,
            TollRepository tollRepository) {

        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.tollRepository = tollRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // 1. Check Bearer token
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7).trim();

        if (token.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 2. Extract email from JWT
            String email = jwtUtil.extractEmail(token);

            // 3. Validate token
            if (!jwtUtil.validateToken(token, email)) {
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }

            // 4. First check normal users
            User user = userRepository.findByEmail(email)
                    .orElse(null);

            if (user != null) {

                String role = normalizeRole(user.getRole());

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                user,
                                null,
                                List.of(
                                    new SimpleGrantedAuthority(role)
                                )
                        );

                authentication.setDetails(
                        new WebAuthenticationDetailsSource()
                                .buildDetails(request)
                );

                SecurityContextHolder.getContext()
                        .setAuthentication(authentication);

            } else {

                // 5. If not a user, check toll operator
                Toll toll = tollRepository.findByEmail(email)
                        .orElse(null);

                if (toll != null) {

                    String role = "ROLE_TOLL_OPERATOR";

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    toll,
                                    null,
                                    List.of(
                                        new SimpleGrantedAuthority(role)
                                    )
                            );

                    authentication.setDetails(
                            new WebAuthenticationDetailsSource()
                                    .buildDetails(request)
                    );

                    SecurityContextHolder.getContext()
                            .setAuthentication(authentication);
                }
            }

        } catch (Exception exception) {
            SecurityContextHolder.clearContext();
        }

        // 6. Continue request
        filterChain.doFilter(request, response);
    }

    private String normalizeRole(String role) {

        if (role == null || role.isBlank()) {
            return "ROLE_USER";
        }

        role = role.trim().toUpperCase();

        if (!role.startsWith("ROLE_")) {
            role = "ROLE_" + role;
        }

        return role;
    }
}