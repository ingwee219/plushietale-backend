package com.plushietale.backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateNicknameRequestDto {

    @NotBlank(message = "Nickname is required.")
    @Size(max = 50, message = "Nickname must be 50 characters or less.")
    private String nickname;
}
