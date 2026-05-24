package com.plushietale.backend.user;

import com.plushietale.backend.comment.dto.MyCommentResponseDto;
import com.plushietale.backend.global.response.ApiResponse;
import com.plushietale.backend.post.dto.PostResponseDto;
import com.plushietale.backend.user.dto.UpdateNicknameRequestDto;
import com.plushietale.backend.user.dto.UpdatePasswordRequestDto;
import com.plushietale.backend.user.dto.UserResponseDto;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User", description = "User API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "Get my profile")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponseDto>> getMyInfo(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getMyInfo(user)));
    }

    @Operation(summary = "Get my posts")
    @GetMapping("/me/posts")
    public ResponseEntity<ApiResponse<List<PostResponseDto>>> getMyPosts(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getMyPosts(user.getId())));
    }

    @Operation(summary = "Get my comments")
    @GetMapping("/me/comments")
    public ResponseEntity<ApiResponse<List<MyCommentResponseDto>>> getMyComments(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getMyComments(user.getId())));
    }

    @Operation(summary = "Update my nickname")
    @PatchMapping("/me/nickname")
    public ResponseEntity<ApiResponse<UserResponseDto>> updateNickname(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateNicknameRequestDto request) {
        return ResponseEntity.ok(ApiResponse.ok("Nickname updated.", userService.updateNickname(user.getId(), request)));
    }

    @Operation(summary = "Update my password")
    @PatchMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> updatePassword(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdatePasswordRequestDto request) {
        userService.updatePassword(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Password updated.", null));
    }
}
