package com.plushietale.backend.toy.dto;

import com.plushietale.backend.toy.Toy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class ToyResponseDto {

    private Long id;
    private String name;
    private String imageUrl;
    private String personality;
    private String description;
    private LocalDateTime createdAt;

    public static ToyResponseDto from(Toy toy) {
        return ToyResponseDto.builder()
                .id(toy.getId())
                .name(toy.getName())
                .imageUrl(toy.getImageUrl())
                .personality(toy.getPersonality())
                .description(toy.getDescription())
                .createdAt(toy.getCreatedAt())
                .build();
    }
}
