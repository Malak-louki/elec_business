package com.hb.cda.elec_business.service;

public interface EmailService {

    void sendValidationEmail(String to, String username, String confirmationCode);

    void sendAccountActivatedEmail(String to, String username);
}