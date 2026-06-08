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

    private static final java.util.Random RANDOM = new java.util.Random();

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

        AgeProfile ageProfile = resolveAgeProfile(targetAge);

        prompt.append("\nRequirements:\n");
        prompt.append("- Target length: ").append(ageProfile.lengthGuidance()).append("\n");
        prompt.append("- Vocabulary & sentences: ").append(ageProfile.vocabularyGuidance()).append("\n");
        prompt.append("- Plot structure: ").append(ageProfile.structureGuidance()).append("\n");
        prompt.append("- Fun, imaginative, and end with a positive message\n");
        prompt.append("- Each toy must play a meaningful role in the story\n");
        prompt.append("- Write the story only — no meta-commentary or introduction\n");

        if (userPrompt != null && !userPrompt.isBlank()) {
            // 유저가 직접 아이디어를 입력한 경우: 유저 의도를 따르고 랜덤 스파크는 주입하지 않는다
            prompt.append("\nThe parent has provided a story idea — follow it as the main direction of the plot:\n");
            prompt.append(userPrompt).append("\n");
            prompt.append("Stay true to this idea while keeping the story age-appropriate and giving the toy character(s) a meaningful role.\n");
        } else {
            // 유저 입력이 없는 경우: 랜덤 스파크를 주입해 매번 다른 이야기가 나오도록 한다
            StorySpark spark = pickStorySpark(targetAge);
            prompt.append("\nToday's spark of inspiration (use it loosely as a starting point — reinterpret it in your own way rather than following it literally):\n");
            prompt.append("- Setting: ").append(spark.setting()).append("\n");
            prompt.append("- Something that kicks off the adventure: ").append(spark.incident()).append("\n");
            prompt.append("- A feeling or value the story can gently explore: ").append(spark.valueToExplore()).append("\n");
            prompt.append("Make this story feel fresh — avoid the most predictable, cliché version of this idea, and give the toy character(s) a distinctive way of handling what happens.\n");
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

    /**
     * 대상 연령에 따라 길이/어휘/플롯 "구조" 난이도를 다르게 가져가기 위한 가이드를 결정합니다.
     * 3~5세(유아), 6~8세(저학년), 9~11세(고학년) 세 구간으로 나눕니다.
     * 실제 줄거리 소재(배경/사건/주제)는 매번 달라지도록 {@link #pickStorySpark}에서 무작위로 결정합니다 —
     * 여기서는 "이야기를 어떻게 짜야 하는가"의 복잡도만 다룹니다.
     */
    private AgeProfile resolveAgeProfile(int age) {
        if (age <= 5) {
            return new AgeProfile(
                    "120-220 words",
                    "Very short, simple sentences (about 5-8 words each). Use only everyday words a young child already knows. Repeat key phrases for comfort and rhythm, and add playful sounds (e.g. 'Whoosh!', 'Giggle giggle').",
                    "Keep the plot to a single small problem that resolves quickly and gently — a calm, predictable arc from 'something happens' to 'all is cozy again', with no scary or stressful moments."
            );
        } else if (age <= 8) {
            return new AgeProfile(
                    "350-550 words",
                    "Clear, varied sentences with light dialogue and a few descriptive words. It's okay to use a handful of new-but-easy-to-guess words.",
                    "Use a simple three-part arc: a clear beginning, one fun challenge to face, and a satisfying resolution. A little humor along the way is welcome."
            );
        } else {
            return new AgeProfile(
                    "650-900 words",
                    "Richer vocabulary, varied sentence lengths, and natural dialogue. Feel free to introduce a few new words the child can learn from context.",
                    "Build a slightly more layered arc — a real challenge, a moment of doubt or a small twist, and a meaningful resolution. Some suspense, humor, or emotional depth is welcome."
            );
        }
    }

    private record AgeProfile(String lengthGuidance, String vocabularyGuidance, String structureGuidance) {}

    /**
     * 매 생성마다 배경/사건/탐구할 가치를 무작위로 골라 "오늘의 영감"으로 제시합니다.
     * 같은 인형·같은 나이로 여러 번 생성해도 줄거리가 매번 달라지도록 다양성을 부여하는 장치입니다.
     */
    private StorySpark pickStorySpark(int age) {
        List<String> settings;
        List<String> incidents;
        List<String> values;

        if (age <= 5) {
            settings = List.of(
                    "a cozy living room on a rainy afternoon",
                    "a sunny backyard garden",
                    "a warm kitchen that smells like fresh cookies",
                    "a soft pile of blankets at naptime",
                    "a colorful toy box full of old friends",
                    "a little porch on a breezy morning",
                    "a bubble-filled bathtub on a quiet evening",
                    "a flower shop with tall bouquets everywhere",
                    "the backseat of a car on a long, winding drive",
                    "a grandparent's house full of interesting knick-knacks",
                    "a small blanket-and-pillow fort in the living room",
                    "a farmer's market stall on a crisp autumn morning"
            );
            incidents = List.of(
                    "a favorite button goes missing",
                    "a butterfly flutters in through an open window",
                    "a tiny puddle appears after the rain",
                    "a new toy friend arrives in a box",
                    "a balloon gets stuck up high",
                    "a sock puppet wants to join the fun",
                    "a tiny snail is found waiting on the doorstep",
                    "someone's hat blows away in a gust of wind",
                    "the lights flicker off for a few cozy minutes",
                    "a drawing gets accidentally crumpled up",
                    "a very loud bird starts singing right outside the window",
                    "a piece of toast lands jelly-side down"
            );
            values = List.of(
                    "sharing",
                    "being gentle with a friend",
                    "trying again after a little stumble",
                    "saying sorry and making up",
                    "helping someone feel better",
                    "taking turns",
                    "being patient while waiting for something exciting",
                    "noticing when a friend feels left out",
                    "being careful with things that belong to others",
                    "asking for help when something is too hard alone",
                    "saying thank you and really meaning it",
                    "being curious instead of scared of something new"
            );
        } else if (age <= 8) {
            settings = List.of(
                    "a treehouse at the edge of the woods",
                    "a sandy beach at low tide",
                    "a sleepy little town on market day",
                    "an old library with creaky shelves",
                    "a garden the morning after a summer storm",
                    "a classroom on the last day before vacation",
                    "a circus tent pitched at the edge of town",
                    "a snowy hillside on the first day of winter",
                    "a train journey through rolling countryside",
                    "a baking competition in the school gym",
                    "a flooded street that looks like a little river",
                    "a rooftop terrace on a warm summer evening"
            );
            incidents = List.of(
                    "a mysterious little map turns up in a drawer",
                    "a strange noise comes from behind the shed",
                    "a new friend moves in next door",
                    "a class project goes hilariously wrong",
                    "a sudden rainstorm traps everyone indoors",
                    "something goes missing right before a big day",
                    "an important message gets delivered to the wrong address",
                    "the power cuts out right in the middle of something",
                    "two friends both promise to help two different people at the same time",
                    "a shortcut turns out to be anything but short",
                    "a contest is announced and the prize is something everyone wants",
                    "an old photograph raises a question nobody can answer"
            );
            values = List.of(
                    "teamwork",
                    "being brave even when you feel small",
                    "standing up for a friend",
                    "patience when plans change",
                    "curiosity that leads to a happy surprise",
                    "honesty, even when it's awkward",
                    "including someone who feels left out",
                    "apologising first, even when it's hard",
                    "asking for help instead of struggling alone",
                    "keeping a promise even when it gets inconvenient",
                    "trying something new even if you might not be good at it",
                    "finding the bright side of something that seems bad at first"
            );
        } else {
            settings = List.of(
                    "an old lighthouse on a foggy coast",
                    "a bustling night market",
                    "a quiet attic full of forgotten things",
                    "a winding forest trail just before sunset",
                    "an abandoned treehouse fort",
                    "a school trip that takes an unexpected turn",
                    "a local newspaper office that is about to close down",
                    "a rooftop garden that almost nobody knew was there",
                    "a town where something strange has been happening every Thursday",
                    "a workshop cluttered with half-finished inventions",
                    "a public swimming pool on its very last day of the season",
                    "the waiting room of a peculiar and slightly unusual train station"
            );
            incidents = List.of(
                    "a coded message turns up in an unlikely place",
                    "a rival shows up with a plan of their own",
                    "an old rumor turns out to be only half true",
                    "a sudden change of plans throws everyone off",
                    "a stranger asks for help with a small secret",
                    "two friends discover they want very different things",
                    "something that was supposed to be easy turns out to be surprisingly difficult",
                    "a small, unintentional lie starts growing into something harder to undo",
                    "an old letter or object reveals something unexpected about someone's past",
                    "two people who thought they were rivals discover they actually need each other",
                    "the thing everyone was worrying about turns out to be something else entirely",
                    "a plan that looks perfect on paper starts falling apart almost immediately"
            );
            values = List.of(
                    "the value of honesty even when it's hard",
                    "growing past a fear",
                    "thinking twice before jumping to conclusions",
                    "finding common ground with someone very different from you",
                    "the quiet courage of doing the right thing",
                    "learning that mistakes aren't the end of the world",
                    "knowing when to lead and when to step back",
                    "the difference between keeping a secret and telling a lie",
                    "how perspective shifts when you see things from someone else's point of view",
                    "the importance of finishing what you started, even when enthusiasm fades",
                    "recognising the value in something that's easy to overlook",
                    "choosing kindness even when nobody would notice if you didn't"
            );
        }

        return new StorySpark(
                settings.get(RANDOM.nextInt(settings.size())),
                incidents.get(RANDOM.nextInt(incidents.size())),
                values.get(RANDOM.nextInt(values.size()))
        );
    }

    private record StorySpark(String setting, String incident, String valueToExplore) {}

    private String detectMimeType(String imageUrl) {
        String lower = imageUrl.toLowerCase();
        if (lower.contains(".png")) return "image/png";
        if (lower.contains(".gif")) return "image/gif";
        if (lower.contains(".webp")) return "image/webp";
        return "image/jpeg";
    }

    public record GeminiResult(String title, String content) {}
}
