package com.chorus.config;

import java.util.Arrays;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "chorus")
public class ChorusProperties {

    private final Ai ai = new Ai();
    private final Cors cors = new Cors();

    public Ai getAi() {
        return ai;
    }

    public Cors getCors() {
        return cors;
    }

    public static class Cors {
        private String allowedOrigins = "http://localhost:3000";

        public String getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(String allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }

        public String[] originPatterns() {
            return Arrays.stream(allowedOrigins.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toArray(String[]::new);
        }
    }

    public static class Ai {
        /**
         * always: Claude responds to every user message.
         * mention: Claude responds only when the message contains @claude.
         */
        private Trigger trigger = Trigger.always;

        /** Approximate token budget for conversation history sent to Claude. */
        private int maxContextTokens = 8_000;

        public Trigger getTrigger() {
            return trigger;
        }

        public void setTrigger(Trigger trigger) {
            this.trigger = trigger;
        }

        public int getMaxContextTokens() {
            return maxContextTokens;
        }

        public void setMaxContextTokens(int maxContextTokens) {
            this.maxContextTokens = maxContextTokens;
        }
    }

    public enum Trigger {
        always,
        mention
    }
}
