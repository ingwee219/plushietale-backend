package com.plushietale.backend.post.dto;

import com.plushietale.backend.post.Post;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class PostResponseDto {

    private Long id;
    private String title;
    private String content;
    private int viewCount;
    private int likeCount;
    private boolean likedByMe;
    private String authorNickname;
    private Long storyId;
    private String storyContent;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PostResponseDto from(Post post, boolean likedByMe) {
        return PostResponseDto.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .viewCount(post.getViewCount())
                .likeCount(post.getLikeCount())
                .likedByMe(likedByMe)
                .authorNickname(post.getUser().getNickname())
                .storyId(post.getStory() != null ? post.getStory().getId() : null)
                .storyContent(post.getStory() != null ? post.getStory().getContent() : null)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    public static PostResponseDto from(Post post) {
        return from(post, false);
    }
}
