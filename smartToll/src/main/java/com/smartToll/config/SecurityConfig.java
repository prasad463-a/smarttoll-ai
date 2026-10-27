
package com.smartToll.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.smartToll.jwt.JwtAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
            // Enable CORS at the security level
            .cors(org.springframework.security.config.Customizer.withDefaults())
            // Disable CSRF for REST APIs
            .csrf(csrf -> csrf.disable())

            // Disable default login methods
            .httpBasic(basic -> basic.disable())
            .formLogin(form -> form.disable())

            // No server-side session; use JWT
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // API access rules
            .authorizeHttpRequests(auth -> auth

                // Public endpoints
                .requestMatchers(
                    "/user/register",
                    "/user/login",
                    "/toll/add",
                    "/toll/login",
                    "/error"
                ).permitAll()

                // Only ADMIN can view all users
                .requestMatchers("/user/getall")
                    .hasRole("ADMIN")

                // USER, ADMIN or TOLL_OPERATOR
                .requestMatchers(
                    "/vehicle/**",
                    "/toll/**",
                    "/payment/**",
                    "/anpr/**"
                ).hasAnyRole("USER", "ADMIN", "TOLL_OPERATOR")

                // All other endpoints require authentication
                .anyRequest().authenticated()
            )

            // Run JWT filter before Spring's username/password filter
            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration)
            throws Exception {
        return configuration.getAuthenticationManager();
    }
}