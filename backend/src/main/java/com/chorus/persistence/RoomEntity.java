package com.chorus.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "rooms")
public class RoomEntity {

    @Id
    @Column(length = 64)
    private String roomId;

    @Column(nullable = false, length = 64)
    private String inviteCode;

    @Column(nullable = false, length = 256)
    private String createdBy;

    @Column(nullable = false)
    private Instant createdAt;

    protected RoomEntity() {}

    public RoomEntity(String roomId, String inviteCode, String createdBy, Instant createdAt) {
        this.roomId = roomId;
        this.inviteCode = inviteCode;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
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
}
