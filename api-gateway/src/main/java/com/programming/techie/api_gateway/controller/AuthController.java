package com.programming.techie.api_gateway.controller;

import com.programming.techie.api_gateway.service.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class AuthController {
    private final MapReactiveUserDetailsService users;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;

    public AuthController(MapReactiveUserDetailsService users, PasswordEncoder encoder, JwtService jwtService) {
        this.users = users;
        this.encoder = encoder;
        this.jwtService = jwtService;
    }

    public record LoginRequest(String username, String password) {
    }

    public record LoginResponse(String accessToken, String tokenType, long expiresInMinutes) {
    }

    @PostMapping("/auth/login")
    public Mono<ResponseEntity<LoginResponse>> login(@RequestBody LoginRequest req) {
        return users.findByUsername(req.username())
                .filter(u -> encoder.matches(req.password(), u.getPassword()))
                .map(u -> ResponseEntity.ok(new LoginResponse(
                        jwtService.createToken(u.getUsername()),
                        "Bearer",
                        60L)))
                .defaultIfEmpty(ResponseEntity.status(401).build());
    }
}