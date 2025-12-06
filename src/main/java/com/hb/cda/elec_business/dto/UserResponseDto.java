package com.hb.cda.elec_business.dto;

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
    private String email;
    private String phone;
    private Set<String> roles;
}
