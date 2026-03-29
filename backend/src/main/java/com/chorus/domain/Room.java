package com.chorus.domain;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * In-memory view of a room for API responses. Source of truth is the database.
 */
public class Room {

    private final String roomId;
    private final String inviteCode;
    private final String createdBy;
    private final Instant createdAt;
    private final List<ChatMessage> messages;
    private final Set<String> participants;
    private final String linkedRepo;
    private final String linkedBranch;

    public Room(
            String roomId,
            String inviteCode,
            String createdBy,
            Instant createdAt,
            List<ChatMessage> messages,
            Set<String> participants,
            String linkedRepo,
            String linkedBranch) {
        this.roomId = roomId;
        this.inviteCode = inviteCode;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.messages = List.copyOf(messages);
        this.participants = new LinkedHashSet<>(participants);
        this.linkedRepo = linkedRepo;
        this.linkedBranch = linkedBranch;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getInviteCode() {
        return inviteCode;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public Set<String> getParticipants() {
        return participants;
    }

    public String getLinkedRepo() {
        return linkedRepo;
    }

    public String getLinkedBranch() {
        return linkedBranch;
    }
}
