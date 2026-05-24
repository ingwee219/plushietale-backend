package com.plushietale.backend.toy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ToyRequestDto {

    @NotBlank(message = "Toy name is required.")
    @Size(max = 50, message = "Toy name must be 50 characters or less.")
    private String name;

    private String imageUrl;    // Replaced with S3 upload in Step 6
    private String personality;
    private String description;
}
