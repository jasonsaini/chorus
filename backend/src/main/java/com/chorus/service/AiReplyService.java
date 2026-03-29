package com.chorus.service;

import com.chorus.domain.ChatMessage;
import com.chorus.domain.MessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AiReplyService {

    private static final String AI_SENDER = "Claude";

    private final RoomService roomService;
    private final ChatAiService chatAiService;
    private final SimpMessagingTemplate messagingTemplate;

    public AiReplyService(RoomService roomService, ChatAiService chatAiService, SimpMessagingTemplate messagingTemplate) {
        this.roomService = roomService;
        this.chatAiService = chatAiService;
        this.messagingTemplate = messagingTemplate;
    }

    @Async("chatTaskExecutor")
    public void replyAsync(String roomId) {
        if (roomService.getRoom(roomId).isEmpty()) {
            return;
        }
        var tail = roomService.conversationForModel(roomId);
        try {
            String reply = chatAiService.generate(tail);
            ChatMessage aiMsg = ChatMessage.of(MessageType.AI, AI_SENDER, reply);
            roomService.appendMessage(roomId, aiMsg);
            messagingTemplate.convertAndSend("/topic/room/" + roomId, aiMsg);
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "AI request failed.";
            messagingTemplate.convertAndSend(
                    "/topic/room/" + roomId, ChatMessage.of(MessageType.ERROR, "system", msg));
        }
    }
}
