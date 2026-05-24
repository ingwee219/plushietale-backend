package com.plushietale.backend.story;

import com.plushietale.backend.global.response.ApiResponse;
import com.plushietale.backend.story.dto.StoryRequestDto;
import com.plushietale.backend.story.dto.StoryResponseDto;
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

@Tag(name = "Story", description = "Story API")
@RestController
@RequestMapping("/api/stories")
@RequiredArgsConstructor
public class StoryController {

    private final StoryService storyService;

    @Operation(summary = "Generate a story")
    @PostMapping
    public ResponseEntity<ApiResponse<StoryResponseDto>> createStory(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody StoryRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Story created.", storyService.createStory(user.getId(), request)));
    }

    @Operation(summary = "Get my stories")
    @GetMapping
    public ResponseEntity<ApiResponse<List<StoryResponseDto>>> getMyStories(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(storyService.getMyStories(user.getId())));
    }

    @Operation(summary = "Get a story by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StoryResponseDto>> getStory(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(storyService.getStory(user.getId(), id)));
    }

    @Operation(summary = "Delete a story")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteStory(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        storyService.deleteStory(user.getId(), id);
        return ResponseEntity.ok(ApiResponse.ok("Story deleted.", null));
    }
}
