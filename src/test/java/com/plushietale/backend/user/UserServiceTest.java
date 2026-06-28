package com.plushietale.backend.user;

import com.plushietale.backend.comment.CommentRepository;
import com.plushietale.backend.global.exception.CustomException;
import com.plushietale.backend.global.exception.ErrorCode;
import com.plushietale.backend.global.moderation.ContentModerationService;
import com.plushietale.backend.post.PostRepository;
import com.plushietale.backend.user.dto.UpdateNicknameRequestDto;
import com.plushietale.backend.user.dto.UpdatePasswordRequestDto;
import com.plushietale.backend.user.dto.UserResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * UserService 단위 테스트.
 *
 * 닉네임 변경(검열 연동)과 비밀번호 변경의 분기를 mock 기반으로 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private BCryptPasswordEncoder passwordEncoder;
    @Mock
    private ContentModerationService moderation;

    @InjectMocks
    private UserService userService;

    private User localUser(Long id, String encodedPassword) {
        return User.builder()
                .id(id)
                .email("user@test.com")
                .password(encodedPassword)
                .nickname("oldNick")
                .provider(Provider.LOCAL)
                .role(Role.USER)
                .build();
    }

    private UpdatePasswordRequestDto passwordRequest(String current, String next) {
        UpdatePasswordRequestDto dto = new UpdatePasswordRequestDto();
        dto.setCurrentPassword(current);
        dto.setNewPassword(next);
        return dto;
    }

    private UpdateNicknameRequestDto nicknameRequest(String nickname) {
        UpdateNicknameRequestDto dto = new UpdateNicknameRequestDto();
        dto.setNickname(nickname);
        return dto;
    }

    @Nested
    @DisplayName("비밀번호 변경")
    class UpdatePassword {

        @Test
        @DisplayName("현재 비밀번호가 맞으면 새 비밀번호를 인코딩해 저장한다")
        void updatesPasswordWhenCurrentMatches() {
            User user = localUser(1L, "OLD_ENCODED");
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(passwordEncoder.matches("current123", "OLD_ENCODED")).willReturn(true);
            given(passwordEncoder.encode("new123456")).willReturn("NEW_ENCODED");

            userService.updatePassword(1L, passwordRequest("current123", "new123456"));

            assertThat(user.getPassword()).isEqualTo("NEW_ENCODED");
        }

        @Test
        @DisplayName("사용자를 찾을 수 없으면 USER_NOT_FOUND 예외를 던진다")
        void rejectsWhenUserMissing() {
            given(userRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updatePassword(99L, passwordRequest("a", "new123456")))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);
        }

        @Test
        @DisplayName("현재 비밀번호가 틀리면 INVALID_PASSWORD 예외를 던진다")
        void rejectsWhenCurrentPasswordWrong() {
            User user = localUser(1L, "OLD_ENCODED");
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(passwordEncoder.matches("wrong", "OLD_ENCODED")).willReturn(false);

            assertThatThrownBy(() -> userService.updatePassword(1L, passwordRequest("wrong", "new123456")))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_PASSWORD);
        }

        @Test
        @DisplayName("소셜 가입 사용자(비밀번호 null)는 INVALID_PASSWORD 예외를 던진다")
        void rejectsSocialUser() {
            User socialUser = localUser(1L, null);
            given(userRepository.findById(1L)).willReturn(Optional.of(socialUser));

            assertThatThrownBy(() -> userService.updatePassword(1L, passwordRequest("any", "new123456")))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_PASSWORD);
        }
    }

    @Nested
    @DisplayName("닉네임 변경")
    class UpdateNickname {

        @Test
        @DisplayName("정상 닉네임이면 검열을 통과하고 닉네임을 바꾼다")
        void updatesNickname() {
            User user = localUser(1L, "ENCODED");
            given(userRepository.findById(1L)).willReturn(Optional.of(user));

            UserResponseDto response = userService.updateNickname(1L, nicknameRequest("newNick"));

            assertThat(user.getNickname()).isEqualTo("newNick");
            assertThat(response.getNickname()).isEqualTo("newNick");
        }

        @Test
        @DisplayName("검열에 걸리는 닉네임이면 예외를 던지고 사용자를 조회하지 않는다")
        void rejectsInappropriateNickname() {
            willThrow(new CustomException(ErrorCode.INAPPROPRIATE_CONTENT))
                    .given(moderation).check("badword");

            assertThatThrownBy(() -> userService.updateNickname(1L, nicknameRequest("badword")))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INAPPROPRIATE_CONTENT);

            verify(userRepository, never()).findById(org.mockito.ArgumentMatchers.anyLong());
        }
    }
}
