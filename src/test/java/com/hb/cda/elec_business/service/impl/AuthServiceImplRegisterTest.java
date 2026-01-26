package com.hb.cda.elec_business.service.impl;

import com.hb.cda.elec_business.dto.auth.AuthResponseDto;
import com.hb.cda.elec_business.dto.auth.RegisterRequestDto;
import com.hb.cda.elec_business.dto.auth.UserResponseDto;
import com.hb.cda.elec_business.entity.Role;
import com.hb.cda.elec_business.entity.RoleName;
import com.hb.cda.elec_business.entity.User;
import com.hb.cda.elec_business.entity.UserStatus;
import com.hb.cda.elec_business.repository.RoleRepository;
import com.hb.cda.elec_business.repository.UserRepository;
import com.hb.cda.elec_business.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplRegisterTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserValidationServiceImpl validationService;

    @InjectMocks
    private AuthServiceImpl authServiceImpl;

    private RegisterRequestDto validRequest;
    private Role userRole;
    private String testUuid;

    @BeforeEach
    void setUp() {
        validRequest = new RegisterRequestDto();
        validRequest.setEmail("test@example.com");
        validRequest.setPhone("0600000000");
        validRequest.setPassword("test123456");
        validRequest.setUsername("test");
        validRequest.setLastName("TestLastName");

        userRole = new Role();
        userRole.setName(RoleName.USER);

        testUuid = UUID.randomUUID().toString();
    }

    @Test
    void register_shouldCreateUserWithPendingStatusAndSendEmail_whenDataIsValid() {
        // GIVEN
        when(userRepository.existsByEmail(validRequest.getEmail())).thenReturn(false);
        when(userRepository.existsByPhone(validRequest.getPhone())).thenReturn(false);
        when(roleRepository.findByName(RoleName.USER)).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode(validRequest.getPassword())).thenReturn("hashedPassword");

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(testUuid);
            return u;
        });

        when(jwtService.generateAccessToken(any(User.class))).thenReturn("jwt-token-123");

        // WHEN
        AuthResponseDto response = authServiceImpl.register(validRequest);

        // THEN
        assertNotNull(response, "La réponse ne doit pas être null");
        assertEquals("jwt-token-123", response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());

        UserResponseDto userDto = response.getUser();
        assertNotNull(userDto);
        assertEquals(testUuid, userDto.getId());
        assertEquals(validRequest.getEmail(), userDto.getEmail());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User capturedUser = userCaptor.getValue();

        assertEquals(UserStatus.PENDING, capturedUser.getUserStatus(),
                "Le statut doit être PENDING après l'inscription");
        assertEquals("hashedPassword", capturedUser.getPassword());

        verify(validationService).createAndSendValidation(capturedUser);
    }

    @Test
    void register_shouldThrowException_whenEmailAlreadyExists() {
        // GIVEN
        when(userRepository.existsByEmail(validRequest.getEmail())).thenReturn(true);

        // WHEN & THEN
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authServiceImpl.register(validRequest)
        );

        assertEquals("Email already exists", exception.getMessage());
        verify(userRepository, never()).save(any());
        verify(jwtService, never()).generateAccessToken(any());
        verify(validationService, never()).createAndSendValidation(any());
    }

    @Test
    void register_shouldThrowException_whenPhoneAlreadyExists() {
        // GIVEN
        when(userRepository.existsByEmail(validRequest.getEmail())).thenReturn(false);
        when(userRepository.existsByPhone(validRequest.getPhone())).thenReturn(true);

        // WHEN & THEN
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authServiceImpl.register(validRequest)
        );

        assertEquals("Phone number already exists", exception.getMessage());
        verify(userRepository, never()).save(any());
        verify(validationService, never()).createAndSendValidation(any());
    }

    @Test
    void register_shouldAutoCreateUserRole_whenRoleNotFound() {
        // GIVEN - Le rôle USER n'existe pas encore
        when(userRepository.existsByEmail(validRequest.getEmail())).thenReturn(false);
        when(userRepository.existsByPhone(validRequest.getPhone())).thenReturn(false);
        when(roleRepository.findByName(RoleName.USER)).thenReturn(Optional.empty());

        // Mock de la création automatique du rôle
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> {
            Role role = invocation.getArgument(0);
            role.setId(UUID.randomUUID().toString());
            return role;
        });

        when(passwordEncoder.encode(validRequest.getPassword())).thenReturn("hashedPassword");

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(testUuid);
            return u;
        });

        when(jwtService.generateAccessToken(any(User.class))).thenReturn("jwt-token-123");

        // WHEN
        AuthResponseDto response = authServiceImpl.register(validRequest);

        // THEN - Le rôle doit avoir été créé automatiquement
        assertNotNull(response, "La réponse ne doit pas être null");
        assertEquals("jwt-token-123", response.getAccessToken());

        // Vérifier que le rôle a été créé
        ArgumentCaptor<Role> roleCaptor = ArgumentCaptor.forClass(Role.class);
        verify(roleRepository).save(roleCaptor.capture());
        Role capturedRole = roleCaptor.getValue();
        assertEquals(RoleName.USER, capturedRole.getName(),
                "Le rôle USER doit être créé automatiquement s'il n'existe pas");

        // Vérifier que l'utilisateur a bien été créé
        verify(userRepository).save(any(User.class));
        verify(validationService).createAndSendValidation(any(User.class));
    }

    @Test
    void register_shouldHandleNullPhone_whenPhoneIsNotProvided() {
        // GIVEN
        validRequest.setPhone(null);

        when(userRepository.existsByEmail(validRequest.getEmail())).thenReturn(false);
        when(roleRepository.findByName(RoleName.USER)).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode(validRequest.getPassword())).thenReturn("hashedPassword");

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(testUuid);
            return u;
        });

        when(jwtService.generateAccessToken(any(User.class))).thenReturn("jwt-token-123");

        // WHEN
        AuthResponseDto response = authServiceImpl.register(validRequest);

        // THEN
        assertNotNull(response);
        assertNull(response.getUser().getPhone());

        verify(userRepository, never()).existsByPhone(any());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertNull(userCaptor.getValue().getPhone());

        verify(validationService).createAndSendValidation(any(User.class));
    }

    @Test
    void register_shouldCreateUserWithCorrectAttributes() {
        // GIVEN
        when(userRepository.existsByEmail(validRequest.getEmail())).thenReturn(false);
        when(userRepository.existsByPhone(validRequest.getPhone())).thenReturn(false);
        when(roleRepository.findByName(RoleName.USER)).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode(validRequest.getPassword())).thenReturn("hashedPassword");

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(testUuid);
            return u;
        });

        when(jwtService.generateAccessToken(any(User.class))).thenReturn("jwt-token-123");

        // WHEN
        authServiceImpl.register(validRequest);

        // THEN
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User capturedUser = userCaptor.getValue();

        assertEquals(validRequest.getEmail(), capturedUser.getEmail());
        assertEquals(validRequest.getPhone(), capturedUser.getPhone());
        assertEquals(validRequest.getUsername(), capturedUser.getFirstName());
        assertEquals(validRequest.getLastName(), capturedUser.getLastName());
        assertEquals(UserStatus.PENDING, capturedUser.getUserStatus());
        assertTrue(capturedUser.getRoles().contains(userRole));
    }
}