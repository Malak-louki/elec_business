package com.hb.cda.elec_business.service.impl;

import com.hb.cda.elec_business.dto.AuthResponseDto;
import com.hb.cda.elec_business.dto.LoginRequestDto;
import com.hb.cda.elec_business.dto.RegisterRequestDto;
import com.hb.cda.elec_business.dto.UserResponseDto;
import com.hb.cda.elec_business.entity.Role;
import com.hb.cda.elec_business.entity.RoleName;
import com.hb.cda.elec_business.entity.User;
import com.hb.cda.elec_business.entity.UserStatus;
import com.hb.cda.elec_business.mapper.UserMapper;
import com.hb.cda.elec_business.repository.RoleRepository;
import com.hb.cda.elec_business.repository.UserRepository;
import com.hb.cda.elec_business.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


import java.util.Collections;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl  implements AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    // private final JwtService jwtService;

    @Override
    public AuthResponseDto register(RegisterRequestDto request) {
        // 1. vérifs métier : email / phone uniques
        if(userRepository.existsByEmail(request.getEmail())){
            throw new IllegalArgumentException("Email already exists");
        }
        if (request.getPhone()!= null && userRepository.existsByPhone(request.getPhone())){
            throw new IllegalArgumentException("Phone number already exists");
        }
        // 2. Rôle par défaut : USER
        Role userRole = roleRepository.findByName(RoleName.USER)
                .orElseThrow(() -> new IllegalStateException("Default role USER not found"));

        // 3. Construction de l'entité User
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setUserStatus(UserStatus.ACTIVE);
        user.setFirstName(request.getUsername());
        user.setLastName(request.getLastName());
        user.setRoles(Collections.singleton(userRole));
        // 4.Persistance
        User savedUser = userRepository.save(user);
        // 5. Mapping vers DTO de réponse
        UserResponseDto userDto = UserMapper.toDto(savedUser);
        // 6. Construction de la réponse d'auth
        return AuthResponseDto.builder()
                .accessToken(null)
                .tokenType("Bearer")
                .user(userDto)
                .build();
    }

    @Override
    public AuthResponseDto login(LoginRequestDto request) {
        // 1. Charger le user
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));

        // 2. Vérifier le password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        // 3. Mapping user -> DTO
        UserResponseDto userDto = UserMapper.toDto(user);

        // 4. Réponse (sans JWT pour l’instant)
        return AuthResponseDto.builder()
                .accessToken(null)
                .tokenType("Bearer")
                .user(userDto)
                .build();
    }
}
