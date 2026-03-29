package com.chorus.web;

import com.chorus.domain.ChatMessage;
import com.chorus.service.RoomService;
import com.chorus.web.dto.CreateRoomRequest;
import com.chorus.web.dto.RoomDetailResponse;
import com.chorus.web.dto.RoomResponse;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
public class RoomRestController {

    private final RoomService roomService;

    public RoomRestController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping
    public ResponseEntity<RoomResponse> create(@Valid @RequestBody CreateRoomRequest request) {
        var room = roomService.createRoom(request.createdBy());
        RoomResponse body =
                new RoomResponse(room.getRoomId(), room.getInviteCode(), room.getCreatedBy(), room.getCreatedAt());
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<RoomDetailResponse> get(@PathVariable String roomId) {
        return roomService
                .getRoom(roomId)
                .map(room -> {
                    List<String> participants = new ArrayList<>(room.getParticipants());
                    List<ChatMessage> messages = List.copyOf(room.getMessages());
                    return ResponseEntity.ok(new RoomDetailResponse(room.getRoomId(), participants, messages));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
