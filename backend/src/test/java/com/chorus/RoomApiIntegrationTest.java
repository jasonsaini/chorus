package com.chorus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.chorus.domain.ChatMessage;
import com.chorus.domain.MessageType;
import com.chorus.service.RoomService;
import com.chorus.web.dto.CreateRoomRequest;
import com.chorus.web.dto.RoomDetailResponse;
import com.chorus.web.dto.RoomResponse;
import java.util.List;
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
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class RoomApiIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private RoomService roomService;

    @MockBean
    private ChatModel chatModel;

    @Test
    void createRoomThenGetHistoryAndParticipantsPersist() {
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("Mock Claude says hi.")))));

        ResponseEntity<RoomResponse> created =
                restTemplate.postForEntity("/api/rooms", new CreateRoomRequest("Jason"), RoomResponse.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody()).isNotNull();
        String roomId = created.getBody().roomId();

        roomService.addParticipant(roomId, "Jason");
        roomService.appendMessage(roomId, ChatMessage.of(MessageType.USER, "Jason", "Hello room"));

        ResponseEntity<RoomDetailResponse> detail =
                restTemplate.getForEntity("/api/rooms/{id}", RoomDetailResponse.class, roomId);
        assertThat(detail.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(detail.getBody()).isNotNull();
        assertThat(detail.getBody().participants()).containsExactly("Jason");
        assertThat(detail.getBody().messages()).hasSize(1);
        assertThat(detail.getBody().messages().getFirst().type()).isEqualTo(MessageType.USER);
        assertThat(detail.getBody().messages().getFirst().content()).isEqualTo("Hello room");
    }

    @Test
    void unknownRoomReturns404() {
        ResponseEntity<RoomDetailResponse> detail =
                restTemplate.getForEntity("/api/rooms/{id}", RoomDetailResponse.class, "does-not-exist");
        assertThat(detail.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
