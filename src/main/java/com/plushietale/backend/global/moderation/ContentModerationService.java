package com.plushietale.backend.global.moderation;

import com.plushietale.backend.global.exception.CustomException;
import com.plushietale.backend.global.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 유저가 입력하는 텍스트(닉네임, 인형 이름, 게시글, 댓글, 스토리 아이디어)에 대해
 * 키워드 기반 1차 검열을 수행합니다.
 *
 * 매칭 전략:
 *  - 입력값을 소문자로 정규화한 뒤 각 키워드가 포함되어 있는지 확인합니다.
 *  - 영어 단어는 단어 경계(\b)로 감싸 "class" 에서 "ass" 를 오탐하는 것을 방지합니다.
 *  - 한국어/기타 문자는 단어 경계 없이 부분 문자열 매칭합니다.
 */
@Service
public class ContentModerationService {

    // 영어 키워드 — 단어 경계(\b) 매칭으로 오탐 방지
    private static final List<String> ENGLISH_KEYWORDS = List.of(
            "fuck", "shit", "bitch", "cunt", "nigger", "nigga",
            "asshole", "bastard", "whore", "slut", "dick", "cock",
            "pussy", "faggot", "retard", "motherfucker", "jackass",
            "rape", "pedophile", "pedo", "molest", "incest"
    );

    // 한국어 키워드 — 부분 문자열 매칭
    private static final List<String> KOREAN_KEYWORDS = List.of(
            "씨발", "시발", "ㅅㅂ", "개새끼", "개새기", "ㄱㅅㄲ",
            "지랄", "존나", "ㅈㄴ", "병신", "ㅂㅅ", "미친놈", "미친년",
            "창녀", "보지", "자지", "섹스", "강간", "성폭행",
            "새끼", "찐따", "느그", "니애미", "니에미", "꺼져",
            "죽어", "죽여", "살인", "자살해"
    );

    /**
     * 텍스트가 부적절한 표현을 포함하면 {@link CustomException}을 던집니다.
     * null 이거나 빈 문자열이면 아무것도 하지 않습니다.
     */
    public void check(String text) {
        if (text == null || text.isBlank()) return;

        String normalized = text.toLowerCase();

        for (String keyword : ENGLISH_KEYWORDS) {
            // 단어 경계로 오탐 방지 (예: "class" 에서 "ass" 매칭 안 됨)
            if (normalized.matches(".*\\b" + keyword + "\\b.*")) {
                throw new CustomException(ErrorCode.INAPPROPRIATE_CONTENT);
            }
        }

        for (String keyword : KOREAN_KEYWORDS) {
            if (normalized.contains(keyword)) {
                throw new CustomException(ErrorCode.INAPPROPRIATE_CONTENT);
            }
        }
    }
}
