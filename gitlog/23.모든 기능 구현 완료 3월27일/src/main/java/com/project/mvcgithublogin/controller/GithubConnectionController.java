package com.project.mvcgithublogin.controller;

import com.project.mvcgithublogin.service.GithubConnectionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class GithubConnectionController {

    private final GithubConnectionService githubConnectionService;

    public GithubConnectionController(GithubConnectionService githubConnectionService) {
        this.githubConnectionService = githubConnectionService;
    }

    @GetMapping("/github/connect")
    public ResponseEntity<Void> connectGithub(
            @RequestParam(value = "returnTo", required = false) String returnTo,
            HttpSession session
    ) {
        Object loginUser = session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", "/login")
                    .build();
        }

        githubConnectionService.beginLinkFlow(session, returnTo);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", "/oauth2/authorization/github")
                .build();
    }

    @DeleteMapping("/github/connect")
    public ResponseEntity<Map<String, Object>> disconnectGithub(Authentication authentication, HttpSession session) {
        GithubConnectionService.GithubAccess access = githubConnectionService.resolveGithubAccess(authentication, session);

        if (!access.available()) {
            return ResponseEntity.ok(Map.of(
                    "message", "연결된 GitHub 계정이 없습니다.",
                    "githubLinked", false
            ));
        }

        if (!access.sessionLinked()) {
            return ResponseEntity.badRequest().body(Map.of(
                "message", "GitHub 소셜 로그인 세션은 이 화면에서 연동 해제할 수 없습니다.",
                "githubLinked", true
            ));
        }

        Object loginUser = session.getAttribute("loginUser");
        if (loginUser != null) {
            githubConnectionService.clearPersistedLinkedGithub(loginUser.toString());
        }
        githubConnectionService.clearLinkedGithub(session);
        return ResponseEntity.ok(Map.of(
                "message", "GitHub 연동이 해제되었습니다.",
                "githubLinked", false
        ));
    }
}
