package com.project.mvcgithublogin.service;

import com.project.mvcgithublogin.domain.JobPosting;
import com.project.mvcgithublogin.domain.User;
import com.project.mvcgithublogin.repository.BookmarkRepository;
import com.project.mvcgithublogin.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final UserRepository userRepository;

    public BookmarkService(BookmarkRepository bookmarkRepository, UserRepository userRepository) {
        this.bookmarkRepository = bookmarkRepository;
        this.userRepository = userRepository;
    }

    private Long getUserIdByLoginId(String loginId) {
        User user = userRepository.find(loginId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        return user.getUserId();
    }

    public void addBookmark(String loginId, Long postingId) {
        Long userId = getUserIdByLoginId(loginId);

        int exists = bookmarkRepository.existsBookmark(userId, postingId);
        if (exists > 0) {
            throw new IllegalArgumentException("이미 즐겨찾기한 공고입니다.");
        }

        bookmarkRepository.insertBookmark(userId, postingId);
    }

    public void removeBookmark(String loginId, Long postingId) {
        Long userId = getUserIdByLoginId(loginId);
        bookmarkRepository.deleteBookmark(userId, postingId);
    }

    public List<Long> getBookmarkedPostingIds(String loginId) {
        Long userId = getUserIdByLoginId(loginId);
        return bookmarkRepository.findBookmarkedPostingIds(userId);
    }

    public List<JobPosting> getBookmarkedJobs(String loginId) {
        Long userId = getUserIdByLoginId(loginId);
        return bookmarkRepository.findBookmarkedJobs(userId);
    }
}