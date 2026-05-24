package com.plushietale.backend.post;

import com.plushietale.backend.global.response.ApiResponse;
import com.plushietale.backend.post.dto.LikeResponseDto;
import com.plushietale.backend.post.dto.PostRequestDto;
import com.plushietale.backend.post.dto.PostResponseDto;
import com.plushietale.backend.post.dto.PostUpdateRequestDto;
import com.plushietale.backend.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Post", description = "Community Board API")
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @Operation(summary = "Share a story to the board")
    @PostMapping
    public ResponseEntity<ApiResponse<PostResponseDto>> createPost(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody PostRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Post created.", postService.createPost(user.getId(), request)));
    }

    @Operation(summary = "Get all posts (paginated)")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<PostResponseDto>>> getPosts(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal User user) {
        Long userId = user != null ? user.getId() : null;
        return ResponseEntity.ok(ApiResponse.ok(postService.getPosts(pageable, userId)));
    }

    @Operation(summary = "Get a post by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PostResponseDto>> getPost(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        Long userId = user != null ? user.getId() : null;
        return ResponseEntity.ok(ApiResponse.ok(postService.getPost(id, userId)));
    }

    @Operation(summary = "Toggle like on a post")
    @PostMapping("/{id}/likes")
    public ResponseEntity<ApiResponse<LikeResponseDto>> toggleLike(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(postService.toggleLike(user.getId(), id)));
    }

    @Operation(summary = "Update a post")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PostResponseDto>> updatePost(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @Valid @RequestBody PostUpdateRequestDto request) {
        return ResponseEntity.ok(ApiResponse.ok("Post updated.", postService.updatePost(user.getId(), id, request)));
    }

    @Operation(summary = "Delete a post")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        postService.deletePost(user.getId(), id);
        return ResponseEntity.ok(ApiResponse.ok("Post deleted.", null));
    }
}
