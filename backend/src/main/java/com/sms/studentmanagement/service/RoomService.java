package com.sms.studentmanagement.service;

import com.sms.studentmanagement.dto.RoutineDtos.RoomRequest;
import com.sms.studentmanagement.dto.RoutineDtos.RoomResponse;
import com.sms.studentmanagement.entity.Room;
import com.sms.studentmanagement.exception.ApiException;
import com.sms.studentmanagement.exception.NotFoundException;
import com.sms.studentmanagement.repository.RoomRepository;
import com.sms.studentmanagement.repository.TimetableRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final TimetableRepository timetableRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<RoomResponse> list() {
        return roomRepository.findAllByOrderByName().stream().map(RoomResponse::from).toList();
    }

    @Transactional
    public RoomResponse create(RoomRequest request) {
        String name = request.name().trim();
        if (roomRepository.existsByName(name)) {
            throw new ApiException(HttpStatus.CONFLICT, "এই নামে একটা রুম আগে থেকেই আছে");
        }
        Room room = new Room();
        room.setName(name);
        room.setCapacity(request.capacity());
        room.setActive(request.active() == null || request.active());
        roomRepository.save(room);
        auditService.log("ROOM_CREATED", "ROOM", room.getId(), name);
        return RoomResponse.from(room);
    }

    @Transactional
    public RoomResponse update(Long id, RoomRequest request) {
        Room room = find(id);
        String name = request.name().trim();
        if (roomRepository.existsByNameAndIdNot(name, id)) {
            throw new ApiException(HttpStatus.CONFLICT, "এই নামে একটা রুম আগে থেকেই আছে");
        }
        room.setName(name);
        room.setCapacity(request.capacity());
        if (request.active() != null) {
            room.setActive(request.active());
        }
        auditService.log("ROOM_UPDATED", "ROOM", id, name + (room.isActive() ? "" : " (নিষ্ক্রিয়)"));
        return RoomResponse.from(room);
    }

    @Transactional
    public void delete(Long id) {
        Room room = find(id);
        if (timetableRepository.existsByRoomId(id)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "এই রুম রুটিনে ব্যবহার হচ্ছে, তাই মোছা যায় না। নিষ্ক্রিয় করে রাখতে পারেন");
        }
        roomRepository.delete(room);
        auditService.log("ROOM_DELETED", "ROOM", id, room.getName());
    }

    private Room find(Long id) {
        return roomRepository.findById(id).orElseThrow(() -> new NotFoundException("রুম পাওয়া যায়নি"));
    }
}