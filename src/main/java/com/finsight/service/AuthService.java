package com.finsight.service;

import com.finsight.dto.ChangePasswordRequest;
import com.finsight.dto.LoginRequest;
import com.finsight.dto.RegisterRequest;
import com.finsight.dto.UpdateSettingsRequest;
import com.finsight.model.AppUser;
import com.finsight.repository.BudgetRepository;
import com.finsight.repository.FinancialGoalRepository;
import com.finsight.repository.TransactionRepository;
import com.finsight.repository.UserRepository;
import com.finsight.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final FinancialGoalRepository goalRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       TransactionRepository transactionRepository,
                       BudgetRepository budgetRepository,
                       FinancialGoalRepository goalRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.budgetRepository = budgetRepository;
        this.goalRepository = goalRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AppUser getCurrentUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    public Map<String, Object> toUserDto(AppUser user) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", user.getId());
        dto.put("name", user.getName());
        dto.put("email", user.getEmail());
        dto.put("phone", user.getPhone() == null ? "" : user.getPhone());
        dto.put("role", user.getRole());
        dto.put("currency", user.getCurrency() == null ? "INR" : user.getCurrency());
        dto.put("savingsTargetPercent", user.getSavingsTargetPercent());
        dto.put("createdAt", user.getCreatedAt() == null ? "" : user.getCreatedAt().toLocalDate().toString());
        return dto;
    }

    public long getRegisteredUserCount() {
        return userRepository.count();
    }

    @Transactional
    public Map<String, Object> register(RegisterRequest request) {
        String email = request.email().toLowerCase().trim();
        if (userRepository.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered. Please sign in.");
        }

        String phone = request.phone() == null ? "" : request.phone().trim();
        String currency = (request.currency() == null || request.currency().isBlank()) ? "INR" : request.currency();

        AppUser user = new AppUser(
            request.name().trim(),
            email,
            passwordEncoder.encode(request.password()),
            phone,
            currency
        );
        AppUser savedUser = userRepository.saveAndFlush(user);

        return Map.of(
            "message", "Registration successful! Your account is ready.",
            "token", jwtService.generateToken(savedUser),
            "user", toUserDto(savedUser)
        );
    }

    public Map<String, Object> login(LoginRequest request) {
        AppUser user = userRepository.findByEmail(request.email().toLowerCase().trim())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        return Map.of("token", jwtService.generateToken(user), "user", toUserDto(user));
    }

    @Transactional
    public Map<String, Object> updateSettings(AppUser user, UpdateSettingsRequest request) {
        user.setName(request.name().trim());
        user.setPhone(request.phone() == null ? "" : request.phone().trim());
        user.setCurrency(request.currency());
        user.setSavingsTargetPercent(request.savingsTargetPercent());
        AppUser saved = userRepository.saveAndFlush(user);
        return toUserDto(saved);
    }

    @Transactional
    public void changePassword(AppUser user, ChangePasswordRequest request) {
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.saveAndFlush(user);
    }
}
