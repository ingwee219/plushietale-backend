package com.plushietale.backend.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.plushietale.backend.global.exception.CustomException;
import com.plushietale.backend.global.exception.ErrorCode;
import com.plushietale.backend.storage.S3Service;
import com.plushietale.backend.toy.Toy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Base64;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiService {

    private static final String GEMINI_API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

    private final S3Service s3Service;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api-key}")
    private String apiKey;

    /**
     * 인형 정보를 기반으로 Gemini API를 호출해 어린이 이야기를 생성합니다.
     * @return "제목\n\n본문" 형식의 문자열
     */
    public GeminiResult generateStory(List<Toy> toys, Integer targetAge, String userPrompt) {
        ObjectNode requestBody = buildRequestBody(toys, targetAge, userPrompt);

        String responseJson;
        try {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(Duration.ofSeconds(10));
            factory.setReadTimeout(Duration.ofSeconds(120));
            RestClient restClient = RestClient.builder().requestFactory(factory).build();
            responseJson = restClient.post()
                    .uri(GEMINI_API_URL + "?key=" + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody.toString())
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            log.error("Gemini API call failed: {}", e.getMessage());
            throw new CustomException(ErrorCode.GEMINI_API_FAILED);
        }

        return parseResponse(responseJson);
    }

    private ObjectNode buildRequestBody(List<Toy> toys, Integer targetAge, String userPrompt) {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode contents = root.putArray("contents");
        ObjectNode content = contents.addObject();
        ArrayNode parts = content.putArray("parts");

        // 텍스트 프롬프트
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a warm and imaginative children's storyteller.\n\n");
        prompt.append("Create an original, engaging story for a ").append(targetAge).append("-year-old child");
        prompt.append(" featuring the following toy character(s):\n\n");

        for (Toy toy : toys) {
            prompt.append("- ").append(toy.getName());
            if (toy.getPersonality() != null && !toy.getPersonality().isBlank()) {
                prompt.append(" (personality: ").append(toy.getPersonality()).append(")");
            }
            prompt.append("\n");
        }

        prompt.append("\nRequirements:\n");
        prompt.append("- Length: 300-500 words\n");
        prompt.append("- Vocabulary and themes appropriate for age ").append(targetAge).append("\n");
        prompt.append("- Fun, imaginative, and end with a positive message\n");
        prompt.append("- Each toy must play a meaningful role in the story\n");
        prompt.append("- Write the story only — no meta-commentary or introduction\n");

        if (userPrompt != null && !userPrompt.isBlank()) {
            prompt.append("\nThe user has provided a story idea to follow:\n");
            prompt.append(userPrompt).append("\n");
            prompt.append("Incorporate this idea into the story while keeping it age-appropriate.\n");
        }

        prompt.append("\nAfter the story, on a new line write exactly:\n");
        prompt.append("TITLE: [your suggested title for this story]");

        parts.addObject().put("text", prompt.toString());

        // 이미지가 있는 인형은 base64로 인코딩해서 추가
        for (Toy toy : toys) {
            if (toy.getImageUrl() != null && !toy.getImageUrl().isBlank()) {
                try {
                    byte[] imageBytes = s3Service.downloadFile(toy.getImageUrl());
                    String base64 = Base64.getEncoder().encodeToString(imageBytes);
                    String mimeType = detectMimeType(toy.getImageUrl());

                    ObjectNode inlineData = objectMapper.createObjectNode();
                    inlineData.put("mime_type", mimeType);
                    inlineData.put("data", base64);
                    parts.addObject().set("inline_data", inlineData);
                } catch (Exception e) {
                    log.warn("Could not load image for toy '{}', proceeding without image: {}", toy.getName(), e.getMessage());
                }
            }
        }

        // 생성 설정 (thinkingBudget:0 → thinking tokens 비활성화, 출력 토큰 절약)
        ObjectNode generationConfig = root.putObject("generationConfig");
        generationConfig.put("temperature", 0.8);
        generationConfig.put("maxOutputTokens", 2048);
        generationConfig.putObject("thinkingConfig").put("thinkingBudget", 0);

        return root;
    }

    private GeminiResult parseResponse(String responseJson) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            String text = root
                    .path("candidates").get(0)
                    .path("content")
                    .path("parts").get(0)
                    .path("text").asText();

            // "TITLE: ..." 파싱 (Gemini가 **TITLE:** 처럼 bold로 반환할 수도 있음)
            java.util.regex.Pattern titlePattern = java.util.regex.Pattern
                    .compile("\\*{0,2}TITLE:\\*{0,2}\\s*(.+)$",
                            java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.MULTILINE);
            java.util.regex.Matcher matcher = titlePattern.matcher(text);
            if (matcher.find()) {
                String title = matcher.group(1).strip();
                String content = text.substring(0, matcher.start()).strip();
                return new GeminiResult(title, content);
            }

            // TITLE 마커 없으면 toy 이름 기반 기본 제목 사용, 전체 텍스트를 본문으로
            log.warn("TITLE: marker not found in Gemini response, using fallback title");
            return new GeminiResult("A Magical Story", text.strip());

        } catch (Exception e) {
            log.error("Failed to parse Gemini response: {}", e.getMessage());
            throw new CustomException(ErrorCode.GEMINI_API_FAILED);
        }
    }

    private String detectMimeType(String imageUrl) {
        String lower = imageUrl.toLowerCase();
        if (lower.contains(".png")) return "image/png";
        if (lower.contains(".gif")) return "image/gif";
        if (lower.contains(".webp")) return "image/webp";
        return "image/jpeg";
    }

    public record GeminiResult(String title, String content) {}
}
