package com.project.mvcgithublogin.dto;

import java.util.List;

public class GithubRepositoryStackDto {
    private long id;
    private String name;
    private String fullName;
    private String url;
    private String description;
    private boolean isPrivate;
    private int stars;
    private List<String> stacks;

    public GithubRepositoryStackDto() {
    }

    public GithubRepositoryStackDto(long id, String name, String fullName, String url, String description, boolean isPrivate, int stars, List<String> stacks) {
        this.id = id;
        this.name = name;
        this.fullName = fullName;
        this.url = url;
        this.description = description;
        this.isPrivate = isPrivate;
        this.stars = stars;
        this.stacks = stacks;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public void setPrivate(boolean aPrivate) {
        isPrivate = aPrivate;
    }

    public int getStars() {
        return stars;
    }

    public void setStars(int stars) {
        this.stars = stars;
    }

    public List<String> getStacks() {
        return stacks;
    }

    public void setStacks(List<String> stacks) {
        this.stacks = stacks;
    }
}
