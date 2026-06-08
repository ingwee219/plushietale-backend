package com.plushietale.backend.toy;

import com.plushietale.backend.global.exception.CustomException;
import com.plushietale.backend.global.exception.ErrorCode;
import com.plushietale.backend.global.moderation.ContentModerationService;
import com.plushietale.backend.storage.S3Service;
import com.plushietale.backend.story.StoryToyRepository;
import com.plushietale.backend.toy.dto.ToyResponseDto;
import com.plushietale.backend.user.User;
import com.plushietale.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ToyService {

    private final ToyRepository toyRepository;
    private final UserRepository userRepository;
    private final StoryToyRepository storyToyRepository;
    private final S3Service s3Service;
    private final ContentModerationService moderation;

    @Transactional
    public ToyResponseDto createToy(Long userId, String name, String personality,
                                    String description, MultipartFile image) {
        moderation.check(name);
        moderation.check(personality);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            imageUrl = s3Service.uploadFile(image, "toys");
        }

        Toy toy = Toy.builder()
                .user(user)
                .name(name)
                .imageUrl(imageUrl)
                .personality(personality)
                .description(description)
                .build();

        return ToyResponseDto.from(toyRepository.save(toy));
    }

    public List<ToyResponseDto> getMyToys(Long userId) {
        return toyRepository.findAllByUserId(userId).stream()
                .map(ToyResponseDto::from)
                .toList();
    }

    @Transactional
    public ToyResponseDto updateToy(Long userId, Long toyId, String name, String personality,
                                    String description, MultipartFile image) {
        Toy toy = toyRepository.findById(toyId)
                .orElseThrow(() -> new CustomException(ErrorCode.TOY_NOT_FOUND));
        if (!toy.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.TOY_ACCESS_DENIED);
        }

        moderation.check(name);
        moderation.check(personality);
        if (name != null && !name.isBlank()) toy.updateName(name);
        if (personality != null) toy.updatePersonality(personality.isBlank() ? null : personality);
        if (description != null) toy.updateDescription(description.isBlank() ? null : description);

        if (image != null && !image.isEmpty()) {
            if (toy.getImageUrl() != null) s3Service.deleteFile(toy.getImageUrl());
            toy.updateImageUrl(s3Service.uploadFile(image, "toys"));
        }

        return ToyResponseDto.from(toyRepository.save(toy));
    }

    @Transactional
    public void deleteToy(Long userId, Long toyId) {
        if (!toyRepository.existsByIdAndUserId(toyId, userId)) {
            throw new CustomException(ErrorCode.TOY_ACCESS_DENIED);
        }
        // Remove all story_toy entries referencing this toy before deletion
        storyToyRepository.deleteAllByToyId(toyId);
        toyRepository.deleteById(toyId);
    }
}
