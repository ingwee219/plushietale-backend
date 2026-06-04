package com.plushietale.backend.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PostRequestDto {

    @NotNull(message = "Story ID is required.")
    private Long storyId;

    @NotBlank(message = "Title is required.")
    @Size(max = 200, message = "Title must be 200 characters or less.")
    private String title;

    private String content;
}
