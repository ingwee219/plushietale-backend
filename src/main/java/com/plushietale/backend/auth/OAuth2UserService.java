package com.plushietale.backend.auth;

import com.plushietale.backend.user.Provider;
import com.plushietale.backend.user.Role;
import com.plushietale.backend.user.User;
import com.plushietale.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String providerId = oAuth2User.getAttribute("sub");  // Google의 고유 사용자 ID

        User user = processUser(email, name, providerId);

        // userId를 attributes에 추가해 SuccessHandler에서 꺼내 쓸 수 있게 함
        Map<String, Object> attributes = new HashMap<>(oAuth2User.getAttributes());
        attributes.put("userId", user.getId());

        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())),
                attributes,
                "email"
        );
    }

    private User processUser(String email, String name, String providerId) {
        // 케이스 1: 기존 Google 사용자 → 로그인
        var existingGoogleUser = userRepository.findByProviderAndProviderId(Provider.GOOGLE, providerId);
        if (existingGoogleUser.isPresent()) {
            return existingGoogleUser.get();
        }

        // 케이스 2: 같은 이메일의 LOCAL 계정 존재 → 계정 연동 (기존 계정 반환)
        var existingLocalUser = userRepository.findByEmail(email);
        if (existingLocalUser.isPresent()) {
            return existingLocalUser.get();
        }

        // 케이스 3: 신규 사용자 → 자동 회원가입
        String nickname = (name != null && !name.isBlank()) ? name : email.split("@")[0];
        return userRepository.save(User.builder()
                .email(email)
                .nickname(nickname)
                .provider(Provider.GOOGLE)
                .providerId(providerId)
                .role(Role.USER)
                .build());
    }
}
