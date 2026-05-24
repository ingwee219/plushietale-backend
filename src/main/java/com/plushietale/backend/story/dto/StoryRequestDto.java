package com.plushietale.backend.story.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class StoryRequestDto {

    @NotEmpty(message = "At least one toy is required.")
    @Size(max = 10, message = "You can include up to 10 toys in one story.")
    private List<Long> toyIds;

    @NotNull(message = "Target age is required.")
    @Min(value = 3, message = "Target age must be between 3 and 11.")
    @Max(value = 11, message = "Target age must be between 3 and 11.")
    private Integer targetAge;

    private String prompt; // optional — user-provided story idea
}
