package com.hb.cda.elec_business.controller;

import com.hb.cda.elec_business.dto.auth.AuthResponseDto;
import com.hb.cda.elec_business.dto.auth.LoginRequestDto;
import com.hb.cda.elec_business.dto.auth.RegisterRequestDto;
import com.hb.cda.elec_business.dto.auth.UserResponseDto;
import com.hb.cda.elec_business.entity.User;
import com.hb.cda.elec_business.mapper.UserMapper;
import com.hb.cda.elec_business.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegisterRequestDto request) {
        log.info("=== REGISTER REQUEST RECEIVED ===");
        log.info("Email: {}", request.getEmail());
        log.info("Username: {}", request.getUsername());
        log.info("Phone: {}", request.getPhone());

        try {
            log.info("Calling authService.register()...");
            AuthResponseDto response = authService.register(request);
            log.info("Registration successful for: {}", request.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("=== REGISTRATION ERROR ===");
            log.error("Error class: {}", e.getClass().getName());
            log.error("Error message: {}", e.getMessage());
            log.error("Full stacktrace:", e);
            throw e; // Relance l'exception pour que Spring la gère
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDto requestDto) {
        log.info("=== LOGIN REQUEST RECEIVED ===");
        log.info("Email: {}", requestDto.getEmail());

        try {
            log.info("Calling authService.login()...");
            AuthResponseDto responseDto = authService.login(requestDto);
            log.info("Login successful for: {}", requestDto.getEmail());
            return ResponseEntity.ok(responseDto);
        } catch (BadCredentialsException e) {
            log.warn("Login failed - Bad credentials for: {}", requestDto.getEmail());
            Map<String, String> error = new HashMap<>();
            error.put("error", "invalid_credentials");
            error.put("message", "Identifiant ou mot de passe incorrect.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        } catch (DisabledException e) {
            log.warn("Login failed - Account disabled for: {}", requestDto.getEmail());
            Map<String, String> error = new HashMap<>();
            error.put("error", "account_disabled");
            error.put("message", "Votre compte n'est pas activé. Vérifiez votre email.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        } catch (Exception e) {
            log.error("=== LOGIN ERROR ===");
            log.error("Error class: {}", e.getClass().getName());
            log.error("Error message: {}", e.getMessage());
            log.error("Full stacktrace:", e);
            throw e;
        }
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> me(Authentication authentication) {
        log.debug("GET /me called");
        User user = (User) authentication.getPrincipal();
        UserResponseDto dto = UserMapper.toDto(user);
        return ResponseEntity.ok(dto);
    }
}