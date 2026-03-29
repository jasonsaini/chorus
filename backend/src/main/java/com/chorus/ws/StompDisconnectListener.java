package com.chorus.ws;

import com.chorus.domain.ChatMessage;
import com.chorus.domain.MessageType;
import com.chorus.service.RoomService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class StompDisconnectListener {

    private final SessionRegistry sessionRegistry;
    private final RoomService roomService;
    private final SimpMessagingTemplate messagingTemplate;

    public StompDisconnectListener(
            SessionRegistry sessionRegistry, RoomService roomService, SimpMessagingTemplate messagingTemplate) {
        this.sessionRegistry = sessionRegistry;
        this.roomService = roomService;
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        sessionRegistry.remove(event.getSessionId()).ifPresent(binding -> {
            roomService.removeParticipant(binding.roomId(), binding.username());
            messagingTemplate.convertAndSend(
                    "/topic/room/" + binding.roomId(),
                    ChatMessage.of(MessageType.LEAVE, binding.username(), ""));
        });
    }
}
