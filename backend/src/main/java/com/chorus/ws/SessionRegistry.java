package com.chorus.ws;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class SessionRegistry {

    public record Binding(String roomId, String username) {}

    private final Map<String, Binding> bySessionId = new ConcurrentHashMap<>();

    public void register(String sessionId, String roomId, String username) {
        bySessionId.put(sessionId, new Binding(roomId, username));
    }

    public Optional<Binding> get(String sessionId) {
        return Optional.ofNullable(bySessionId.get(sessionId));
    }

    public Optional<Binding> remove(String sessionId) {
        return Optional.ofNullable(bySessionId.remove(sessionId));
    }
}
