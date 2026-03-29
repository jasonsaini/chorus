package com.chorus.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomMessageRepository extends JpaRepository<RoomMessageEntity, Long> {

    List<RoomMessageEntity> findByRoom_RoomIdOrderByIdAsc(String roomId);
}
