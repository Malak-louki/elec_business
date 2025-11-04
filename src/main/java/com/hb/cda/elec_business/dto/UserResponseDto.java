package com.hb.cda.elec_business.dto;

import java.util.Set;

public class UserResponseDto {

    private String id;           // UUID string
    private String username;
    private String email;
    private String phone;
    private Set<String> roles;
}
