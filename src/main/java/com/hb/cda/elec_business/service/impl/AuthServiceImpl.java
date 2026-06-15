package com.hb.cda.elec_business.service.impl;

import com.hb.cda.elec_business.dto.auth.AuthResponseDto;
import com.hb.cda.elec_business.dto.auth.LoginRequestDto;
import com.hb.cda.elec_business.dto.auth.RegisterRequestDto;
import com.hb.cda.elec_business.dto.auth.UserResponseDto;
import com.hb.cda.elec_business.entity.Role;
import com.hb.cda.elec_business.entity.RoleName;
import com.hb.cda.elec_business.entity.User;
import com.hb.cda.elec_business.entity.UserStatus;
import com.hb.cda.elec_business.mapper.UserMapper;
import com.hb.cda.elec_business.repository.RoleRepository;
import com.hb.cda.elec_business.repository.UserRepository;
import com.hb.cda.elec_business.security.JwtService;
import com.hb.cda.elec_business.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserValidationServiceImpl validationService;

    @Override
    @Transactional
    public AuthResponseDto register(RegisterRequestDto request) {


        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Email already exists: {}", request.getEmail());
            throw new IllegalArgumentException("Email already exists");
        }

        if (request.getPhone() != null && userRepository.existsByPhone(request.getPhone())) {
            log.warn("Phone number already exists: {}", request.getPhone());
            throw new IllegalArgumentException("Phone number already exists");
        }


        Role userRole = roleRepository.findByName(RoleName.USER)
                .orElseThrow(() -> new IllegalStateException("USER role not found - RoleInitializer may have failed"));

        log.info("Creating new user entity...");
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setUserStatus(UserStatus.PENDING);
        user.setFirstName(request.getUsername());
        user.setLastName(request.getLastName());
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        user.setRoles(roles);

        User savedUser = userRepository.save(user);


        try {
            validationService.createAndSendValidation(savedUser);
        } catch (Exception e) {
            log.error("Failed to send validation email", e);

        }

        String accessToken = jwtService.generateAccessToken(savedUser);

        UserResponseDto userDto = UserMapper.toDto(savedUser);

        return AuthResponseDto.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .user(userDto)
                .build();
    }

    @Override
    public AuthResponseDto login(LoginRequestDto loginRequestDto) {


        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequestDto.getEmail(),
                            loginRequestDto.getPassword()
                    )
            );

            User user = (User) authentication.getPrincipal();
            log.info("User retrieved: {} (ID: {})", user.getEmail(), user.getId());

            if (user.getUserStatus() != UserStatus.ACTIVE) {
                log.warn("User status is not ACTIVE: {}", user.getUserStatus());
                throw new DisabledException("Account is not activated yet, please check your email.");
            }

            String accessToken = jwtService.generateAccessToken(user);

            UserResponseDto userDto = UserMapper.toDto(user);

            return AuthResponseDto.builder()
                    .accessToken(accessToken)
                    .tokenType("Bearer")
                    .user(userDto)
                    .build();
        } catch (DisabledException e) {
            log.warn("Login failed - Account disabled: {}", loginRequestDto.getEmail());
            throw new DisabledException("Account is not activated yet, please check your email to activate your account.");
        } catch (Exception e) {
            log.error("Login failed for: {}", loginRequestDto.getEmail(), e);
            throw e;
        }
    }
}