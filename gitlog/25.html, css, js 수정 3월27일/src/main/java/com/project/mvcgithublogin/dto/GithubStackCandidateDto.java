package com.project.mvcgithublogin.dto;

import java.util.List;

public class GithubStackCandidateDto {
    private String name;
    private int repoCount;
    private List<String> repositories;
    private List<String> sourceTypes;

    public GithubStackCandidateDto() {
    }

    public GithubStackCandidateDto(String name, int repoCount, List<String> repositories, List<String> sourceTypes) {
        this.name = name;
        this.repoCount = repoCount;
        this.repositories = repositories;
        this.sourceTypes = sourceTypes;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getRepoCount() {
        return repoCount;
    }

    public void setRepoCount(int repoCount) {
        this.repoCount = repoCount;
    }

    public List<String> getRepositories() {
        return repositories;
    }

    public void setRepositories(List<String> repositories) {
        this.repositories = repositories;
    }

    public List<String> getSourceTypes() {
        return sourceTypes;
    }

    public void setSourceTypes(List<String> sourceTypes) {
        this.sourceTypes = sourceTypes;
    }
}
