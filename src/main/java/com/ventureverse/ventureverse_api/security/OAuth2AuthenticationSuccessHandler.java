package com.ventureverse.ventureverse_api.security;

import com.ventureverse.ventureverse_api.entities.User;
import com.ventureverse.ventureverse_api.repositories.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        System.out.println("===== OAuth2 SUCCESS HANDLER CALLED =====");

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oauth2User = oauthToken.getPrincipal();

        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");
        String googleId = oauth2User.getAttribute("sub");
        String pictureUrl = oauth2User.getAttribute("picture");

        System.out.println("Google Email: " + email);
        System.out.println("Google Name: " + name);

        Optional<User> existingUser = userRepository.findByEmail(email);

        User user;
        if (existingUser.isPresent()) {
            System.out.println("Existing user found");
            user = existingUser.get();
            user.setOauthProvider("google");
            user.setOauthProviderId(googleId);
            user.setProfilePictureUrl(pictureUrl);
            user.setEmailVerified(true);
            user.setLastLoginAt(LocalDateTime.now());
        } else {
            System.out.println("Creating new user");
            String username = email.split("@")[0];
            if (userRepository.existsByUsername(username)) {
                username = username + "_" + (System.currentTimeMillis() % 10000);
            }

            user = User.builder()
                    .fullName(name)
                    .username(username)
                    .email(email)
                    .oauthProvider("google")
                    .oauthProviderId(googleId)
                    .profilePictureUrl(pictureUrl)
                    .emailVerified(true)
                    .enabled(true)
                    .createdAt(LocalDateTime.now())
                    .lastLoginAt(LocalDateTime.now())
                    .build();
        }

        userRepository.save(user);
        System.out.println("User saved: " + user.getUsername());

        String token = jwtService.generateToken(user.getUsername());
        System.out.println("Token generated: " + token.substring(0, 20) + "...");

        String redirectUrl = frontendUrl + "/oauth2/callback?" +
                "token=" + URLEncoder.encode(token, StandardCharsets.UTF_8) +
                "&username=" + URLEncoder.encode(user.getUsername(), StandardCharsets.UTF_8) +
                "&email=" + URLEncoder.encode(user.getEmail(), StandardCharsets.UTF_8) +
                "&fullName=" + URLEncoder.encode(user.getFullName(), StandardCharsets.UTF_8) +
                "&id=" + user.getId();

        System.out.println("Redirecting to: " + redirectUrl);

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}