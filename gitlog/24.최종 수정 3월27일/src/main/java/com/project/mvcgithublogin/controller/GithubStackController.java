package com.project.mvcgithublogin.controller;

import com.project.mvcgithublogin.dto.GithubStackResponseDto;
import com.project.mvcgithublogin.service.GithubConnectionService;
import com.project.mvcgithublogin.service.GithubStackService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class GithubStackController {

    private final GithubConnectionService githubConnectionService;
    private final GithubStackService githubStackService;

    public GithubStackController(
            GithubConnectionService githubConnectionService,
            GithubStackService githubStackService
    ) {
        this.githubConnectionService = githubConnectionService;
        this.githubStackService = githubStackService;
    }

    @GetMapping("/api/github/stacks")
    public GithubStackResponseDto githubStacks(Authentication authentication, HttpSession session) {
        GithubConnectionService.GithubAccess access =
                githubConnectionService.resolveGithubAccess(authentication, session);

        if (!access.available()) {
            return new GithubStackResponseDto(
                    false,
                    "GitHub 연동 후 다시 시도해 주세요.",
                    List.of(),
                    List.of()
            );
        }

        try {
            return new GithubStackResponseDto(
                    true,
                    access.sessionLinked()
                            ? "연동된 GitHub 레포지토리에서 기술 스택 후보를 불러왔습니다."
                            : "GitHub 로그인 계정의 레포지토리에서 기술 스택 후보를 불러왔습니다.",
                    githubStackService.buildStackCandidates(access.accessToken()),
                    githubStackService.buildRepositoryStacks(access.accessToken())
            );
        } catch (IllegalStateException e) {
            return new GithubStackResponseDto(false, e.getMessage(), List.of(), List.of());
        }
    }
}
