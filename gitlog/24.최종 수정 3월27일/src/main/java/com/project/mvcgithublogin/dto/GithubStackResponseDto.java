package com.project.mvcgithublogin.dto;

import java.util.List;

public class GithubStackResponseDto {
    private boolean available;
    private String message;
    private List<GithubStackCandidateDto> candidates;
    private List<GithubRepositoryStackDto> repositories;

    public GithubStackResponseDto() {
    }

    public GithubStackResponseDto(boolean available, String message, List<GithubStackCandidateDto> candidates, List<GithubRepositoryStackDto> repositories) {
        this.available = available;
        this.message = message;
        this.candidates = candidates;
        this.repositories = repositories;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<GithubStackCandidateDto> getCandidates() {
        return candidates;
    }

    public void setCandidates(List<GithubStackCandidateDto> candidates) {
        this.candidates = candidates;
    }

    public List<GithubRepositoryStackDto> getRepositories() {
        return repositories;
    }

    public void setRepositories(List<GithubRepositoryStackDto> repositories) {
        this.repositories = repositories;
    }
}
