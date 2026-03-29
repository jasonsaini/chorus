package com.chorus.service;

import com.chorus.config.ChorusProperties;
import com.chorus.domain.ChatMessage;
import com.chorus.domain.MessageType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

@Service
public class ChatAiService {

    private static final String SYSTEM = """
            You are Claude, a participant in a collaborative group chat. Multiple humans share this context.
            Answer clearly and concisely. You may address the group or specific people when helpful.""";

    private final ChatModel chatModel;

    public ChatAiService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public boolean shouldInvokeAi(ChorusProperties properties, String content) {
        if (properties.getAi().getTrigger() == ChorusProperties.Trigger.always) {
            return true;
        }
        return content != null && content.toLowerCase().contains("@claude");
    }

    public String generate(List<ChatMessage> historyTail, Optional<String> repoContext) {
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(SYSTEM));
        repoContext.ifPresent(ctx ->
                messages.add(new SystemMessage("## Linked repository context\n\n" + ctx)));
        for (ChatMessage cm : historyTail) {
            if (cm.type() == MessageType.USER) {
                messages.add(new UserMessage(cm.sender() + ": " + cm.content()));
            } else if (cm.type() == MessageType.AI) {
                messages.add(new AssistantMessage(cm.content()));
            }
        }
        var response = chatModel.call(new Prompt(messages));
        return response.getResult().getOutput().getText();
    }
}
