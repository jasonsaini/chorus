package com.chorus.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomParticipantRepository extends JpaRepository<RoomParticipantEntity, Long> {

    List<RoomParticipantEntity> findByRoom_RoomId(String roomId);

    boolean existsByRoom_RoomIdAndUsername(String roomId, String username);

    void deleteByRoom_RoomIdAndUsername(String roomId, String username);
}
