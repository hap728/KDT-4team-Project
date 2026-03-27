package com.project.mvcgithublogin.profile;

import com.project.mvcgithublogin.dto.ProfileUserProfileRequest;
import com.project.mvcgithublogin.service.GithubConnectionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class ProfileApiController {
    private final ProfileUserService profileUserService;
    private final GithubConnectionService githubConnectionService;

    public ProfileApiController(
            ProfileUserService profileUserService,
            GithubConnectionService githubConnectionService
    ) {
        this.profileUserService = profileUserService;
        this.githubConnectionService = githubConnectionService;
    }

    @PutMapping("/users/profile")
    public ResponseEntity<Map<String, Object>> updateProfile(@RequestBody ProfileUserProfileRequest request, HttpSession session) {
        Object loginUser = session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "로그인이 필요합니다."));
        }

        try {
            request.setId(loginUser.toString());
            ProfileUser updatedUser = profileUserService.updateProfile(request);
            List<String> savedStackNames = profileUserService.getUserStackNames(updatedUser.getId());

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("message", "프로필 수정이 완료되었습니다.");
            body.put("userNo", updatedUser.getUserNo());
            body.put("id", updatedUser.getId());
            body.put("nickname", updatedUser.getNickname());
            body.put("intro", updatedUser.getIntro());
            body.put("stackNames", savedStackNames);
            body.put("githubAgreeYn", updatedUser.getGithubAgreeYn());

            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/users/profile/github-consent")
    public ResponseEntity<Map<String, Object>> agreeGithubConsent(HttpSession session) {
        Object loginUser = session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "로그인이 필요합니다."));
        }

        try {
            ProfileUser user = profileUserService.agreeGithubAccess(loginUser.toString());
            return ResponseEntity.ok(Map.of(
                    "message", "GitHub 접근 동의를 저장했습니다.",
                    "githubAgreeYn", user.getGithubAgreeYn()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/users/profile")
    public ResponseEntity<Map<String, Object>> getProfile(HttpSession session, Authentication authentication) {
        Object loginUser = session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.ok(Map.of("loggedIn", false));
        }

        ProfileUser user = profileUserService.getByLoginId(loginUser.toString());
        List<String> stackNames = profileUserService.getUserStackNames(user.getId());
        GithubConnectionService.GithubAccess githubAccess =
                githubConnectionService.resolveGithubAccess(authentication, session);
        Map<String, String> flashPayload = githubConnectionService.consumeFlashMessage(session);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("loggedIn", true);
        body.put("id", user.getId());
        body.put("nickname", user.getNickname());
        body.put("intro", user.getIntro());
        body.put("stackNames", stackNames);
        body.put("githubAgreeYn", user.getGithubAgreeYn());
        body.put("githubLinked", githubAccess.available());
        body.put("githubConnectedLogin", githubAccess.githubLogin());
        body.put("githubSessionLinked", githubAccess.sessionLinked());
        body.put("githubConnectUrl", "/github/connect?returnTo=/profile-edit");
        body.put("githubLinkMessage", flashPayload.getOrDefault("message", ""));
        body.put("githubLinkMessageType", flashPayload.getOrDefault("type", ""));

        return ResponseEntity.ok(body);
    }
}
