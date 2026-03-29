package com.chorus.service;

import com.chorus.domain.ChatMessage;
import com.chorus.domain.MessageType;
import java.util.Optional;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AiReplyService {

    private static final String AI_SENDER = "Claude";

    private final RoomService roomService;
    private final ChatAiService chatAiService;
    private final GitHubService gitHubService;
    private final SimpMessagingTemplate messagingTemplate;

    public AiReplyService(
            RoomService roomService,
            ChatAiService chatAiService,
            GitHubService gitHubService,
            SimpMessagingTemplate messagingTemplate) {
        this.roomService = roomService;
        this.chatAiService = chatAiService;
        this.gitHubService = gitHubService;
        this.messagingTemplate = messagingTemplate;
    }

    @Async("chatTaskExecutor")
    public void replyAsync(String roomId) {
        var roomOpt = roomService.getRoom(roomId);
        if (roomOpt.isEmpty()) {
            return;
        }
        var room = roomOpt.get();
        var tail = roomService.conversationForModel(roomId);

        Optional<String> repoContext = Optional.empty();
        if (room.getLinkedRepo() != null) {
            String token = roomService.getGithubToken(roomId).orElse(null);
            if (token != null) {
                repoContext = gitHubService.fetchContextFile(
                        token, room.getLinkedRepo(),
                        room.getLinkedBranch() != null ? room.getLinkedBranch() : "main");
            }
        }

        try {
            String reply = chatAiService.generate(tail, repoContext);
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
