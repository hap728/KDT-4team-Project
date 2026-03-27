package com.project.mvcgithublogin.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.mvcgithublogin.domain.User;
import com.project.mvcgithublogin.dto.CreateUserRequest;
import com.project.mvcgithublogin.dto.LoginRequest;
import com.project.mvcgithublogin.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void signup(CreateUserRequest request) {
        String id = request.getId() == null ? null : request.getId().trim().toLowerCase();
        String pw = request.getPw() == null ? null : request.getPw().trim();
        String nickname = request.getNickname() == null ? null : request.getNickname().trim();

        validateLocalSignup(id, pw, nickname);
        ensureEmailNotTaken(id);

        User user = new User();
        user.setId(id);
        user.setPw(passwordEncoder.encode(pw));
        user.setNickname(nickname);
        user.setLoginType("LOCAL");
        user.setAuthKey("LOCAL_USER");
        user.setGithubAgreeYn("N");
        userRepository.save(user);
    }

    public User login(LoginRequest request) {
        String id = request.getId() == null ? null : request.getId().trim().toLowerCase();
        String pw = request.getPw() == null ? null : request.getPw().trim();

        if (id == null || id.isBlank() || pw == null || pw.isBlank()) {
            throw new IllegalArgumentException("이메일과 비밀번호를 입력해주세요.");
        }

        User user = userRepository.find(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        if ("GOOGLE".equalsIgnoreCase(user.getLoginType())) {
            throw new IllegalArgumentException("소셜 로그인으로 가입한 계정입니다. Google 로그인을 사용해주세요.");
        }

        if (!passwordEncoder.matches(pw, user.getPw())) {
            throw new IllegalArgumentException("비밀번호가 올바르지 않습니다.");
        }
        return user;
    }

    public User loginWithGoogle(String credential, String expectedAudience) {
        if (credential == null || credential.isBlank()) {
            throw new IllegalArgumentException("Google 인증 정보가 비어 있습니다.");
        }
        if (expectedAudience == null || expectedAudience.isBlank()) {
            throw new IllegalStateException("app.google.client-id 설정이 필요합니다.");
        }

        GoogleTokenInfo tokenInfo = verifyGoogleToken(credential, expectedAudience);
        return findOrCreateGoogleUser(tokenInfo.email(), tokenInfo.nickname(), tokenInfo.subject());
    }

    public User findOrCreateGoogleUser(String email, String nickname, String subject) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        String normalizedNickname = nickname == null ? "" : nickname.trim();
        String normalizedSubject = subject == null ? "" : subject.trim();

        if (normalizedEmail.isBlank()) {
            throw new IllegalArgumentException("검증된 Google 이메일이 필요합니다.");
        }

        Optional<User> existingUser = userRepository.find(normalizedEmail);
        if (existingUser.isPresent()) {
            return existingUser.get();
        }

        User user = new User();
        user.setId(normalizedEmail);
        user.setPw(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setNickname(normalizedNickname.isBlank() ? normalizedEmail.substring(0, normalizedEmail.indexOf("@")) : normalizedNickname);
        user.setLoginType("GOOGLE");
        user.setAuthKey(normalizedSubject.isBlank() ? "GOOGLE_USER" : normalizedSubject);
        user.setGithubAgreeYn("N");
        userRepository.save(user);

        return userRepository.find(normalizedEmail)
                .orElseThrow(() -> new IllegalStateException("Google 로그인 사용자를 저장하지 못했습니다."));
    }

    public User findOrCreateGithubUser(String loginId, String nickname, String githubId) {
        String normalizedLoginId = loginId == null ? "" : loginId.trim().toLowerCase();
        String normalizedNickname = nickname == null ? "" : nickname.trim();
        String normalizedGithubId = githubId == null ? "" : githubId.trim();

        if (normalizedLoginId.isBlank()) {
            throw new IllegalArgumentException("GitHub 로그인 아이디를 확인할 수 없습니다.");
        }

        Optional<User> existingUser = userRepository.find(normalizedLoginId);
        if (existingUser.isPresent()) {
            return existingUser.get();
        }

        User user = new User();
        user.setId(normalizedLoginId);
        user.setPw(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setNickname(normalizedNickname.isBlank() ? normalizedLoginId : normalizedNickname);
        user.setLoginType("GITHUB");
        user.setAuthKey(normalizedGithubId.isBlank() ? "GITHUB_USER" : normalizedGithubId);
        user.setGithubAgreeYn("N");
        userRepository.save(user);

        return userRepository.find(normalizedLoginId)
                .orElseThrow(() -> new IllegalStateException("GitHub 로그인 사용자를 저장하지 못했습니다."));
    }

    private void validateLocalSignup(String id, String pw, String nickname) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("이메일을 입력해주세요.");
        }
        if (pw == null || pw.isBlank()) {
            throw new IllegalArgumentException("비밀번호를 입력해주세요.");
        }
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("닉네임을 입력해주세요.");
        }
    }

    private void ensureEmailNotTaken(String id) {
        if (userRepository.find(id).isPresent()) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }
    }

    private GoogleTokenInfo verifyGoogleToken(String credential, String expectedAudience) {
        try {
            String encodedCredential = URLEncoder.encode(credential, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://oauth2.googleapis.com/tokeninfo?id_token=" + encodedCredential))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalArgumentException("Google 토큰 검증에 실패했습니다.");
            }

            JsonNode json = objectMapper.readTree(response.body());
            String audience = json.path("aud").asText("");
            String email = json.path("email").asText("").trim().toLowerCase();
            boolean emailVerified = Boolean.parseBoolean(json.path("email_verified").asText("false"));
            String subject = json.path("sub").asText("");
            String name = json.path("name").asText("").trim();

            if (!expectedAudience.equals(audience)) {
                throw new IllegalArgumentException("허용되지 않은 Google 클라이언트입니다.");
            }
            if (!emailVerified || email.isBlank()) {
                throw new IllegalArgumentException("검증된 Google 이메일이 필요합니다.");
            }

            String resolvedNickname = name.isBlank() ? email.substring(0, email.indexOf("@")) : name;
            return new GoogleTokenInfo(email, resolvedNickname, subject);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Google 로그인 검증 중 인터럽트가 발생했습니다.", e);
        } catch (IOException e) {
            throw new IllegalStateException("Google 로그인 검증 중 오류가 발생했습니다.", e);
        }
    }

    private record GoogleTokenInfo(String email, String nickname, String subject) {
    }
}
