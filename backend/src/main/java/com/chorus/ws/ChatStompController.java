package com.chorus.ws;

import com.chorus.config.ChorusProperties;
import com.chorus.domain.ChatMessage;
import com.chorus.domain.MessageType;
import com.chorus.service.AiReplyService;
import com.chorus.service.ChatAiService;
import com.chorus.service.RoomService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
public class ChatStompController {

    private final RoomService roomService;
    private final ChatAiService chatAiService;
    private final ChorusProperties chorusProperties;
    private final SimpMessagingTemplate messagingTemplate;
    private final SessionRegistry sessionRegistry;
    private final AiReplyService aiReplyService;

    public ChatStompController(
            RoomService roomService,
            ChatAiService chatAiService,
            ChorusProperties chorusProperties,
            SimpMessagingTemplate messagingTemplate,
            SessionRegistry sessionRegistry,
            AiReplyService aiReplyService) {
        this.roomService = roomService;
        this.chatAiService = chatAiService;
        this.chorusProperties = chorusProperties;
        this.messagingTemplate = messagingTemplate;
        this.sessionRegistry = sessionRegistry;
        this.aiReplyService = aiReplyService;
    }

    @MessageMapping("/chat.join")
    public void join(ChatJoinPayload payload, StompHeaderAccessor accessor) {
        String roomId = payload.roomId();
        String username = payload.username();
        if (isBlank(roomId) || isBlank(username)) {
            return;
        }
        var roomOpt = roomService.getRoom(roomId);
        if (roomOpt.isEmpty()) {
            publish(roomId, ChatMessage.of(MessageType.ERROR, "system", "Room not found."));
            return;
        }
        String sessionId = accessor.getSessionId();
        sessionRegistry.register(sessionId, roomId, username);
        roomService.addParticipant(roomId, username);
        publish(roomId, ChatMessage.of(MessageType.JOIN, username, ""));
    }

    @MessageMapping("/chat.send")
    public void send(ChatSendPayload payload, StompHeaderAccessor accessor) {
        String roomId = payload.roomId();
        String sender = payload.sender();
        String content = payload.content() == null ? "" : payload.content();
        if (isBlank(roomId) || isBlank(sender)) {
            return;
        }
        String sessionId = accessor.getSessionId();
        var binding = sessionRegistry.get(sessionId);
        if (binding.isEmpty()
                || !binding.get().roomId().equals(roomId)
                || !binding.get().username().equals(sender)) {
            publish(roomId, ChatMessage.of(MessageType.ERROR, "system", "Join the room before sending messages."));
            return;
        }
        var roomOpt = roomService.getRoom(roomId);
        if (roomOpt.isEmpty()) {
            publish(roomId, ChatMessage.of(MessageType.ERROR, "system", "Room not found."));
            return;
        }
        ChatMessage userMsg = ChatMessage.of(MessageType.USER, sender, content);
        roomService.appendMessage(roomId, userMsg);
        publish(roomId, userMsg);

        if (!chatAiService.shouldInvokeAi(chorusProperties, content)) {
            return;
        }

        publish(roomId, ChatMessage.of(MessageType.AI_TYPING, "Claude", ""));
        aiReplyService.replyAsync(roomId);
    }

    private void publish(String roomId, ChatMessage message) {
        messagingTemplate.convertAndSend("/topic/room/" + roomId, message);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    public record ChatJoinPayload(String roomId, String username) {}

    public record ChatSendPayload(String roomId, String sender, String content) {}
}
