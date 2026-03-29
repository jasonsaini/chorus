package com.chorus.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "room_participants",
        uniqueConstraints = @UniqueConstraint(columnNames = {"room_id", "username"}))
public class RoomParticipantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY, cascade = {})
    @JoinColumn(name = "room_id", nullable = false)
    private RoomEntity room;

    @Column(nullable = false, length = 256)
    private String username;

    protected RoomParticipantEntity() {}

    public RoomParticipantEntity(RoomEntity room, String username) {
        this.room = room;
        this.username = username;
    }

    public Long getId() {
        return id;
    }

    public RoomEntity getRoom() {
        return room;
    }

    public String getUsername() {
        return username;
    }
}
