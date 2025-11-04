package com.hb.cda.elec_business.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class LoginRequestDto {
    @NotBlank
    @Email
    private String email;
    @NotBlank
    private String password;
}
