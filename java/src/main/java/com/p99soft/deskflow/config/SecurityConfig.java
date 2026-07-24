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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Spring Security configuration — authentication, authorization, and CORS.
 *
 * <h3>Role matrix</h3>
 * <pre>
 * ┌────────────────────────────────────────┬──────────┬───────┬───────┐
 * │ Endpoint                               │ EMPLOYEE │ AGENT │ ADMIN │
 * ├────────────────────────────────────────┼──────────┼───────┼───────┤
 * │ POST   /api/v1/auth/login              │    ✅    │  ✅   │  ✅   │ (public)
 * │ POST   /api/v1/auth/register           │    ❌    │  ❌   │  ✅   │
 * ├────────────────────────────────────────┼──────────┼───────┼───────┤
 * │ POST   /api/v1/tickets                 │    ✅    │  ❌   │  ❌   │ (create)
 * │ GET    /api/v1/tickets                 │    ✅    │  ✅   │  ✅   │ (list/view)
 * │ GET    /api/v1/tickets/{id}            │    ✅    │  ✅   │  ✅   │
 * │ PUT    /api/v1/tickets/{id}            │    ❌    │  ✅   │  ✅   │ (assign/update)
 * ├────────────────────────────────────────┼──────────┼───────┼───────┤
 * │ POST   /api/v1/tickets/{id}/comments   │    ✅    │  ✅   │  ✅   │
 * │ GET    /api/v1/tickets/{id}/comments   │    ✅    │  ✅   │  ✅   │
 * │ GET    /api/v1/tickets/{id}/activities │    ✅    │  ✅   │  ✅   │
 * └────────────────────────────────────────┴──────────┴───────┴───────┘
 * </pre>
 *
 * <p>Rules are defined at URL level in {@link #securityFilterChain} (coarse-grained)
 * and reinforced with {@code @PreAuthorize} on individual methods (fine-grained).</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity          // enables @PreAuthorize, @PostAuthorize
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsService          userDetailsService;
    private final JwtAuthenticationFilter     jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler      jwtAccessDeniedHandler;
    private final CorsProperties              corsProperties;

    // ------------------------------------------------------------------ //
    // Core beans
    // ------------------------------------------------------------------ //

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

    // ------------------------------------------------------------------ //
    // CORS
    // ------------------------------------------------------------------ //

    /**
     * Reads allowed origins, methods, and headers from {@link CorsProperties}
     * (bound from {@code application.yaml} / environment variables).
     *
     * <p>In production set {@code CORS_ALLOWED_ORIGINS} to your frontend domain(s).</p>
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(corsProperties.getAllowedOrigins());
        config.setAllowedMethods(corsProperties.getAllowedMethods());
        config.setAllowedHeaders(corsProperties.getAllowedHeaders());
        config.setAllowCredentials(corsProperties.isAllowCredentials());
        config.setMaxAge(corsProperties.getMaxAge());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    // ------------------------------------------------------------------ //
    // Security filter chain
    // ------------------------------------------------------------------ //

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // ── CORS ──────────────────────────────────────────────────────────
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // ── CSRF — disabled for stateless JWT REST API ────────────────────
                .csrf(AbstractHttpConfigurer::disable)

                // ── Exception handlers ────────────────────────────────────────────
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)  // 401
                        .accessDeniedHandler(jwtAccessDeniedHandler)             // 403
                )

                // ── Authorization rules ───────────────────────────────────────────
                .authorizeHttpRequests(auth -> auth

                        // ── Public ────────────────────────────────────────────────
                        .requestMatchers(HttpMethod.POST,  "/api/v1/auth/login").permitAll()
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/actuator/health"
                        ).permitAll()

                        // ── Auth — ADMIN only ──────────────────────────────────────
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/register")
                                .hasRole("ADMIN")

                        // ── Tickets — create: EMPLOYEE only ───────────────────────
                        // (only employees raise tickets)
                        .requestMatchers(HttpMethod.POST, "/api/v1/tickets")
                                .hasRole("EMPLOYEE")

                        // ── Tickets — update/assign: AGENT and ADMIN only ──────────
                        // (EMPLOYEEs cannot reassign or change ticket status)
                        .requestMatchers(HttpMethod.PUT, "/api/v1/tickets/**")
                                .hasAnyRole("AGENT", "ADMIN")

                        // ── Tickets — read: all authenticated users ────────────────
                        .requestMatchers(HttpMethod.GET, "/api/v1/tickets/**")
                                .hasAnyRole("EMPLOYEE", "AGENT", "ADMIN")

                        // ── Comments & Activities — all authenticated users ─────────
                        .requestMatchers("/api/v1/tickets/*/comments/**")
                                .hasAnyRole("EMPLOYEE", "AGENT", "ADMIN")
                        .requestMatchers("/api/v1/tickets/*/activities/**")
                                .hasAnyRole("EMPLOYEE", "AGENT", "ADMIN")

                        // ── Catch-all: any remaining endpoint requires a valid JWT ──
                        .anyRequest().authenticated()
                )

                // ── Session — stateless ────────────────────────────────────────────
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authenticationProvider(authenticationProvider())

                // JWT filter runs before Spring's default username/password filter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
