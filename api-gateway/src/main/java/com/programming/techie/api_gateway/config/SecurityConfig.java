package com.programming.techie.api_gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Security configuration for the API Gateway.
 *
 * - Public endpoints: /auth/login + selected actuator endpoints.
 * - Everything else requires a valid JWT (OAuth2 Resource Server).
 * - MapReactiveUserDetailsService + PasswordEncoder are used ONLY by
 * AuthController
 * to validate the username/password pair that is exchanged for a JWT.
 * They are NOT used for HTTP Basic authentication on other endpoints.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

        @Value("${gateway.auth.username:gateway}")
        private String username;

        @Value("${gateway.auth.password}")
        private String password;

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http, ReactiveJwtDecoder jwtDecoder) {
                return http
                                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                                .authorizeExchange(exchanges -> exchanges
                                                .pathMatchers(
                                                                "/actuator/health/**",
                                                                "/actuator/info",
                                                                "/actuator/prometheus",
                                                                "/auth/login",
                                                                "/swagger-ui.html",
                                                                "/swagger-ui/**",
                                                                "/api-docs",
                                                                "/api-docs/**",
                                                                "/v3/api-docs",
                                                                "/v3/api-docs/**",
                                                                "/webjars/**",
                                                                "/api/product/v3/api-docs",
                                                                "/api/product/v3/api-docs/**",
                                                                "/api/order/v3/api-docs",
                                                                "/api/order/v3/api-docs/**",
                                                                "/api/inventory/v3/api-docs",
                                                                "/api/inventory/v3/api-docs/**")
                                                .permitAll()
                                                .anyExchange().authenticated())
                                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtDecoder(jwtDecoder)))
                                .build();
        }

        /**
         * HS256 JWT decoder. The secret must be at least 32 characters (256 bits).
         */
        @Bean
        public ReactiveJwtDecoder jwtDecoder(@Value("${gateway.jwt.secret}") String secret) {
                if (secret == null || secret.length() < 32) {
                        throw new IllegalStateException(
                                        "gateway.jwt.secret must be at least 32 characters long for HS256. "
                                                        + "Current length: " + (secret == null ? 0 : secret.length()));
                }
                SecretKeySpec key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
                return NimbusReactiveJwtDecoder.withSecretKey(key).build();
        }

        /**
         * In-memory user store used exclusively by AuthController for the /auth/login
         * exchange.
         * This is intentionally simple for demo purposes; replace with a real user
         * service in production.
         */
        @Bean
        public MapReactiveUserDetailsService userDetailsService(PasswordEncoder encoder) {
                UserDetails user = User.withUsername(username)
                                .password(encoder.encode(password))
                                .roles("USER")
                                .build();
                return new MapReactiveUserDetailsService(user);
        }
}