package com.chorus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chorus.config.ChorusProperties;
import com.chorus.domain.ChatMessage;
import com.chorus.domain.MessageType;
import com.chorus.service.ChatAiService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

@ExtendWith(MockitoExtension.class)
class ChatAiServiceTest {

    @Mock
    private ChatModel chatModel;

    private ChatAiService chatAiService;

    @BeforeEach
    void setUp() {
        chatAiService = new ChatAiService(chatModel);
    }

    @Test
    void generateWithoutRepoContextSendsOnlySystemAndHistory() {
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("ok")))));
        var history = List.of(ChatMessage.of(MessageType.USER, "Alice", "hello"));

        chatAiService.generate(history, Optional.empty());

        var captor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(captor.capture());
        var messages = captor.getValue().getInstructions();

        // system + 1 user message = 2, no repo context injected
        assertThat(messages).hasSize(2);
        assertThat(messages.get(0).getText()).doesNotContain("repository context");
    }

    @Test
    void generateWithRepoContextInjectsItAsSecondSystemMessage() {
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("ok")))));
        var history = List.of(ChatMessage.of(MessageType.USER, "Alice", "what does this do?"));
        String contextFileContent = "# My Project\nDoes cool stuff.";

        chatAiService.generate(history, Optional.of(contextFileContent));

        var captor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(captor.capture());
        var messages = captor.getValue().getInstructions();

        // system + repo context system + 1 user = 3
        assertThat(messages).hasSize(3);
        assertThat(messages.get(1).getText()).contains("repository context");
        assertThat(messages.get(1).getText()).contains("My Project");
    }

    @Test
    void generateFormatsSenderPrefixInUserMessages() {
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("ok")))));
        var history = List.of(
                ChatMessage.of(MessageType.USER, "Bob", "ping"),
                ChatMessage.of(MessageType.AI, "Claude", "pong"),
                ChatMessage.of(MessageType.USER, "Alice", "nice"));

        chatAiService.generate(history, Optional.empty());

        var captor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(captor.capture());
        var messages = captor.getValue().getInstructions();

        // system + 2 user + 1 assistant = 4
        assertThat(messages).hasSize(4);
        assertThat(messages.get(1).getText()).isEqualTo("Bob: ping");
        assertThat(messages.get(3).getText()).isEqualTo("Alice: nice");
    }

    @Test
    void shouldInvokeAiReturnsTrueWhenTriggerIsAlways() {
        var props = new ChorusProperties();
        assertThat(chatAiService.shouldInvokeAi(props, "anything")).isTrue();
    }

    @Test
    void shouldInvokeAiReturnsFalseForMentionTriggerWithoutAtClaude() {
        var props = new ChorusProperties();
        props.getAi().setTrigger(ChorusProperties.Trigger.mention);
        assertThat(chatAiService.shouldInvokeAi(props, "just chatting")).isFalse();
    }

    @Test
    void shouldInvokeAiReturnsTrueForMentionTriggerWithAtClaude() {
        var props = new ChorusProperties();
        props.getAi().setTrigger(ChorusProperties.Trigger.mention);
        assertThat(chatAiService.shouldInvokeAi(props, "hey @claude what's up")).isTrue();
    }
}
