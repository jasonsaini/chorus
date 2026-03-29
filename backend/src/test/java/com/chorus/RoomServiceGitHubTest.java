package com.chorus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.chorus.service.RoomService;
import com.chorus.web.dto.CreateRoomRequest;
import com.chorus.web.dto.RoomDetailResponse;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class RoomServiceGitHubTest {

    @Autowired
    private RoomService roomService;

    @Autowired
    private TestRestTemplate restTemplate;

    @MockBean
    private ChatModel chatModel;

    @Test
    void storeGithubTokenAndRetrieveIt() {
        var created = restTemplate.postForEntity(
                "/api/rooms", new CreateRoomRequest("Alice"), com.chorus.web.dto.RoomResponse.class);
        String roomId = created.getBody().roomId();

        roomService.storeGithubToken(roomId, "gho_testtoken123");

        assertThat(roomService.getGithubToken(roomId)).hasValue("gho_testtoken123");
    }

    @Test
    void linkRepoSetsRepoAndBranchOnRoom() {
        var created = restTemplate.postForEntity(
                "/api/rooms", new CreateRoomRequest("Bob"), com.chorus.web.dto.RoomResponse.class);
        String roomId = created.getBody().roomId();

        roomService.linkRepo(roomId, "owner/my-repo", "develop");

        var detail = restTemplate.getForEntity(
                "/api/rooms/{id}", RoomDetailResponse.class, roomId);
        assertThat(detail.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(detail.getBody().linkedRepo()).isEqualTo("owner/my-repo");
        assertThat(detail.getBody().linkedBranch()).isEqualTo("develop");
    }

    @Test
    void linkRepoDefaultsToBranchMain() {
        var created = restTemplate.postForEntity(
                "/api/rooms", new CreateRoomRequest("Carol"), com.chorus.web.dto.RoomResponse.class);
        String roomId = created.getBody().roomId();

        roomService.linkRepo(roomId, "owner/my-repo", null);

        var detail = restTemplate.getForEntity(
                "/api/rooms/{id}", RoomDetailResponse.class, roomId);
        assertThat(detail.getBody().linkedBranch()).isEqualTo("main");
    }

    @Test
    void getGithubTokenReturnsEmptyWhenNotSet() {
        var created = restTemplate.postForEntity(
                "/api/rooms", new CreateRoomRequest("Dave"), com.chorus.web.dto.RoomResponse.class);
        String roomId = created.getBody().roomId();

        assertThat(roomService.getGithubToken(roomId)).isEmpty();
    }

    @Test
    void roomDetailResponseContainsNullLinkedRepoWhenNotLinked() {
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("hi")))));

        var created = restTemplate.postForEntity(
                "/api/rooms", new CreateRoomRequest("Eve"), com.chorus.web.dto.RoomResponse.class);
        String roomId = created.getBody().roomId();

        var detail = restTemplate.getForEntity(
                "/api/rooms/{id}", RoomDetailResponse.class, roomId);
        assertThat(detail.getBody().linkedRepo()).isNull();
        assertThat(detail.getBody().linkedBranch()).isNull();
    }
}
