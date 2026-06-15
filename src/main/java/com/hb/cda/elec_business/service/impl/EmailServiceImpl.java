package com.hb.cda.elec_business.service.impl;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailServiceImpl {

    @Value("${brevo.api.key:fake-key-for-tests}")
    private String brevoApiKey;

    @Value("${spring.mail.username:noreply@elecbusiness.com}")
    private String fromEmail;

    @Value("${app.base-url}")
    private String baseUrl;

    private final OkHttpClient httpClient = new OkHttpClient();

    @Async
    public void sendValidationEmail(String to, String username, String confirmationCode) {
        try {
            String validationLink = baseUrl + "/api/auth/validate?code=" + confirmationCode;
            String htmlContent = buildEmailContent(username, validationLink);

            String jsonBody = String.format("""
                {
                  "sender": {"email": "%s", "name": "ElecBusiness"},
                  "to": [{"email": "%s", "name": "%s"}],
                  "subject": "Activez votre compte ElecBusiness",
                  "htmlContent": %s
                }
                """,
                    fromEmail,
                    to,
                    username,
                    escapeJson(htmlContent)
            );

            Request request = new Request.Builder()
                    .url("https://api.brevo.com/v3/smtp/email")
                    .addHeader("api-key", brevoApiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    log.info("✅ Email de validation envoyé avec succès à {}", to);
                } else {
                    log.error("❌ Erreur Brevo API: {} - {}", response.code(), response.body().string());
                    throw new RuntimeException("Échec envoi email: " + response.code());
                }
            }

        } catch (Exception e) {
            log.error("❌ Erreur lors de l'envoi de l'email: {}", e.getMessage());
            throw new RuntimeException("Échec de l'envoi de l'email de validation", e);
        }
    }

    @Async
    public void sendAccountActivatedEmail(String to, String username) {
        try {
            String htmlContent = String.format("""
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
                """, username);

            String jsonBody = String.format("""
                {
                  "sender": {"email": "%s", "name": "ElecBusiness"},
                  "to": [{"email": "%s", "name": "%s"}],
                  "subject": "Votre compte ElecBusiness est activé !",
                  "htmlContent": %s
                }
                """,
                    fromEmail,
                    to,
                    username,
                    escapeJson(htmlContent)
            );

            Request request = new Request.Builder()
                    .url("https://api.brevo.com/v3/smtp/email")
                    .addHeader("api-key", brevoApiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    log.info("✅ Email de confirmation envoyé à {}", to);
                }
            }

        } catch (Exception e) {
            log.error("❌ Erreur confirmation email: {}", e.getMessage());
        }
    }

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

    private String escapeJson(String html) {
        return "\"" + html
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t") + "\"";
    }
}