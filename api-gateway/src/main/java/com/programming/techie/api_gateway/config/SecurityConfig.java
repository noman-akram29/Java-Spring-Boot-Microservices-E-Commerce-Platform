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
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.HttpBasicServerAuthenticationEntryPoint;

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
        public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
                return http
                                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                                .authorizeExchange(exchanges -> exchanges
                                                .pathMatchers(
                                                                "/actuator/health/**",
                                                                "/actuator/info",
                                                                "/actuator/prometheus")
                                                .permitAll()
                                                .anyExchange().authenticated())
                                .httpBasic(httpBasic -> httpBasic
                                                .authenticationEntryPoint(
                                                                new HttpBasicServerAuthenticationEntryPoint()))
                                .build();
        }

        @Bean
        public MapReactiveUserDetailsService userDetailsService(PasswordEncoder encoder) {
                UserDetails user = User.withUsername(username)
                                .password(encoder.encode(password))
                                .roles("USER")
                                .build();
                return new MapReactiveUserDetailsService(user);
        }
}