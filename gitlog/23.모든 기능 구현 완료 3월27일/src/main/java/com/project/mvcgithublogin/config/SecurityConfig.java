package com.project.mvcgithublogin.config;

import com.project.mvcgithublogin.domain.User;
import com.project.mvcgithublogin.service.GithubConnectionService;
import com.project.mvcgithublogin.service.GithubStackService;
import com.project.mvcgithublogin.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationSuccessHandler oauthSuccessHandler) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .oauth2Login(oauth -> oauth
                        .loginPage("/login")
                        .successHandler(oauthSuccessHandler)
                )
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable());

        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler oauthSuccessHandler(
            UserService userService,
            OAuth2AuthorizedClientService authorizedClientService,
            GithubStackService githubStackService,
            GithubConnectionService githubConnectionService
    ) {
        return (HttpServletRequest request, HttpServletResponse response, org.springframework.security.core.Authentication authentication) -> {
            String redirectTo = "/";

            if (authentication instanceof OAuth2AuthenticationToken token) {
                OAuth2User oauthUser = token.getPrincipal();
                String registrationId = token.getAuthorizedClientRegistrationId();
                HttpSession session = request.getSession(true);

                if ("google".equalsIgnoreCase(registrationId)) {
                    String email = stringValue(oauthUser.getAttribute("email")).trim().toLowerCase();
                    String name = stringValue(oauthUser.getAttribute("name")).trim();
                    String subject = stringValue(oauthUser.getAttribute("sub")).trim();
                    User user = userService.findOrCreateGoogleUser(email, name, subject);
                    session.setAttribute("loginUser", user.getId());
                } else if ("github".equalsIgnoreCase(registrationId)) {
                    OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(registrationId, token.getName());
                    if (client != null && client.getAccessToken() != null) {
                        String githubAccessToken = client.getAccessToken().getTokenValue();
                        GithubStackService.GithubUserProfile profile = githubStackService.fetchAuthenticatedUserProfile(
                                githubAccessToken,
                                oauthUser.getAttributes() == null
                                        ? null
                                        : new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(oauthUser.getAttributes())
                        );

                        if (githubConnectionService.isLinkFlow(session) && session.getAttribute("loginUser") != null) {
                            githubConnectionService.storeLinkedGithub(session, githubAccessToken, profile.githubLogin());
                            githubConnectionService.persistLinkedGithub(
                                    String.valueOf(session.getAttribute("loginUser")),
                                    githubAccessToken,
                                    profile.githubLogin()
                            );
                            githubConnectionService.setFlashMessage(
                                    session,
                                    "GitHub @" + profile.githubLogin() + " 계정이 연동되었습니다.",
                                    "success"
                            );
                            redirectTo = githubConnectionService.consumeReturnTo(session);
                        } else {
                            User user = userService.findOrCreateGithubUser(profile.loginId(), profile.nickname(), profile.githubId());
                            session.setAttribute("loginUser", user.getId());
                            githubConnectionService.storeOauthGithubLogin(
                                    session,
                                    githubAccessToken,
                                    profile.githubLogin().isBlank() ? user.getId() : profile.githubLogin()
                            );
                            githubConnectionService.clearLinkFlow(session);
                            redirectTo = "/";
                        }
                    }
                }
            }

            response.sendRedirect(redirectTo);
        };
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
