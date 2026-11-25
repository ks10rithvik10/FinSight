package com.finsight.controller;

import com.finsight.dto.ChangePasswordRequest;
import com.finsight.dto.UpdateSettingsRequest;
import com.finsight.model.AppUser;
import com.finsight.service.AuthService;
import com.finsight.service.FinanceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final AuthService authService;
    private final FinanceService financeService;

    public SettingsController(AuthService authService, FinanceService financeService) {
        this.authService = authService;
        this.financeService = financeService;
    }

    @GetMapping
    public Map<String, Object> getSettings(Authentication auth) {
        return authService.toUserDto(authService.getCurrentUser(auth));
    }

    @PutMapping("/profile")
    public Map<String, Object> updateProfile(@Valid @RequestBody UpdateSettingsRequest request,
                                             Authentication auth) {
        AppUser user = authService.getCurrentUser(auth);
        return authService.updateSettings(user, request);
    }

    @PutMapping("/password")
    public Map<String, Object> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                              Authentication auth) {
        AppUser user = authService.getCurrentUser(auth);
        authService.changePassword(user, request);
        return Map.of("message", "Password updated successfully");
    }

    @DeleteMapping("/data")
    public ResponseEntity<Void> clearData(Authentication auth) {
        AppUser user = authService.getCurrentUser(auth);
        financeService.clearAllUserData(user);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/account")
    public ResponseEntity<Void> deleteAccount(Authentication auth) {
        AppUser user = authService.getCurrentUser(auth);
        financeService.deleteUserAccount(user);
        return ResponseEntity.noContent().build();
    }
}
