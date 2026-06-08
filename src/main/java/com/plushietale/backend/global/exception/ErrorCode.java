package com.plushietale.backend.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Auth
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "This email is already in use."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User not found."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "Invalid password."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "Invalid token."),

    // Toy
    TOY_NOT_FOUND(HttpStatus.NOT_FOUND, "Toy not found."),
    TOY_ACCESS_DENIED(HttpStatus.FORBIDDEN, "Access denied to this toy."),

    // Story
    STORY_NOT_FOUND(HttpStatus.NOT_FOUND, "Story not found."),
    STORY_ACCESS_DENIED(HttpStatus.FORBIDDEN, "Access denied to this story."),

    // Post
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "Post not found."),
    POST_ACCESS_DENIED(HttpStatus.FORBIDDEN, "Access denied to this post."),
    STORY_ALREADY_POSTED(HttpStatus.CONFLICT, "This story has already been posted."),

    // Comment
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Comment not found."),
    COMMENT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "Access denied to this comment."),

    // S3
    S3_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload file."),

    // Moderation
    INAPPROPRIATE_CONTENT(HttpStatus.BAD_REQUEST, "Your message contains inappropriate content. Please keep it family-friendly."),

    // Gemini
    GEMINI_API_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate story. Please try again."),
    GEMINI_CONTENT_BLOCKED(HttpStatus.BAD_REQUEST, "The story could not be generated because the content was flagged as inappropriate. Please try a different story idea.");

    private final HttpStatus httpStatus;
    private final String message;
}
