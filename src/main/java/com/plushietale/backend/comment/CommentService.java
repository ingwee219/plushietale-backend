package com.plushietale.backend.comment;

import com.plushietale.backend.comment.dto.CommentRequestDto;
import com.plushietale.backend.comment.dto.CommentResponseDto;
import com.plushietale.backend.global.exception.CustomException;
import com.plushietale.backend.global.exception.ErrorCode;
import com.plushietale.backend.global.moderation.ContentModerationService;
import com.plushietale.backend.post.Post;
import com.plushietale.backend.post.PostRepository;
import com.plushietale.backend.user.User;
import com.plushietale.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final ContentModerationService moderation;

    @Transactional
    public CommentResponseDto createComment(Long userId, Long postId, CommentRequestDto request) {
        moderation.check(request.getContent());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        Comment comment = Comment.builder()
                .user(user)
                .post(post)
                .content(request.getContent())
                .build();

        return CommentResponseDto.from(commentRepository.save(comment));
    }

    public List<CommentResponseDto> getComments(Long postId) {
        if (!postRepository.existsById(postId)) {
            throw new CustomException(ErrorCode.POST_NOT_FOUND);
        }
        return commentRepository.findAllByPostIdOrderByCreatedAtAsc(postId).stream()
                .map(CommentResponseDto::from)
                .toList();
    }

    @Transactional
    public CommentResponseDto updateComment(Long userId, Long commentId, CommentRequestDto request) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

        if (!comment.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.COMMENT_ACCESS_DENIED);
        }

        moderation.check(request.getContent());
        comment.update(request.getContent());
        return CommentResponseDto.from(comment);
    }

    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

        if (!comment.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.COMMENT_ACCESS_DENIED);
        }

        commentRepository.delete(comment);
    }
}
