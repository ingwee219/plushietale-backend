package com.plushietale.backend.story.dto;

import com.plushietale.backend.story.Story;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class StoryResponseDto {

    private Long id;
    private String title;
    private String content;
    private Integer targetAge;
    private Boolean isShared;
    private List<String> toyNames;
    private LocalDateTime createdAt;

    public static StoryResponseDto from(Story story) {
        return StoryResponseDto.builder()
                .id(story.getId())
                .title(story.getTitle())
                .content(story.getContent())
                .targetAge(story.getTargetAge())
                .isShared(story.getIsShared())
                .toyNames(story.getStoryToys().stream()
                        .map(st -> st.getToy().getName())
                        .toList())
                .createdAt(story.getCreatedAt())
                .build();
    }
}
