package com.chorus.persistence;

import com.chorus.domain.MessageType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "room_messages")
public class RoomMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private RoomEntity room;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MessageType type;

    @Column(nullable = false, length = 256)
    private String sender;

    @Column(nullable = false, length = 50_000)
    private String content;

    @Column(nullable = false)
    private Instant timestamp;

    protected RoomMessageEntity() {}

    public RoomMessageEntity(RoomEntity room, MessageType type, String sender, String content, Instant timestamp) {
        this.room = room;
        this.type = type;
        this.sender = sender;
        this.content = content;
        this.timestamp = timestamp;
    }

    public Long getId() {
        return id;
    }

    public RoomEntity getRoom() {
        return room;
    }

    public MessageType getType() {
        return type;
    }

    public String getSender() {
        return sender;
    }

    public String getContent() {
        return content;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
