package com.hb.cda.elec_business.service.impl;

import com.hb.cda.elec_business.dto.AuthResponseDto;
import com.hb.cda.elec_business.dto.LoginRequestDto;
import com.hb.cda.elec_business.dto.UserResponseDto;
import com.hb.cda.elec_business.entity.Role;
import com.hb.cda.elec_business.entity.RoleName;
import com.hb.cda.elec_business.entity.User;
import com.hb.cda.elec_business.entity.UserStatus;
import com.hb.cda.elec_business.repository.RoleRepository;
import com.hb.cda.elec_business.repository.UserRepository;
import com.hb.cda.elec_business.security.JwtService;
import com.hb.cda.elec_business.service.UserValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplLoginTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private Authentication authentication;

    @Mock
    private UserValidationService validationService;

    @InjectMocks
    private AuthServiceImpl authServiceImpl;

    private LoginRequestDto validLoginRequest;
    private User authenticatedUser;
    private Role userRole;
    private String testUuid;

    @BeforeEach
    void setUp() {
        validLoginRequest = new LoginRequestDto();
        validLoginRequest.setEmail("test@example.com");
        validLoginRequest.setPassword("test123456");

        testUuid = UUID.randomUUID().toString();

        userRole = new Role();
        userRole.setId(UUID.randomUUID().toString());
        userRole.setName(RoleName.USER);

        authenticatedUser = new User();
        authenticatedUser.setId(testUuid);
        authenticatedUser.setEmail("test@example.com");
        authenticatedUser.setPassword("test123456");
        authenticatedUser.setPhone("0600000000");
        authenticatedUser.setFirstName("Test");
        authenticatedUser.setLastName("TestLastname");
        authenticatedUser.setUserStatus(UserStatus.ACTIVE);
        authenticatedUser.setRoles(Collections.singleton(userRole));
    }

    @Test
    void login_shouldAuthenticateAndReturnToken_whenCredentialsAreValidAndAccountActive(){
        // GIVEN
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(authenticatedUser);
        when(jwtService.generateAccessToken(authenticatedUser)).thenReturn("jwt-login-token-456");

        // WHEN
        AuthResponseDto response = authServiceImpl.login(validLoginRequest);

        // THEN
        assertNotNull(response);
        assertEquals("jwt-login-token-456", response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());

        UserResponseDto userDto = response.getUser();
        assertNotNull(userDto);
        assertEquals(testUuid, userDto.getId());
        assertEquals(authenticatedUser.getEmail(), userDto.getEmail());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void login_shouldThrowBadCredentialsException_whenPasswordIsIncorrect(){
        // GIVEN
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        // WHEN & THEN
        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> authServiceImpl.login(validLoginRequest)
        );

        assertEquals("Invalid credentials", exception.getMessage());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService, never()).generateAccessToken(any());
    }

    @Test
    void login_shouldThrowBadCredentialsException_whenEmailDoesNotExist(){
        // GIVEN
        LoginRequestDto invalidEmailRequest = new LoginRequestDto();
        invalidEmailRequest.setEmail("nonexistent@example.com");
        invalidEmailRequest.setPassword("test123456");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("User not found"));

        // WHEN & THEN
        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> authServiceImpl.login(invalidEmailRequest)
        );

        assertEquals("User not found", exception.getMessage());
        verify(jwtService, never()).generateAccessToken(any());
    }

    @Test
    void login_shouldHandleUserWithMultipleRoles() {
        // GIVEN
        Role adminRole = new Role();
        adminRole.setId(UUID.randomUUID().toString());
        adminRole.setName(RoleName.ADMIN);

        User multiRoleUser = new User();
        multiRoleUser.setId(testUuid);
        multiRoleUser.setEmail("admin@example.com");
        multiRoleUser.setFirstName("Admin");
        multiRoleUser.setLastName("User");
        multiRoleUser.setPhone("0600000001");
        multiRoleUser.setUserStatus(UserStatus.ACTIVE);
        multiRoleUser.setRoles(Set.of(userRole, adminRole));

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(multiRoleUser);
        when(jwtService.generateAccessToken(multiRoleUser)).thenReturn("jwt-admin-token");

        // WHEN
        AuthResponseDto response = authServiceImpl.login(validLoginRequest);

        // THEN
        assertNotNull(response);
        assertEquals("jwt-admin-token", response.getAccessToken());

        UserResponseDto userDto = response.getUser();
        assertEquals(2, userDto.getRoles().size());
        assertTrue(userDto.getRoles().contains("USER"));
        assertTrue(userDto.getRoles().contains("ADMIN"));
    }

    @Test
    void login_shouldGenerateUniqueTokenForEachLogin(){
        // GIVEN
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(authenticatedUser);
        when(jwtService.generateAccessToken(authenticatedUser))
                .thenReturn("token-1")
                .thenReturn("token-2");

        // WHEN
        AuthResponseDto response1 = authServiceImpl.login(validLoginRequest);
        AuthResponseDto response2 = authServiceImpl.login(validLoginRequest);

        // THEN
        assertEquals("token-1", response1.getAccessToken());
        assertEquals("token-2", response2.getAccessToken());
        assertNotEquals(response1.getAccessToken(), response2.getAccessToken());

        verify(authenticationManager, times(2)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService, times(2)).generateAccessToken(authenticatedUser);
    }

    @Test
    void login_shouldNotEncodePassword_duringAuthentication(){
        // GIVEN
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(authenticatedUser);
        when(jwtService.generateAccessToken(authenticatedUser)).thenReturn("jwt-token");

        // WHEN
        authServiceImpl.login(validLoginRequest);

        // THEN
        verify(passwordEncoder, never()).encode(anyString());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }


    @Test
    void login_shouldThrowDisabledException_whenAccountIsPending(){
        // GIVEN - Utilisateur avec statut PENDING
        authenticatedUser.setUserStatus(UserStatus.PENDING);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(authenticatedUser);

        // WHEN & THEN
        DisabledException exception = assertThrows(
                DisabledException.class,
                () -> authServiceImpl.login(validLoginRequest),
                "Une exception doit être levée si le compte n'est pas activé"
        );

        assertTrue(exception.getMessage().contains("not activated"),
                "Le message doit indiquer que le compte n'est pas activé");

        verify(jwtService, never()).generateAccessToken(any());
    }

    @Test
    void login_shouldThrowDisabledException_whenAccountIsSuspended(){
        // GIVEN - Utilisateur suspendu
        authenticatedUser.setUserStatus(UserStatus.SUSPENDED);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(authenticatedUser);

        // WHEN & THEN
        DisabledException exception = assertThrows(
                DisabledException.class,
                () -> authServiceImpl.login(validLoginRequest)
        );

        assertNotNull(exception.getMessage());
        verify(jwtService, never()).generateAccessToken(any());
    }

    @Test
    void login_shouldThrowDisabledException_whenAccountIsBanned(){
        // GIVEN - Utilisateur banni
        authenticatedUser.setUserStatus(UserStatus.BANNED);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(authenticatedUser);

        // WHEN & THEN
        DisabledException exception = assertThrows(
                DisabledException.class,
                () -> authServiceImpl.login(validLoginRequest)
        );

        assertNotNull(exception.getMessage());
        verify(jwtService, never()).generateAccessToken(any());
    }

    @Test
    void login_shouldSucceed_onlyWhenAccountIsActive(){
        // GIVEN - Compte actif
        authenticatedUser.setUserStatus(UserStatus.ACTIVE);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(authenticatedUser);
        when(jwtService.generateAccessToken(authenticatedUser)).thenReturn("jwt-token");

        // WHEN
        AuthResponseDto response = authServiceImpl.login(validLoginRequest);

        // THEN
        assertNotNull(response);
        assertEquals("jwt-token", response.getAccessToken());
        verify(jwtService).generateAccessToken(authenticatedUser);
    }
}