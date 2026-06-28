package com.plushietale.backend.global.moderation;

import com.plushietale.backend.global.exception.CustomException;
import com.plushietale.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ContentModerationService 단위 테스트.
 *
 * 외부 의존성이 전혀 없는 순수 로직이라 mock 없이 바로 인스턴스를 만들어 검증한다.
 */
class ContentModerationServiceTest {

    private final ContentModerationService moderation = new ContentModerationService();

    @Nested
    @DisplayName("부적절한 내용을 차단한다")
    class Blocks {

        @ParameterizedTest
        @ValueSource(strings = {"fuck", "this is shit", "you bastard"})
        @DisplayName("영어 비속어가 포함되면 예외를 던진다")
        void blocksEnglishProfanity(String text) {
            assertThatThrownBy(() -> moderation.check(text))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INAPPROPRIATE_CONTENT);
        }

        @ParameterizedTest
        @ValueSource(strings = {"씨발", "너 진짜 병신이야", "ㅈㄴ 별로"})
        @DisplayName("한국어 비속어가 포함되면 예외를 던진다")
        void blocksKoreanProfanity(String text) {
            assertThatThrownBy(() -> moderation.check(text))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INAPPROPRIATE_CONTENT);
        }

        @Test
        @DisplayName("대소문자를 구분하지 않는다")
        void isCaseInsensitive() {
            assertThatThrownBy(() -> moderation.check("FUCK YOU"))
                    .isInstanceOf(CustomException.class);
        }
    }

    @Nested
    @DisplayName("정상적인 내용은 통과시킨다")
    class Allows {

        @ParameterizedTest
        @ValueSource(strings = {
                "Daisy the Goose",
                "What a lovely bedtime story!",
                "오늘은 즐거운 하루였어요",
        })
        @DisplayName("깨끗한 텍스트는 예외 없이 통과한다")
        void allowsCleanText(String text) {
            assertThatCode(() -> moderation.check(text)).doesNotThrowAnyException();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("null·빈 문자열·공백만 있는 입력은 통과한다")
        void allowsNullAndBlank(String text) {
            assertThatCode(() -> moderation.check(text)).doesNotThrowAnyException();
        }

        @ParameterizedTest
        @ValueSource(strings = {"cockpit", "Dickinson", "Scunthorpe"})
        @DisplayName("비속어를 부분 문자열로 포함한 정상 단어는 오탐하지 않는다 (단어 경계 매칭)")
        void doesNotFalsePositiveOnSubstrings(String text) {
            // "cockpit"의 "cock", "Dickinson"의 "dick", "Scunthorpe"의 "cunt" 등은
            // 단어 경계(\b)가 맞지 않으므로 차단되면 안 된다.
            assertThatCode(() -> moderation.check(text)).doesNotThrowAnyException();
        }
    }
}
