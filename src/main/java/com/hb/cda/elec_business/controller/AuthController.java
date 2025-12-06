package com.hb.cda.elec_business.controller;

import com.hb.cda.elec_business.dto.AuthResponseDto;
import com.hb.cda.elec_business.dto.LoginRequestDto;
import com.hb.cda.elec_business.dto.RegisterRequestDto;
import com.hb.cda.elec_business.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
