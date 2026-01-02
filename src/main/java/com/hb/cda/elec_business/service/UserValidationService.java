package com.hb.cda.elec_business.service;

import com.hb.cda.elec_business.entity.User;

public interface UserValidationService {

    void createAndSendValidation(User user);

    boolean validateUserAccount(String confirmationCode);

    boolean resendValidationEmail(String email);
}
