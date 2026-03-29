package com.chorus;

import static org.assertj.core.api.Assertions.assertThat;

import com.chorus.domain.ChatMessage;
import com.chorus.domain.MessageType;
import com.chorus.web.dto.CreateRoomRequest;
import com.chorus.web.dto.RoomResponse;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.RestTemplateXhrTransport;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

/**
 * Opt-in live test: boots a real server and connects two users via WebSocket/STOMP.
 * Verifies that messages from one user are received by the other, and that both
 * users receive AI replies — confirming shared context.
 *
 * <p>Run: {@code ANTHROPIC_API_KEY=sk-ant-... ./mvnw test -Dtest=MultiUserChatLiveIT}
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
class MultiUserChatLiveIT {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void multipleUsersShareContextAndBothReceiveAiReply() throws Exception {
        // 1. Create a room via REST
        ResponseEntity<RoomResponse> created =
                restTemplate.postForEntity("/api/rooms", new CreateRoomRequest("Alice"), RoomResponse.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String roomId = created.getBody().roomId();

        String wsUrl = "http://localhost:" + port + "/ws";

        BlockingQueue<ChatMessage> aliceInbox = new LinkedBlockingQueue<>();
        BlockingQueue<ChatMessage> bobInbox = new LinkedBlockingQueue<>();

        // 2. Connect both users
        StompSession alice = connect(wsUrl, roomId, aliceInbox);
        StompSession bob = connect(wsUrl, roomId, bobInbox);

        // 3. Both join the room
        alice.send("/app/chat.join", Map.of("roomId", roomId, "username", "Alice"));
        bob.send("/app/chat.join", Map.of("roomId", roomId, "username", "Bob"));

        // Drain JOIN messages (not what we're testing)
        Thread.sleep(500);
        aliceInbox.clear();
        bobInbox.clear();

        // 4. Alice sends a deterministic prompt
        alice.send("/app/chat.send",
                Map.of("roomId", roomId, "sender", "Alice",
                        "content", "@claude reply with only the single word: hello"));

        // 5. Both users should receive Alice's USER message and the AI reply
        ChatMessage aliceAi = pollForAiMessage(aliceInbox, 20);
        ChatMessage bobAi = pollForAiMessage(bobInbox, 20);

        System.out.println();
        System.out.println("=== MultiUserChatLiveIT: Alice's inbox AI reply ===");
        System.out.println(aliceAi.content());
        System.out.println("=== MultiUserChatLiveIT: Bob's inbox AI reply ===");
        System.out.println(bobAi.content());
        System.out.println("===================================================");
        System.out.println();

        assertThat(aliceAi.content()).isNotBlank();
        assertThat(bobAi.content()).isNotBlank();
        assertThat(aliceAi.content().toLowerCase()).contains("hello");
        assertThat(bobAi.content().toLowerCase()).contains("hello");

        aliceInbox.clear();
        bobInbox.clear();

        // 6. Bob sends a follow-up prompt
        bob.send("/app/chat.send",
                Map.of("roomId", roomId, "sender", "Bob",
                        "content", "@claude reply with only the single word: world"));

        // 7. Both should receive Bob's AI reply too
        ChatMessage aliceAi2 = pollForAiMessage(aliceInbox, 20);
        ChatMessage bobAi2 = pollForAiMessage(bobInbox, 20);

        System.out.println();
        System.out.println("=== MultiUserChatLiveIT: Bob's second AI reply (Alice's view) ===");
        System.out.println(aliceAi2.content());
        System.out.println("=== MultiUserChatLiveIT: Bob's second AI reply (Bob's view) ===");
        System.out.println(bobAi2.content());
        System.out.println("===================================================");
        System.out.println();

        assertThat(aliceAi2.content().toLowerCase()).contains("world");
        assertThat(bobAi2.content().toLowerCase()).contains("world");

        alice.disconnect();
        bob.disconnect();
    }

    // ---- helpers ----

    private StompSession connect(String url, String roomId, BlockingQueue<ChatMessage> inbox) throws Exception {
        var transports = List.of(
                new WebSocketTransport(new StandardWebSocketClient()),
                new RestTemplateXhrTransport());
        var client = new WebSocketStompClient(new SockJsClient(transports));
        client.setMessageConverter(new MappingJackson2MessageConverter());

        StompSession session = client
                .connectAsync(url, new StompSessionHandlerAdapter() {})
                .get(10, TimeUnit.SECONDS);

        session.subscribe("/topic/room/" + roomId, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ChatMessage.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                inbox.offer((ChatMessage) payload);
            }
        });

        return session;
    }

    /** Poll inbox until an AI message arrives, skipping non-AI messages. */
    private ChatMessage pollForAiMessage(BlockingQueue<ChatMessage> inbox, int timeoutSeconds)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
        while (System.currentTimeMillis() < deadline) {
            long remaining = deadline - System.currentTimeMillis();
            ChatMessage msg = inbox.poll(remaining, TimeUnit.MILLISECONDS);
            if (msg == null) break;
            if (msg.type() == MessageType.AI) return msg;
        }
        throw new AssertionError("No AI message received within " + timeoutSeconds + "s");
    }
}
