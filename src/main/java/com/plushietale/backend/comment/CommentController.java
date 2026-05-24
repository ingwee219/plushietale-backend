package com.plushietale.backend.comment;

import com.plushietale.backend.comment.dto.CommentRequestDto;
import com.plushietale.backend.comment.dto.CommentResponseDto;
import com.plushietale.backend.global.response.ApiResponse;
import com.plushietale.backend.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Comment", description = "Comment API")
@RestController
@RequestMapping("/api/posts/{postId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "Write a comment")
    @PostMapping
    public ResponseEntity<ApiResponse<CommentResponseDto>> createComment(
            @AuthenticationPrincipal User user,
            @PathVariable Long postId,
            @Valid @RequestBody CommentRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Comment posted.", commentService.createComment(user.getId(), postId, request)));
    }

    @Operation(summary = "Get comments for a post")
    @GetMapping
    public ResponseEntity<ApiResponse<List<CommentResponseDto>>> getComments(@PathVariable Long postId) {
        return ResponseEntity.ok(ApiResponse.ok(commentService.getComments(postId)));
    }

    @Operation(summary = "Update a comment")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CommentResponseDto>> updateComment(
            @AuthenticationPrincipal User user,
            @PathVariable Long postId,
            @PathVariable Long id,
            @Valid @RequestBody CommentRequestDto request) {
        return ResponseEntity.ok(ApiResponse.ok("Comment updated.", commentService.updateComment(user.getId(), id, request)));
    }

    @Operation(summary = "Delete a comment")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @AuthenticationPrincipal User user,
            @PathVariable Long postId,
            @PathVariable Long id) {
        commentService.deleteComment(user.getId(), id);
        return ResponseEntity.ok(ApiResponse.ok("Comment deleted.", null));
    }
}
