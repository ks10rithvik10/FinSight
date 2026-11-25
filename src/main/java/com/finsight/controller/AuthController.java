package com.finsight.controller;

import com.finsight.dto.LoginRequest;
import com.finsight.dto.RegisterRequest;
import com.finsight.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/auth/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/auth/login")
    public Map<String, Object> login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/auth/stats")
    public Map<String, Object> authStats() {
        return Map.of("registeredUsers", authService.getRegisteredUserCount());
    }

    @GetMapping("/me")
    public Map<String, Object> currentUser(Authentication authentication) {
        return authService.toUserDto(authService.getCurrentUser(authentication));
    }
}
