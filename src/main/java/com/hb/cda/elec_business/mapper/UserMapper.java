package com.hb.cda.elec_business.mapper;

import com.hb.cda.elec_business.dto.auth.UserResponseDto;
import com.hb.cda.elec_business.entity.User;

import java.util.stream.Collectors;

public class UserMapper {

    /**
     * Convertit un User en UserResponseDto
     */
    public static UserResponseDto toDto(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .username(user.getFirstName())
                .email(user.getEmail())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .userStatus(user.getUserStatus() != null ? user.getUserStatus().name() : null)
                .roles(
                        user.getRoles().stream()
                                .map(role -> role.getName().name())
                                .collect(Collectors.toSet())
                )
                .build();
    }
}
