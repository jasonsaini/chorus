package com.chorus.service;

import com.chorus.config.ChorusProperties;
import com.chorus.web.dto.RepoInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class GitHubService {

    private static final String GITHUB_API = "https://api.github.com";
    private static final String OAUTH_AUTHORIZE = "https://github.com/login/oauth/authorize";
    private static final String OAUTH_TOKEN = "https://github.com/login/oauth/access_token";

    private final ChorusProperties properties;
    private final RestClient restClient;

    public GitHubService(ChorusProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .defaultHeader(HttpHeaders.USER_AGENT, "chorus-app")
                .build();
    }

    public String getAuthorizationUrl(String roomId) {
        return UriComponentsBuilder.fromHttpUrl(OAUTH_AUTHORIZE)
                .queryParam("client_id", properties.getGithub().getClientId())
                .queryParam("scope", "repo")
                .queryParam("state", roomId)
                .toUriString();
    }

    public String exchangeCodeForToken(String code) {
        var response = restClient.post()
                .uri(OAUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new TokenRequest(
                        properties.getGithub().getClientId(),
                        properties.getGithub().getClientSecret(),
                        code))
                .retrieve()
                .body(TokenResponse.class);
        if (response == null || response.accessToken() == null) {
            throw new IllegalStateException("GitHub token exchange returned no access token");
        }
        return response.accessToken();
    }

    public List<RepoInfo> listRepos(String accessToken) {
        GitHubRepo[] repos = restClient.get()
                .uri(GITHUB_API + "/user/repos?sort=updated&per_page=50")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(GitHubRepo[].class);
        if (repos == null) return List.of();
        return Arrays.stream(repos)
                .map(r -> new RepoInfo(r.fullName(), r.isPrivate(), r.defaultBranch()))
                .toList();
    }

    public Optional<String> fetchContextFile(String accessToken, String repoFullName, String branch) {
        try {
            var content = restClient.get()
                    .uri(GITHUB_API + "/repos/{repo}/contents/.context.md?ref={branch}",
                            repoFullName, branch)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(GitHubContent.class);
            if (content == null || content.content() == null) return Optional.empty();
            String decoded = new String(Base64.getMimeDecoder().decode(content.content()));
            return Optional.of(decoded);
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }

    // ---- internal deserialization types ----

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TokenRequest(
            @JsonProperty("client_id") String clientId,
            @JsonProperty("client_secret") String clientSecret,
            String code) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TokenResponse(@JsonProperty("access_token") String accessToken) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GitHubRepo(
            @JsonProperty("full_name") String fullName,
            @JsonProperty("private") boolean isPrivate,
            @JsonProperty("default_branch") String defaultBranch) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GitHubContent(String content, String encoding) {}
}
