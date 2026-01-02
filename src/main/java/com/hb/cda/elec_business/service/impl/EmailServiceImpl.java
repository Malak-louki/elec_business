package com.hb.cda.elec_business.service.impl;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.base-url}")
    private String baseUrl;

    /**
     * Envoie un email de validation de manière asynchrone
     * @param to Email du destinataire
     * @param username Nom de l'utilisateur
     * @param confirmationCode Code de confirmation unique
     */
    @Async
    public void sendValidationEmail(String to, String username, String confirmationCode) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Activez votre compte ElecBusiness");

            String validationLink = baseUrl + "/api/auth/validate?code=" + confirmationCode;

            String htmlContent = buildEmailContent(username, validationLink);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email de validation envoyé avec succès à {}", to);

        } catch (MessagingException e) {
            log.error("Erreur lors de l'envoi de l'email de validation à {}: {}", to, e.getMessage());
            throw new RuntimeException("Échec de l'envoi de l'email de validation", e);
        }
    }

    /**
     * Construit le contenu HTML de l'email
     */
    private String buildEmailContent(String username, String validationLink) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #4CAF50; color: white; padding: 20px; text-align: center; }
                    .content { background-color: #f9f9f9; padding: 30px; border-radius: 5px; }
                    .button { display: inline-block; padding: 12px 30px; background-color: #4CAF50; 
                              color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                    .footer { text-align: center; margin-top: 20px; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Bienvenue sur ElecBusiness !</h1>
                    </div>
                    <div class="content">
                        <h2>Bonjour %s,</h2>
                        <p>Merci de vous être inscrit sur notre plateforme de recharge électrique.</p>
                        <p>Pour activer votre compte, veuillez cliquer sur le bouton ci-dessous :</p>
                        <div style="text-align: center;">
                            <a href="%s" class="button">Activer mon compte</a>
                        </div>
                        <p>Ou copiez-collez ce lien dans votre navigateur :</p>
                        <p style="word-break: break-all; color: #4CAF50;">%s</p>
                        <p><strong>Ce lien expire dans 24 heures.</strong></p>
                        <p>Si vous n'êtes pas à l'origine de cette inscription, ignorez cet email.</p>
                    </div>
                    <div class="footer">
                        <p>© 2024 ElecBusiness - Tous droits réservés</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(username, validationLink, validationLink);
    }

    /**
     * Envoie un email de confirmation après validation
     */
    @Async
    public void sendAccountActivatedEmail(String to, String username) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Votre compte ElecBusiness est activé !");

            String htmlContent = """
                <!DOCTYPE html>
                <html>
                <body style="font-family: Arial, sans-serif;">
                    <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                        <h2 style="color: #4CAF50;">Compte activé avec succès !</h2>
                        <p>Bonjour %s,</p>
                        <p>Votre compte ElecBusiness est maintenant actif.</p>
                        <p>Vous pouvez dès maintenant vous connecter et profiter de nos services.</p>
                        <p>À bientôt sur notre plateforme !</p>
                    </div>
                </body>
                </html>
                """.formatted(username);

            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Email de confirmation d'activation envoyé à {}", to);

        } catch (MessagingException e) {
            log.error("Erreur lors de l'envoi de l'email de confirmation à {}: {}", to, e.getMessage());
        }
    }
}