package com.project.mvcgithublogin.domain;

public class Bookmark {
    private Long bookmarkId;
    private Long userId;
    private Long postingId;
    private String bookmarkedAt;

    public Long getBookmarkId() {
        return bookmarkId;
    }

    public void setBookmarkId(Long bookmarkId) {
        this.bookmarkId = bookmarkId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getPostingId() {
        return postingId;
    }

    public void setPostingId(Long postingId) {
        this.postingId = postingId;
    }

    public String getBookmarkedAt() {
        return bookmarkedAt;
    }

    public void setBookmarkedAt(String bookmarkedAt) {
        this.bookmarkedAt = bookmarkedAt;
    }
}