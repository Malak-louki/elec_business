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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto requestDto){
        AuthResponseDto responseDto = authService.login(requestDto);
        return ResponseEntity.ok(responseDto);
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
