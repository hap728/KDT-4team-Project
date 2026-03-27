package com.project.mvcgithublogin.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.mvcgithublogin.domain.TechStack;
import com.project.mvcgithublogin.dto.GithubStackCandidateDto;
import com.project.mvcgithublogin.dto.GithubRepositoryStackDto;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class GithubStackService {

    private static final String GITHUB_API_BASE = "https://api.github.com";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TechStackService techStackService;

    private static final Map<String, String> STACK_ALIAS_MAP = createAliasMap();

    public GithubStackService(TechStackService techStackService) {
        this.techStackService = techStackService;
    }

    public GithubUserProfile fetchAuthenticatedUserProfile(String accessToken, JsonNode oauthUser) {
        String login = text(oauthUser, "login");
        String name = text(oauthUser, "name");
        String email = text(oauthUser, "email");
        String githubId = text(oauthUser, "id");

        JsonNode userNode = oauthUser;
        if (login.isBlank() || githubId.isBlank() || email.isBlank()) {
            userNode = githubFetch("/user", accessToken);
            if (login.isBlank()) {
                login = text(userNode, "login");
            }
            if (name.isBlank()) {
                name = text(userNode, "name");
            }
            if (email.isBlank()) {
                email = text(userNode, "email");
            }
            if (githubId.isBlank()) {
                githubId = text(userNode, "id");
            }
        }

        if (email.isBlank()) {
            email = fetchPrimaryEmail(accessToken);
        }

        String loginId = !email.isBlank() ? email.trim().toLowerCase(Locale.ROOT) : "github_" + login;
        String nickname = !name.isBlank() ? name.trim() : login;

        return new GithubUserProfile(loginId, nickname, githubId, login);
    }

    public List<GithubStackCandidateDto> buildStackCandidates(String accessToken) {
        List<GithubRepositoryStackDto> repositories = buildRepositoryStacks(accessToken);
        Map<String, CandidateAccumulator> candidateMap = new LinkedHashMap<>();

        for (GithubRepositoryStackDto repo : repositories) {
            if (repo.getStacks() == null) {
                continue;
            }
            for (String stackName : repo.getStacks()) {
                candidateMap.computeIfAbsent(stackName, CandidateAccumulator::new)
                        .add("repo", repo.getFullName());
            }
        }

        return candidateMap.values().stream()
                .map(candidate -> new GithubStackCandidateDto(
                        candidate.name,
                        candidate.repositories.size(),
                        new ArrayList<>(candidate.repositories),
                        candidate.getSortedSourceTypes()
                ))
                .sorted(Comparator
                        .comparingInt(GithubStackCandidateDto::getRepoCount).reversed()
                        .thenComparing(GithubStackCandidateDto::getName))
                .toList();
    }

    public List<GithubRepositoryStackDto> buildRepositoryStacks(String accessToken) {
        Map<String, String> allowedStacks = createAllowedStackMap();
        List<JsonNode> repositories = fetchAllRepositories(accessToken);
        List<GithubRepositoryStackDto> results = new ArrayList<>();

        for (JsonNode repo : repositories) {
            String fullName = text(repo, "full_name");
            String languagesUrl = text(repo, "languages_url");
            Set<String> repoStacks = new LinkedHashSet<>();

            if (!languagesUrl.isBlank()) {
                JsonNode languagesNode = githubFetchAbsolute(languagesUrl, accessToken);
                languagesNode.fieldNames().forEachRemaining((language) -> {
                    String allowed = resolveAllowedStack(allowedStacks, language);
                    if (allowed != null) {
                        repoStacks.add(allowed);
                    }
                });
            }

            JsonNode topicsNode = repo.path("topics");
            if (topicsNode.isArray()) {
                for (JsonNode topicNode : topicsNode) {
                    String allowed = resolveAllowedStack(allowedStacks, topicNode.asText(""));
                    if (allowed != null) {
                        repoStacks.add(allowed);
                    }
                }
            }

            if (repoStacks.isEmpty()) {
                continue;
            }

            results.add(new GithubRepositoryStackDto(
                    repo.path("id").asLong(),
                    text(repo, "name"),
                    fullName,
                    text(repo, "html_url"),
                    text(repo, "description"),
                    repo.path("private").asBoolean(false),
                    repo.path("stargazers_count").asInt(0),
                    new ArrayList<>(repoStacks)
            ));
        }

        results.sort(Comparator.comparing(GithubRepositoryStackDto::getFullName, String.CASE_INSENSITIVE_ORDER));
        return results;
    }

    private List<JsonNode> fetchAllRepositories(String accessToken) {
        List<JsonNode> repos = new ArrayList<>();
        int page = 1;

        while (true) {
            JsonNode batch = githubFetch("/user/repos?per_page=100&page=" + page + "&sort=updated", accessToken);
            if (!batch.isArray() || batch.isEmpty()) {
                break;
            }

            batch.forEach(repos::add);
            if (batch.size() < 100) {
                break;
            }
            page += 1;
        }

        return repos;
    }

    private String fetchPrimaryEmail(String accessToken) {
        JsonNode emails = githubFetch("/user/emails", accessToken);
        if (!emails.isArray()) {
            return "";
        }

        for (JsonNode emailNode : emails) {
            boolean primary = emailNode.path("primary").asBoolean(false);
            boolean verified = emailNode.path("verified").asBoolean(false);
            if (primary && verified) {
                return text(emailNode, "email");
            }
        }

        for (JsonNode emailNode : emails) {
            String email = text(emailNode, "email");
            if (!email.isBlank()) {
                return email;
            }
        }

        return "";
    }

    private void upsertCandidate(
            Map<String, CandidateAccumulator> candidateMap,
            Map<String, String> allowedStacks,
            String rawValue,
            String sourceType,
            String repository
    ) {
        String normalized = normalizeStackName(rawValue);
        if (normalized == null) {
            return;
        }

        String canonicalName = allowedStacks.get(normalizeKey(normalized));
        if (canonicalName == null) {
            return;
        }

        candidateMap.computeIfAbsent(canonicalName, CandidateAccumulator::new)
                .add(sourceType, repository);
    }

    private Map<String, String> createAllowedStackMap() {
        Map<String, String> allowedStacks = new HashMap<>();
        for (TechStack stack : techStackService.findAllStacks()) {
            if (stack.getStackName() == null || stack.getStackName().isBlank()) {
                continue;
            }
            allowedStacks.put(normalizeKey(stack.getStackName()), stack.getStackName().trim());
        }
        return allowedStacks;
    }

    private String resolveAllowedStack(Map<String, String> allowedStacks, String rawValue) {
        String normalized = normalizeStackName(rawValue);
        if (normalized == null) {
            return null;
        }
        return allowedStacks.get(normalizeKey(normalized));
    }

    private JsonNode githubFetch(String path, String accessToken) {
        return githubFetchAbsolute(GITHUB_API_BASE + path, accessToken);
    }

    private JsonNode githubFetchAbsolute(String url, String accessToken) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .header("Accept", "application/vnd.github+json")
                    .header("Authorization", "Bearer " + accessToken)
                    .header("User-Agent", "trendbridge-github-stack")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("GitHub API 요청 실패: " + response.statusCode());
            }

            return objectMapper.readTree(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("GitHub 요청 중 인터럽트가 발생했습니다.", e);
        } catch (IOException e) {
            throw new IllegalStateException("GitHub 응답을 읽는 중 오류가 발생했습니다.", e);
        }
    }

    private String normalizeStackName(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        if (trimmed.isBlank()) {
            return null;
        }

        String normalizedKey = normalizeKey(trimmed);
        return STACK_ALIAS_MAP.getOrDefault(normalizedKey, trimmed);
    }

    private String normalizeKey(String value) {
        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replace("_", "-")
                .replace(" ", "");
    }

    private static String text(JsonNode node, String fieldName) {
        return node == null ? "" : node.path(fieldName).asText("");
    }

    private static Map<String, String> createAliasMap() {
        Map<String, String> map = new HashMap<>();
        map.put("c#", "C#");
        map.put("c++", "C++");
        map.put("cplusplus", "C++");
        map.put("objective-c", "Objective-C");
        map.put("golang", "Go");
        map.put("node", "Node.js");
        map.put("nodejs", "Node.js");
        map.put("reactjs", "React");
        map.put("nextjs", "Next.js");
        map.put("next-js", "Next.js");
        map.put("nuxtjs", "Nuxt.js");
        map.put("vuejs", "Vue.js");
        map.put("expressjs", "Express");
        map.put("springboot", "Spring Boot");
        map.put("spring-boot", "Spring Boot");
        map.put("tailwindcss", "Tailwind CSS");
        map.put("tailwind", "Tailwind CSS");
        map.put("postgres", "PostgreSQL");
        map.put("postgresql", "PostgreSQL");
        map.put("mysql", "MySQL");
        map.put("mariadb", "MariaDB");
        map.put("mongodb", "MongoDB");
        map.put("typescript", "TypeScript");
        map.put("javascript", "JavaScript");
        map.put("python", "Python");
        map.put("java", "Java");
        map.put("kotlin", "Kotlin");
        map.put("swift", "Swift");
        map.put("flutter", "Flutter");
        map.put("dart", "Dart");
        map.put("docker", "Docker");
        map.put("kubernetes", "Kubernetes");
        map.put("aws", "AWS");
        map.put("gcp", "GCP");
        map.put("azure", "Azure");
        return map;
    }

    public record GithubUserProfile(String loginId, String nickname, String githubId, String githubLogin) {
    }

    private static class CandidateAccumulator {
        private final String name;
        private final Set<String> repositories = new LinkedHashSet<>();
        private final Set<String> sourceTypes = new LinkedHashSet<>();

        private CandidateAccumulator(String name) {
            this.name = name;
        }

        private CandidateAccumulator add(String sourceType, String repository) {
            if (repository != null && !repository.isBlank()) {
                repositories.add(repository);
            }
            if (sourceType != null && !sourceType.isBlank()) {
                sourceTypes.add(sourceType);
            }
            return this;
        }

        private List<String> getSortedSourceTypes() {
            return sourceTypes.stream().sorted().toList();
        }
    }
}
