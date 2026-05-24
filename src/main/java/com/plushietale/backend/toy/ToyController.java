package com.plushietale.backend.toy;

import com.plushietale.backend.global.response.ApiResponse;
import com.plushietale.backend.toy.dto.ToyResponseDto;
import com.plushietale.backend.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Toy", description = "Toy API")
@RestController
@RequestMapping("/api/toys")
@RequiredArgsConstructor
public class ToyController {

    private final ToyService toyService;

    @Operation(summary = "Register a toy (multipart/form-data)")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ToyResponseDto>> createToy(
            @AuthenticationPrincipal User user,
            @RequestParam("name") String name,
            @RequestParam(value = "personality", required = false) String personality,
            @RequestParam(value = "description", required = false) String description,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Toy registered.",
                        toyService.createToy(user.getId(), name, personality, description, image)));
    }

    @Operation(summary = "Get my toys")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ToyResponseDto>>> getMyToys(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(toyService.getMyToys(user.getId())));
    }

    @Operation(summary = "Update a toy")
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ToyResponseDto>> updateToy(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "personality", required = false) String personality,
            @RequestParam(value = "description", required = false) String description,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        return ResponseEntity.ok(ApiResponse.ok("Toy updated.",
                toyService.updateToy(user.getId(), id, name, personality, description, image)));
    }

    @Operation(summary = "Delete a toy")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteToy(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        toyService.deleteToy(user.getId(), id);
        return ResponseEntity.ok(ApiResponse.ok("Toy deleted.", null));
    }
}
