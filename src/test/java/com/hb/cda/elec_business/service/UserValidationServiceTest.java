package com.hb.cda.elec_business.service;

import com.hb.cda.elec_business.entity.User;
import com.hb.cda.elec_business.entity.UserStatus;
import com.hb.cda.elec_business.entity.UserValidation;
import com.hb.cda.elec_business.repository.UserRepository;
import com.hb.cda.elec_business.repository.UserValidationRepository;
import com.hb.cda.elec_business.service.impl.EmailServiceImpl;
import com.hb.cda.elec_business.service.impl.UserValidationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserValidationServiceTest {

    @Mock
    private UserValidationRepository validationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailServiceImpl emailService;

    @InjectMocks
    private UserValidationServiceImpl validationService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(UUID.randomUUID().toString());
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setUserStatus(UserStatus.PENDING);

        // Configure l'expiration à 24h (en millisecondes)
        ReflectionTestUtils.setField(validationService, "tokenExpirationMs", 86400000L);
    }

    @Test
    void createAndSendValidation_shouldCreateValidationAndSendEmail() {
        // GIVEN
        when(validationRepository.save(any(UserValidation.class))).thenAnswer(i -> i.getArgument(0));

        // WHEN
        validationService.createAndSendValidation(testUser);

        // THEN
        ArgumentCaptor<UserValidation> validationCaptor = ArgumentCaptor.forClass(UserValidation.class);
        verify(validationRepository).save(validationCaptor.capture());

        UserValidation savedValidation = validationCaptor.getValue();
        assertNotNull(savedValidation.getConfirmationCode(), "Le code de confirmation doit être généré");
        assertEquals(testUser, savedValidation.getUser(), "L'utilisateur doit être associé");
        assertNull(savedValidation.getValidatedAt(), "La date de validation doit être null");

        // Vérifie que l'email a été envoyé
        verify(emailService).sendValidationEmail(
                eq(testUser.getEmail()),
                eq(testUser.getFirstName()),
                eq(savedValidation.getConfirmationCode())
        );
    }

    @Test
    void validateUserAccount_shouldActivateAccount_whenCodeIsValid() {
        // GIVEN
        String confirmationCode = UUID.randomUUID().toString();
        UserValidation validation = new UserValidation();
        validation.setConfirmationCode(confirmationCode);
        validation.setUser(testUser);
        ReflectionTestUtils.setField(
                validation,
                "createdAt",
                Instant.now().minus(1, ChronoUnit.HOURS)
        );

        when(validationRepository.findByConfirmationCode(confirmationCode))
                .thenReturn(Optional.of(validation));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(validationRepository.save(any(UserValidation.class))).thenAnswer(i -> i.getArgument(0));

        // WHEN
        boolean result = validationService.validateUserAccount(confirmationCode);

        // THEN
        assertTrue(result, "La validation doit réussir");
        assertEquals(UserStatus.ACTIVE, testUser.getUserStatus(), "Le statut doit être ACTIVE");
        assertNotNull(validation.getValidatedAt(), "La date de validation doit être définie");

        verify(userRepository).save(testUser);
        verify(validationRepository).save(validation);
        verify(emailService).sendAccountActivatedEmail(testUser.getEmail(), testUser.getFirstName());
    }

    @Test
    void validateUserAccount_shouldReturnFalse_whenCodeDoesNotExist() {
        // GIVEN
        String invalidCode = "invalid-code";
        when(validationRepository.findByConfirmationCode(invalidCode))
                .thenReturn(Optional.empty());

        // WHEN
        boolean result = validationService.validateUserAccount(invalidCode);

        // THEN
        assertFalse(result, "La validation doit échouer pour un code invalide");
        verify(userRepository, never()).save(any());
        verify(emailService, never()).sendAccountActivatedEmail(any(), any());
    }

    @Test
    void validateUserAccount_shouldReturnFalse_whenCodeAlreadyUsed() {
        // GIVEN
        String confirmationCode = UUID.randomUUID().toString();
        UserValidation validation = new UserValidation();
        validation.setConfirmationCode(confirmationCode);
        validation.setUser(testUser);
        validation.setValidatedAt(Instant.now().minus(1, ChronoUnit.DAYS)); // Déjà validé

        when(validationRepository.findByConfirmationCode(confirmationCode))
                .thenReturn(Optional.of(validation));

        // WHEN
        boolean result = validationService.validateUserAccount(confirmationCode);

        // THEN
        assertFalse(result, "La validation doit échouer si le code est déjà utilisé");
        verify(userRepository, never()).save(any());
    }

    @Test
    void validateUserAccount_shouldReturnFalse_whenCodeIsExpired() {
        // GIVEN
        String confirmationCode = UUID.randomUUID().toString();
        UserValidation validation = new UserValidation();
        validation.setConfirmationCode(confirmationCode);
        validation.setUser(testUser);
        // Simulation : créé il y a 2 jours → forcément > 24h
        ReflectionTestUtils.setField(
                validation,
                "createdAt",
                Instant.now().minus(2, ChronoUnit.DAYS)
        );

        when(validationRepository.findByConfirmationCode(confirmationCode))
                .thenReturn(Optional.of(validation));

        // WHEN
        boolean result = validationService.validateUserAccount(confirmationCode);

        // THEN
        assertFalse(result, "La validation doit échouer si le code est expiré");
        verify(userRepository, never()).save(any());
    }

    @Test
    void resendValidationEmail_shouldSendNewEmail_whenUserExists() {
        // GIVEN
        when(userRepository.findByEmail(testUser.getEmail()))
                .thenReturn(Optional.of(testUser));
        when(validationRepository.save(any(UserValidation.class)))
                .thenAnswer(i -> i.getArgument(0));

        // WHEN
        boolean result = validationService.resendValidationEmail(testUser.getEmail());

        // THEN
        assertTrue(result, "Le renvoi d'email doit réussir");
        verify(validationRepository).deleteByUserAndValidatedAtIsNull(testUser);
        verify(validationRepository).save(any(UserValidation.class));
        verify(emailService).sendValidationEmail(any(), any(), any());
    }

    @Test
    void resendValidationEmail_shouldReturnFalse_whenUserDoesNotExist() {
        // GIVEN
        String email = "nonexistent@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // WHEN
        boolean result = validationService.resendValidationEmail(email);

        // THEN
        assertFalse(result, "Le renvoi doit échouer si l'utilisateur n'existe pas");
        verify(emailService, never()).sendValidationEmail(any(), any(), any());
    }

    @Test
    void resendValidationEmail_shouldReturnFalse_whenAccountAlreadyActive() {
        // GIVEN
        testUser.setUserStatus(UserStatus.ACTIVE);
        when(userRepository.findByEmail(testUser.getEmail()))
                .thenReturn(Optional.of(testUser));

        // WHEN
        boolean result = validationService.resendValidationEmail(testUser.getEmail());

        // THEN
        assertFalse(result, "Le renvoi doit échouer si le compte est déjà actif");
        verify(emailService, never()).sendValidationEmail(any(), any(), any());
    }
}