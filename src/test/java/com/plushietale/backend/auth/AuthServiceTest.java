package com.plushietale.backend.auth;

import com.plushietale.backend.auth.dto.AuthResponseDto;
import com.plushietale.backend.auth.dto.LoginRequestDto;
import com.plushietale.backend.auth.dto.SignupRequestDto;
import com.plushietale.backend.global.exception.CustomException;
import com.plushietale.backend.global.exception.ErrorCode;
import com.plushietale.backend.user.Provider;
import com.plushietale.backend.user.Role;
import com.plushietale.backend.user.User;
import com.plushietale.backend.user.UserRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * AuthService 단위 테스트.
 *
 * Repository / PasswordEncoder / JwtTokenProvider 를 모두 mock 으로 대체해
 * 실제 DB·암호화·토큰 생성 없이 비즈니스 분기만 빠르게 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    private SignupRequestDto signupRequest(String email, String password, String nickname) {
        SignupRequestDto dto = new SignupRequestDto();
        dto.setEmail(email);
        dto.setPassword(password);
        dto.setNickname(nickname);
        return dto;
    }

    private LoginRequestDto loginRequest(String email, String password) {
        LoginRequestDto dto = new LoginRequestDto();
        dto.setEmail(email);
        dto.setPassword(password);
        return dto;
    }

    @Nested
    @DisplayName("회원가입")
    class Signup {

        @Test
        @DisplayName("새 이메일이면 사용자를 저장하고 토큰을 반환한다")
        void signsUpNewUser() {
            SignupRequestDto request = signupRequest("new@test.com", "password123", "newbie");
            given(userRepository.existsByEmail("new@test.com")).willReturn(false);
            given(passwordEncoder.encode("password123")).willReturn("ENCODED");
            given(jwtTokenProvider.generateToken(any(), any(), any())).willReturn("TOKEN");

            AuthResponseDto response = authService.signup(request);

            assertThat(response.getToken()).isEqualTo("TOKEN");
            assertThat(response.getEmail()).isEqualTo("new@test.com");
            assertThat(response.getNickname()).isEqualTo("newbie");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("이미 가입된 이메일이면 DUPLICATE_EMAIL 예외를 던지고 저장하지 않는다")
        void rejectsDuplicateEmail() {
            SignupRequestDto request = signupRequest("dup@test.com", "password123", "nick");
            given(userRepository.existsByEmail("dup@test.com")).willReturn(true);

            assertThatThrownBy(() -> authService.signup(request))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.DUPLICATE_EMAIL);

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("로그인")
    class Login {

        private User localUser(String email, String encodedPassword) {
            return User.builder()
                    .id(1L)
                    .email(email)
                    .password(encodedPassword)
                    .nickname("nick")
                    .provider(Provider.LOCAL)
                    .role(Role.USER)
                    .build();
        }

        @Test
        @DisplayName("올바른 자격이면 토큰을 반환한다")
        void logsInWithValidCredentials() {
            User user = localUser("user@test.com", "ENCODED");
            given(userRepository.findByEmail("user@test.com")).willReturn(Optional.of(user));
            given(passwordEncoder.matches("password123", "ENCODED")).willReturn(true);
            given(jwtTokenProvider.generateToken(any(), any(), any())).willReturn("TOKEN");

            AuthResponseDto response = authService.login(loginRequest("user@test.com", "password123"));

            assertThat(response.getToken()).isEqualTo("TOKEN");
            assertThat(response.getEmail()).isEqualTo("user@test.com");
        }

        @Test
        @DisplayName("존재하지 않는 이메일이면 USER_NOT_FOUND 예외를 던진다")
        void rejectsUnknownEmail() {
            given(userRepository.findByEmail("ghost@test.com")).willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(loginRequest("ghost@test.com", "password123")))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);
        }

        @Test
        @DisplayName("비밀번호가 틀리면 INVALID_PASSWORD 예외를 던진다")
        void rejectsWrongPassword() {
            User user = localUser("user@test.com", "ENCODED");
            given(userRepository.findByEmail("user@test.com")).willReturn(Optional.of(user));
            given(passwordEncoder.matches("wrong", "ENCODED")).willReturn(false);

            assertThatThrownBy(() -> authService.login(loginRequest("user@test.com", "wrong")))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_PASSWORD);
        }

        @Test
        @DisplayName("소셜 가입 사용자(비밀번호 null)는 일반 로그인 시 INVALID_PASSWORD 예외를 던진다")
        void rejectsSocialUserLocalLogin() {
            User socialUser = User.builder()
                    .id(2L)
                    .email("google@test.com")
                    .password(null)             // 소셜 로그인 사용자는 비밀번호가 없다
                    .nickname("googler")
                    .provider(Provider.GOOGLE)
                    .role(Role.USER)
                    .build();
            given(userRepository.findByEmail("google@test.com")).willReturn(Optional.of(socialUser));

            assertThatThrownBy(() -> authService.login(loginRequest("google@test.com", "password123")))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_PASSWORD);
        }
    }
}
