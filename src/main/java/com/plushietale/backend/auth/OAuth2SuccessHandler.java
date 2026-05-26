package com.plushietale.backend.auth;

import com.plushietale.backend.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Value("${app.frontend-url}")
    private String frontendUrl;

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Long userId = (Long) oAuth2User.getAttributes().get("userId");

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found after OAuth2 authentication."));

        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail(), user.getRole());

        // JWT를 URL 파라미터로 담아 프론트엔드로 리다이렉트
        getRedirectStrategy().sendRedirect(request, response, frontendUrl + "/oauth2/callback?token=" + token);
    }
}
