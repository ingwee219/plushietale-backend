package com.plushietale.backend.story;

import com.plushietale.backend.ai.GeminiService;
import com.plushietale.backend.global.exception.CustomException;
import com.plushietale.backend.global.exception.ErrorCode;
import com.plushietale.backend.global.moderation.ContentModerationService;
import com.plushietale.backend.post.PostRepository;
import com.plushietale.backend.story.dto.StoryRequestDto;
import com.plushietale.backend.story.dto.StoryResponseDto;
import com.plushietale.backend.toy.Toy;
import com.plushietale.backend.toy.ToyRepository;
import com.plushietale.backend.user.User;
import com.plushietale.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoryService {

    private final StoryRepository storyRepository;
    private final ToyRepository toyRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final GeminiService geminiService;
    private final ContentModerationService moderation;

    @Transactional
    public StoryResponseDto createStory(Long userId, StoryRequestDto request) {
        // Gemini에 전달되기 전에 유저의 스토리 아이디어 입력을 먼저 검열
        moderation.check(request.getPrompt());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 모든 인형이 존재하고 이 사용자 소유인지 검증
        List<Toy> toys = request.getToyIds().stream()
                .map(toyId -> {
                    if (!toyRepository.existsByIdAndUserId(toyId, userId)) {
                        throw new CustomException(ErrorCode.TOY_ACCESS_DENIED);
                    }
                    return toyRepository.findById(toyId)
                            .orElseThrow(() -> new CustomException(ErrorCode.TOY_NOT_FOUND));
                })
                .toList();

        // Gemini API 호출로 이야기 생성
        GeminiService.GeminiResult result = geminiService.generateStory(toys, request.getTargetAge(), request.getPrompt());

        Story story = Story.builder()
                .user(user)
                .title(result.title())
                .content(result.content())
                .targetAge(request.getTargetAge())
                .build();

        storyRepository.save(story);

        toys.forEach(toy -> story.getStoryToys().add(
                StoryToy.builder()
                        .story(story)
                        .toy(toy)
                        .build()
        ));

        return StoryResponseDto.from(story);
    }

    public List<StoryResponseDto> getMyStories(Long userId) {
        return storyRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(StoryResponseDto::from)
                .toList();
    }

    public StoryResponseDto getStory(Long userId, Long storyId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new CustomException(ErrorCode.STORY_NOT_FOUND));

        if (!story.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.STORY_ACCESS_DENIED);
        }

        return StoryResponseDto.from(story);
    }

    @Transactional
    public void deleteStory(Long userId, Long storyId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new CustomException(ErrorCode.STORY_NOT_FOUND));

        if (!story.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.STORY_ACCESS_DENIED);
        }

        // Post FK 제약 때문에 연결된 게시글 먼저 삭제
        postRepository.deleteByStoryId(storyId);
        storyRepository.delete(story);
    }
}
