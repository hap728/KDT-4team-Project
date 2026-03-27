package com.project.mvcgithublogin.service;

import jakarta.servlet.http.HttpSession;
import com.project.mvcgithublogin.profile.ProfileUser;
import com.project.mvcgithublogin.profile.ProfileUserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class GithubConnectionService {

    public static final String SESSION_GITHUB_ACCESS_TOKEN = "githubAccessToken";
    public static final String SESSION_GITHUB_LOGIN = "githubConnectedLogin";
    public static final String SESSION_GITHUB_SOURCE = "githubConnectionSource";
    public static final String SESSION_GITHUB_LINK_MODE = "githubLinkMode";
    public static final String SESSION_GITHUB_LINK_RETURN_TO = "githubLinkReturnTo";
    public static final String SESSION_GITHUB_FLASH_MESSAGE = "githubLinkFlashMessage";
    public static final String SESSION_GITHUB_FLASH_TYPE = "githubLinkFlashType";

    private static final String DEFAULT_RETURN_TO = "/profile-edit";

    private final OAuth2AuthorizedClientService authorizedClientService;
    private final ProfileUserRepository profileUserRepository;

    public GithubConnectionService(
            OAuth2AuthorizedClientService authorizedClientService,
            ProfileUserRepository profileUserRepository
    ) {
        this.authorizedClientService = authorizedClientService;
        this.profileUserRepository = profileUserRepository;
    }

    public void beginLinkFlow(HttpSession session, String returnTo) {
        session.setAttribute(SESSION_GITHUB_LINK_MODE, "Y");
        session.setAttribute(SESSION_GITHUB_LINK_RETURN_TO, sanitizeReturnTo(returnTo));
    }

    public boolean isLinkFlow(HttpSession session) {
        Object value = session.getAttribute(SESSION_GITHUB_LINK_MODE);
        return value != null && "Y".equalsIgnoreCase(String.valueOf(value));
    }

    public String consumeReturnTo(HttpSession session) {
        Object value = session.getAttribute(SESSION_GITHUB_LINK_RETURN_TO);
        clearLinkFlow(session);
        return sanitizeReturnTo(value == null ? null : value.toString());
    }

    public void clearLinkFlow(HttpSession session) {
        session.removeAttribute(SESSION_GITHUB_LINK_MODE);
        session.removeAttribute(SESSION_GITHUB_LINK_RETURN_TO);
    }

    public GithubAccess resolveGithubAccess(Authentication authentication, HttpSession session) {
        String sessionToken = stringValue(session.getAttribute(SESSION_GITHUB_ACCESS_TOKEN));
        String sessionLogin = stringValue(session.getAttribute(SESSION_GITHUB_LOGIN));
        String sessionSource = stringValue(session.getAttribute(SESSION_GITHUB_SOURCE));

        if (!sessionToken.isBlank()) {
            return new GithubAccess(true, sessionToken, sessionLogin, "linked".equalsIgnoreCase(sessionSource));
        }

        String loginUser = stringValue(session.getAttribute("loginUser"));
        if (!loginUser.isBlank()) {
            Optional<PersistedGithubLink> persistedLink = loadPersistedGithubLink(loginUser);
            if (persistedLink.isPresent()) {
                PersistedGithubLink link = persistedLink.get();
                storeLinkedGithub(session, link.accessToken(), link.githubLogin());
                return new GithubAccess(true, link.accessToken(), link.githubLogin(), true);
            }
        }

        if (authentication instanceof OAuth2AuthenticationToken token
                && "github".equalsIgnoreCase(token.getAuthorizedClientRegistrationId())) {
            OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                    token.getAuthorizedClientRegistrationId(),
                    token.getName()
            );

            if (client != null && client.getAccessToken() != null) {
                String login = sessionLogin;
                if (login.isBlank()) {
                    login = stringValue(token.getPrincipal().getAttribute("login"));
                }
                return new GithubAccess(true, client.getAccessToken().getTokenValue(), login, false);
            }
        }

        return new GithubAccess(false, "", "", false);
    }

    public void storeLinkedGithub(HttpSession session, String accessToken, String githubLogin) {
        session.setAttribute(SESSION_GITHUB_ACCESS_TOKEN, accessToken);
        session.setAttribute(SESSION_GITHUB_LOGIN, githubLogin);
        session.setAttribute(SESSION_GITHUB_SOURCE, "linked");
    }

    @Transactional
    public void persistLinkedGithub(String loginId, String accessToken, String githubLogin) {
        profileUserRepository.findByLoginId(loginId).ifPresent((user) -> {
            AuthKeyBundle bundle = AuthKeyBundle.parse(user.getAuthkey());
            user.setAuthkey(bundle.withGithub(githubLogin, accessToken).serialize());
        });
    }

    @Transactional
    public void clearPersistedLinkedGithub(String loginId) {
        profileUserRepository.findByLoginId(loginId).ifPresent((user) -> {
            AuthKeyBundle bundle = AuthKeyBundle.parse(user.getAuthkey());
            user.setAuthkey(bundle.withoutGithub().serialize());
        });
    }

    public void storeOauthGithubLogin(HttpSession session, String accessToken, String githubLogin) {
        session.setAttribute(SESSION_GITHUB_ACCESS_TOKEN, accessToken);
        session.setAttribute(SESSION_GITHUB_LOGIN, githubLogin);
        session.setAttribute(SESSION_GITHUB_SOURCE, "oauth-login");
    }

    public void clearLinkedGithub(HttpSession session) {
        session.removeAttribute(SESSION_GITHUB_ACCESS_TOKEN);
        session.removeAttribute(SESSION_GITHUB_LOGIN);
        session.removeAttribute(SESSION_GITHUB_SOURCE);
    }

    public void setFlashMessage(HttpSession session, String message, String type) {
        session.setAttribute(SESSION_GITHUB_FLASH_MESSAGE, message);
        session.setAttribute(SESSION_GITHUB_FLASH_TYPE, type);
    }

    public Map<String, String> consumeFlashMessage(HttpSession session) {
        Map<String, String> payload = new LinkedHashMap<>();

        Object message = session.getAttribute(SESSION_GITHUB_FLASH_MESSAGE);
        Object type = session.getAttribute(SESSION_GITHUB_FLASH_TYPE);
        session.removeAttribute(SESSION_GITHUB_FLASH_MESSAGE);
        session.removeAttribute(SESSION_GITHUB_FLASH_TYPE);

        if (message != null && !message.toString().isBlank()) {
            payload.put("message", message.toString());
            payload.put("type", type == null ? "info" : type.toString());
        }

        return payload;
    }

    private String sanitizeReturnTo(String returnTo) {
        if (returnTo == null || returnTo.isBlank()) {
            return DEFAULT_RETURN_TO;
        }
        if (!returnTo.startsWith("/") || returnTo.startsWith("//")) {
            return DEFAULT_RETURN_TO;
        }
        return returnTo;
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    public record GithubAccess(boolean available, String accessToken, String githubLogin, boolean sessionLinked) {
    }

    private Optional<PersistedGithubLink> loadPersistedGithubLink(String loginId) {
        return profileUserRepository.findByLoginId(loginId)
                .map(ProfileUser::getAuthkey)
                .map(AuthKeyBundle::parse)
                .flatMap((bundle) -> {
                    if (bundle.githubToken().isBlank() || bundle.githubLogin().isBlank()) {
                        return Optional.empty();
                    }
                    return Optional.of(new PersistedGithubLink(bundle.githubLogin(), bundle.githubToken()));
                });
    }

    private record PersistedGithubLink(String githubLogin, String accessToken) {
    }

    private record AuthKeyBundle(String baseValue, String githubLogin, String githubToken) {
        private static final String BASE_PREFIX = "base=";
        private static final String LOGIN_PREFIX = "gh_login=";
        private static final String TOKEN_PREFIX = "gh_token=";

        private static AuthKeyBundle parse(String rawAuthKey) {
            String raw = stringValue(rawAuthKey);
            if (raw.isBlank()) {
                return new AuthKeyBundle("", "", "");
            }

            if (!raw.startsWith(BASE_PREFIX)) {
                return new AuthKeyBundle(raw, "", "");
            }

            String base = "";
            String login = "";
            String token = "";

            String[] parts = raw.split("\\|");
            for (String part : parts) {
                if (part.startsWith(BASE_PREFIX)) {
                    base = decode(part.substring(BASE_PREFIX.length()));
                } else if (part.startsWith(LOGIN_PREFIX)) {
                    login = decode(part.substring(LOGIN_PREFIX.length()));
                } else if (part.startsWith(TOKEN_PREFIX)) {
                    token = decode(part.substring(TOKEN_PREFIX.length()));
                }
            }

            return new AuthKeyBundle(base, login, token);
        }

        private AuthKeyBundle withGithub(String login, String token) {
            return new AuthKeyBundle(baseValue, stringValue(login), stringValue(token));
        }

        private AuthKeyBundle withoutGithub() {
            return new AuthKeyBundle(baseValue, "", "");
        }

        private String serialize() {
            StringBuilder builder = new StringBuilder();
            builder.append(BASE_PREFIX).append(encode(baseValue));

            if (!githubLogin.isBlank()) {
                builder.append("|").append(LOGIN_PREFIX).append(encode(githubLogin));
            }

            if (!githubToken.isBlank()) {
                builder.append("|").append(TOKEN_PREFIX).append(encode(githubToken));
            }

            return builder.toString();
        }

        private static String encode(String value) {
            return URLEncoder.encode(stringValue(value), StandardCharsets.UTF_8);
        }

        private static String decode(String value) {
            return URLDecoder.decode(stringValue(value), StandardCharsets.UTF_8);
        }
    }
}
