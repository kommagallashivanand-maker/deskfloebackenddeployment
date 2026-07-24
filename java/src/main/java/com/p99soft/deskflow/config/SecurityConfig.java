package com.p99soft.deskflow.config;

import com.p99soft.deskflow.security.JwtAccessDeniedHandler;
import com.p99soft.deskflow.security.JwtAuthenticationEntryPoint;
import com.p99soft.deskflow.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration.
 *
 * <p>Access rules:</p>
 * <ul>
 *   <li>{@code POST /api/v1/auth/login}     — public (no token required)</li>
 *   <li>{@code POST /api/v1/auth/register}  — ADMIN only (requires valid JWT with ADMIN role)</li>
 *   <li>All other endpoints                 — any authenticated user (valid JWT)</li>
 * </ul>
 *
 * <p>Security features:</p>
 * <ul>
 *   <li>BCrypt password hashing</li>
 *   <li>Stateless sessions — no HTTP session created or used</li>
 *   <li>CSRF disabled — REST API uses JWT, not cookies</li>
 *   <li>{@link JwtAuthenticationFilter} validates tokens on every request</li>
 *   <li>{@link JwtAuthenticationEntryPoint} returns structured 401 JSON for unauthenticated requests</li>
 *   <li>{@link JwtAccessDeniedHandler} returns structured 403 JSON for insufficient role</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsService          userDetailsService;
    private final JwtAuthenticationFilter     jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler      jwtAccessDeniedHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF — not needed for stateless REST APIs using JWT
                .csrf(AbstractHttpConfigurer::disable)

                // Exception handlers — return structured JSON instead of HTML error pages
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)  // 401 — no/invalid token
                        .accessDeniedHandler(jwtAccessDeniedHandler)             // 403 — wrong role
                )

                // Endpoint access rules
                .authorizeHttpRequests(auth -> auth
                        // Login is fully public
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        // Register is restricted to ADMIN only
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/register").hasRole("ADMIN")
                        // API docs and health check are public
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/actuator/health"
                        ).permitAll()
                        // Everything else requires a valid JWT
                        .anyRequest().authenticated()
                )

                // Stateless — no HTTP session created or used
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authenticationProvider(authenticationProvider())

                // JWT filter runs before Spring's username/password filter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
