package com.plushietale.backend.post;

import com.plushietale.backend.global.exception.CustomException;
import com.plushietale.backend.global.exception.ErrorCode;
import com.plushietale.backend.post.dto.LikeResponseDto;
import com.plushietale.backend.post.dto.PostRequestDto;
import com.plushietale.backend.post.dto.PostResponseDto;
import com.plushietale.backend.post.dto.PostUpdateRequestDto;
import com.plushietale.backend.story.Story;
import com.plushietale.backend.story.StoryRepository;
import com.plushietale.backend.user.User;
import com.plushietale.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final StoryRepository storyRepository;
    private final UserRepository userRepository;

    @Transactional
    public PostResponseDto createPost(Long userId, PostRequestDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Story story = storyRepository.findById(request.getStoryId())
                .orElseThrow(() -> new CustomException(ErrorCode.STORY_NOT_FOUND));

        if (!story.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.STORY_ACCESS_DENIED);
        }

        if (postRepository.existsByStoryId(request.getStoryId())) {
            throw new CustomException(ErrorCode.STORY_ALREADY_POSTED);
        }

        Post post = Post.builder()
                .user(user)
                .story(story)
                .title(request.getTitle())
                .content(request.getContent())
                .build();

        return PostResponseDto.from(postRepository.save(post));
    }

    public Page<PostResponseDto> getPosts(Pageable pageable, Long userId) {
        return postRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(post -> {
                    boolean likedByMe = userId != null &&
                            postLikeRepository.existsByPostIdAndUserId(post.getId(), userId);
                    return PostResponseDto.from(post, likedByMe);
                });
    }

    @Transactional
    public PostResponseDto getPost(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
        post.incrementViewCount();
        boolean likedByMe = userId != null &&
                postLikeRepository.existsByPostIdAndUserId(postId, userId);
        return PostResponseDto.from(post, likedByMe);
    }

    @Transactional
    public LikeResponseDto toggleLike(Long userId, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        boolean likedByMe;
        if (postLikeRepository.existsByPostIdAndUserId(postId, userId)) {
            postLikeRepository.deleteByPostIdAndUserId(postId, userId);
            likedByMe = false;
        } else {
            postLikeRepository.save(PostLike.builder().post(post).user(user).build());
            likedByMe = true;
        }
        int likeCount = (int) postLikeRepository.countByPostId(postId);
        return new LikeResponseDto(likeCount, likedByMe);
    }

    @Transactional
    public PostResponseDto updatePost(Long userId, Long postId, PostUpdateRequestDto request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        if (!post.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.POST_ACCESS_DENIED);
        }

        post.update(request.getTitle(), request.getContent());
        return PostResponseDto.from(post);
    }

    @Transactional
    public void deletePost(Long userId, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        if (!post.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.POST_ACCESS_DENIED);
        }

        postRepository.delete(post);
    }
}
