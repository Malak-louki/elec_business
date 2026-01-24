package com.hb.cda.elec_business.dto.auth;

import com.hb.cda.elec_business.entity.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDto {

    private String id;           // UUID string
    private String username;
    private String lastName;
    private String email;
    private String phone;
    private Set<String> roles;
    private String userStatus;
}

