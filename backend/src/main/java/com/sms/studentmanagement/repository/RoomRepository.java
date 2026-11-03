package com.sms.studentmanagement.repository;

import com.sms.studentmanagement.entity.Room;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findAllByOrderByName();

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}