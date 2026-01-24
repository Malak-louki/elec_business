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
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegisterRequestDto request) {
        var response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDto requestDto) {
        try {
            AuthResponseDto responseDto = authService.login(requestDto);
            return ResponseEntity.ok(responseDto);
        } catch (BadCredentialsException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "invalid_credentials");
            error.put("message", "Identifiant ou mot de passe incorrect.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        } catch (DisabledException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "account_disabled");
            error.put("message", "Votre compte n'est pas activé. Vérifiez votre email.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }
    }

    /**
     * Retourne les infos de l'utilisateur actuellement authentifié (à partir du JWT).
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> me(Authentication authentication) {
        // Principal posé par JwtAuthenticationFilter
        User user = (User) authentication.getPrincipal();
        UserResponseDto dto = UserMapper.toDto(user);
        return ResponseEntity.ok(dto);
    }

}
