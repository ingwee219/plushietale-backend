package com.plushietale.backend.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AuthResponseDto {

    private String token;
    private String email;
    private String nickname;
    private String role;
}
