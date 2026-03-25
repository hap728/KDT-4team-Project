package com.project.mvcgithublogin.controller;

import com.project.mvcgithublogin.domain.JobPosting;
import com.project.mvcgithublogin.service.BookmarkService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookmarks")
public class BookmarkApiController {

    private final BookmarkService bookmarkService;

    public BookmarkApiController(BookmarkService bookmarkService) {
        this.bookmarkService = bookmarkService;
    }

    @PostMapping("/{postingId}")
    public ResponseEntity<Map<String, Object>> addBookmark(@PathVariable Long postingId, HttpSession session) {
        Object loginUser = session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "로그인 후 이용 가능합니다."));
        }

        try {
            bookmarkService.addBookmark(loginUser.toString(), postingId);
            return ResponseEntity.ok(Map.of("message", "즐겨찾기 추가 완료"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{postingId}")
    public ResponseEntity<Map<String, Object>> removeBookmark(@PathVariable Long postingId, HttpSession session) {
        Object loginUser = session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "로그인 후 이용 가능합니다."));
        }

        bookmarkService.removeBookmark(loginUser.toString(), postingId);
        return ResponseEntity.ok(Map.of("message", "즐겨찾기 해제 완료"));
    }

    @GetMapping("/ids")
    public ResponseEntity<?> getBookmarkedPostingIds(HttpSession session) {
        Object loginUser = session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.ok(List.of());
        }

        List<Long> bookmarkedIds = bookmarkService.getBookmarkedPostingIds(loginUser.toString());
        return ResponseEntity.ok(bookmarkedIds);
    }

    @GetMapping
    public ResponseEntity<?> getBookmarkedJobs(HttpSession session) {
        Object loginUser = session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "로그인 후 이용 가능합니다."));
        }

        List<JobPosting> jobs = bookmarkService.getBookmarkedJobs(loginUser.toString());
        return ResponseEntity.ok(jobs);
    }


}