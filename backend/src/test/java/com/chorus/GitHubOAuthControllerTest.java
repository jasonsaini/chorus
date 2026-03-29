package com.chorus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chorus.service.GitHubService;
import com.chorus.service.RoomService;
import com.chorus.web.dto.CreateRoomRequest;
import com.chorus.web.dto.RepoInfo;
import com.chorus.web.dto.RoomResponse;
import java.util.List;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GitHubOAuthControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private RoomService roomService;

    @MockBean
    private GitHubService gitHubService;

    @MockBean
    private ChatModel chatModel;

    @BeforeEach
    void disableRedirects() {
        var httpClient = HttpClients.custom().disableRedirectHandling().build();
        restTemplate.getRestTemplate().setRequestFactory(
                new HttpComponentsClientHttpRequestFactory(httpClient));
    }

    @Test
    void authorizeRedirectsToGitHubOAuthUrl() {
        var room = createRoom("Alice");
        when(gitHubService.getAuthorizationUrl(room.roomId()))
                .thenReturn("https://github.com/login/oauth/authorize?client_id=test&state=" + room.roomId());

        var response = restTemplate.getForEntity(
                "/api/auth/github?roomId={roomId}", Void.class, room.roomId());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getLocation().toString())
                .startsWith("https://github.com/login/oauth/authorize");
    }

    @Test
    void authorizeReturns404ForUnknownRoom() {
        var response = restTemplate.getForEntity(
                "/api/auth/github?roomId=nonexistent", Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void callbackExchangesCodeStoresTokenAndRedirectsToFrontend() {
        var room = createRoom("Bob");
        when(gitHubService.exchangeCodeForToken("auth-code-123")).thenReturn("gho_token456");

        var response = restTemplate.getForEntity(
                "/api/auth/github/callback?code={code}&state={state}",
                Void.class, "auth-code-123", room.roomId());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getLocation().toString())
                .contains("/rooms/" + room.roomId())
                .contains("repoLinked=true");
        assertThat(roomService.getGithubToken(room.roomId())).hasValue("gho_token456");
    }

    @Test
    void callbackRedirectsToErrorUrlOnFailure() {
        var room = createRoom("Carol");
        when(gitHubService.exchangeCodeForToken(any()))
                .thenThrow(new RuntimeException("GitHub error"));

        var response = restTemplate.getForEntity(
                "/api/auth/github/callback?code={code}&state={state}",
                Void.class, "bad-code", room.roomId());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getLocation().toString())
                .contains("error=github_auth_failed");
    }

    @Test
    void listReposReturnsReposForLinkedToken() {
        var room = createRoom("Dave");
        roomService.storeGithubToken(room.roomId(), "gho_valid_token");
        when(gitHubService.listRepos("gho_valid_token")).thenReturn(List.of(
                new RepoInfo("dave/project-a", false, "main"),
                new RepoInfo("dave/project-b", true, "develop")));

        var response = restTemplate.getForEntity(
                "/api/auth/github/repos?roomId={roomId}", RepoInfo[].class, room.roomId());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody()[0].fullName()).isEqualTo("dave/project-a");
        assertThat(response.getBody()[1].isPrivate()).isTrue();
    }

    @Test
    void listReposReturns401WhenNoTokenLinked() {
        var room = createRoom("Eve");

        var response = restTemplate.getForEntity(
                "/api/auth/github/repos?roomId={roomId}", Void.class, room.roomId());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ---- helpers ----

    private RoomResponse createRoom(String createdBy) {
        return restTemplate.postForEntity(
                        "/api/rooms", new CreateRoomRequest(createdBy), RoomResponse.class)
                .getBody();
    }
}
