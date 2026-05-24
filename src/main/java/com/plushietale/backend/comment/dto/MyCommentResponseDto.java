package com.plushietale.backend.comment.dto;

import com.plushietale.backend.comment.Comment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class MyCommentResponseDto {

    private Long id;
    private String content;
    private Long postId;
    private String postTitle;
    private LocalDateTime createdAt;

    public static MyCommentResponseDto from(Comment comment) {
        return MyCommentResponseDto.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .postId(comment.getPost().getId())
                .postTitle(comment.getPost().getTitle())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
