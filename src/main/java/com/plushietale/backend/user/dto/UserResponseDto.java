package com.plushietale.backend.user.dto;

import com.plushietale.backend.user.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class UserResponseDto {

    private Long id;
    private String email;
    private String nickname;
    private String profileImageUrl;
    private String provider;
    private String role;

    public static UserResponseDto from(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .provider(user.getProvider().name())
                .role(user.getRole().name())
                .build();
    }
}
