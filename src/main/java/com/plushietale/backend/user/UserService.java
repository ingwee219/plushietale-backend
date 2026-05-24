package com.plushietale.backend.user;

import com.plushietale.backend.comment.CommentRepository;
import com.plushietale.backend.comment.dto.MyCommentResponseDto;
import com.plushietale.backend.global.exception.CustomException;
import com.plushietale.backend.global.exception.ErrorCode;
import com.plushietale.backend.post.PostRepository;
import com.plushietale.backend.post.dto.PostResponseDto;
import com.plushietale.backend.user.dto.UpdateNicknameRequestDto;
import com.plushietale.backend.user.dto.UpdatePasswordRequestDto;
import com.plushietale.backend.user.dto.UserResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserResponseDto getMyInfo(User user) {
        return UserResponseDto.from(user);
    }

    @Transactional
    public UserResponseDto updateNickname(Long userId, UpdateNicknameRequestDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        user.updateNickname(request.getNickname());
        return UserResponseDto.from(user);
    }

    public List<PostResponseDto> getMyPosts(Long userId) {
        return postRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(PostResponseDto::from)
                .toList();
    }

    public List<MyCommentResponseDto> getMyComments(Long userId) {
        return commentRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(MyCommentResponseDto::from)
                .toList();
    }

    @Transactional
    public void updatePassword(Long userId, UpdatePasswordRequestDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (user.getPassword() == null || !passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        user.updatePassword(passwordEncoder.encode(request.getNewPassword()));
    }
}
