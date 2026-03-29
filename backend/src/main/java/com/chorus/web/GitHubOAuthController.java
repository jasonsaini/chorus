package com.chorus.web;

import com.chorus.config.ChorusProperties;
import com.chorus.service.GitHubService;
import com.chorus.service.RoomService;
import com.chorus.web.dto.RepoInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/github")
@Tag(name = "GitHub OAuth", description = "GitHub OAuth flow for linking repos to rooms")
public class GitHubOAuthController {

    private final GitHubService gitHubService;
    private final RoomService roomService;
    private final ChorusProperties properties;

    public GitHubOAuthController(
            GitHubService gitHubService,
            RoomService roomService,
            ChorusProperties properties) {
        this.gitHubService = gitHubService;
        this.roomService = roomService;
        this.properties = properties;
    }

    @GetMapping
    @Operation(summary = "Redirect to GitHub OAuth — pass roomId as state")
    public ResponseEntity<Void> authorize(@RequestParam String roomId) {
        if (roomService.getRoom(roomId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(gitHubService.getAuthorizationUrl(roomId)))
                .build();
    }

    @GetMapping("/callback")
    @Operation(summary = "GitHub OAuth callback — exchanges code, stores token, redirects to frontend")
    public ResponseEntity<Void> callback(@RequestParam String code, @RequestParam String state) {
        String roomId = state;
        String frontendUrl = properties.getGithub().getFrontendUrl();
        try {
            String token = gitHubService.exchangeCodeForToken(code);
            roomService.storeGithubToken(roomId, token);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(frontendUrl + "/rooms/" + roomId + "?repoLinked=true"))
                    .build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(frontendUrl + "/rooms/" + roomId + "?error=github_auth_failed"))
                    .build();
        }
    }

    @GetMapping("/repos")
    @Operation(summary = "List repos accessible to the GitHub token linked to this room")
    public ResponseEntity<List<RepoInfo>> listRepos(@RequestParam String roomId) {
        return roomService.getGithubToken(roomId)
                .map(token -> ResponseEntity.ok(gitHubService.listRepos(token)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }
}
