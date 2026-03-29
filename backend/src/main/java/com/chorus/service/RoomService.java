package com.chorus.service;

import com.chorus.domain.ChatMessage;
import com.chorus.domain.MessageType;
import com.chorus.domain.Room;
import com.chorus.persistence.RoomEntity;
import com.chorus.persistence.RoomJpaRepository;
import com.chorus.persistence.RoomMessageEntity;
import com.chorus.persistence.RoomMessageRepository;
import com.chorus.persistence.RoomParticipantEntity;
import com.chorus.persistence.RoomParticipantRepository;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomService {

    private final RoomJpaRepository roomRepository;
    private final RoomMessageRepository messageRepository;
    private final RoomParticipantRepository participantRepository;

    public RoomService(
            RoomJpaRepository roomRepository,
            RoomMessageRepository messageRepository,
            RoomParticipantRepository participantRepository) {
        this.roomRepository = roomRepository;
        this.messageRepository = messageRepository;
        this.participantRepository = participantRepository;
    }

    @Transactional
    public Room createRoom(String createdBy) {
        String id = shortId();
        Instant now = Instant.now();
        RoomEntity entity = new RoomEntity(id, id, createdBy, now);
        roomRepository.save(entity);
        return new Room(id, id, createdBy, now, List.of(), Set.of());
    }

    @Transactional(readOnly = true)
    public Optional<Room> getRoom(String roomId) {
        return roomRepository.findById(roomId).map(this::toDomain);
    }

    @Transactional
    public void addParticipant(String roomId, String username) {
        roomRepository
                .findById(roomId)
                .ifPresent(room -> {
                    if (!participantRepository.existsByRoom_RoomIdAndUsername(roomId, username)) {
                        participantRepository.save(new RoomParticipantEntity(room, username));
                    }
                });
    }

    @Transactional
    public void removeParticipant(String roomId, String username) {
        participantRepository.deleteByRoom_RoomIdAndUsername(roomId, username);
    }

    /**
     * Persists a chat line for history and model context. Only {@link MessageType#USER} and
     * {@link MessageType#AI} are stored; other types are ignored.
     */
    @Transactional
    public boolean appendMessage(String roomId, ChatMessage message) {
        if (message.type() != MessageType.USER && message.type() != MessageType.AI) {
            return false;
        }
        return roomRepository
                .findById(roomId)
                .map(room -> {
                    messageRepository.save(new RoomMessageEntity(
                            room, message.type(), message.sender(), message.content(), message.timestamp()));
                    return true;
                })
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public List<ChatMessage> conversationForModel(String roomId) {
        return messageRepository.findByRoom_RoomIdOrderByIdAsc(roomId).stream()
                .filter(m -> m.getType() == MessageType.USER || m.getType() == MessageType.AI)
                .map(this::toChatMessage)
                .toList();
    }

    private Room toDomain(RoomEntity entity) {
        List<ChatMessage> messages = messageRepository.findByRoom_RoomIdOrderByIdAsc(entity.getRoomId()).stream()
                .map(this::toChatMessage)
                .toList();
        Set<String> participants = participantRepository.findByRoom_RoomId(entity.getRoomId()).stream()
                .map(RoomParticipantEntity::getUsername)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return new Room(
                entity.getRoomId(),
                entity.getInviteCode(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                messages,
                participants);
    }

    private ChatMessage toChatMessage(RoomMessageEntity entity) {
        return new ChatMessage(
                entity.getType(), entity.getSender(), entity.getContent(), entity.getTimestamp());
    }

    private static String shortId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
