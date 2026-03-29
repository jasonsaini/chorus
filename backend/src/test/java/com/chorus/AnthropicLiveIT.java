package com.chorus;

import static org.assertj.core.api.Assertions.assertThat;

import com.chorus.domain.ChatMessage;
import com.chorus.domain.MessageType;
import com.chorus.service.ChatAiService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Opt-in live test: calls the real Anthropic API (costs a small amount of usage).
 * Skipped unless {@code ANTHROPIC_API_KEY} is set in the environment.
 *
 * <p>Run: {@code ANTHROPIC_API_KEY=sk-ant-... ./mvnw test -Dtest=AnthropicLiveIT}
 */
@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
class AnthropicLiveIT {

    @Autowired
    private ChatAiService chatAiService;

    @Test
    void generateCallsClaudeAndReturnsNonEmptyReply() {
        List<ChatMessage> history =
                List.of(ChatMessage.of(MessageType.USER, "Tester", "Reply with only the single word: pong"));
        String reply = chatAiService.generate(history, Optional.empty());
        System.out.println();
        System.out.println("=== AnthropicLiveIT: Claude reply ===");
        System.out.println(reply);
        System.out.println("====================================");
        System.out.println();
        assertThat(reply).isNotBlank();
        assertThat(reply.toLowerCase()).contains("pong");
    }
}
