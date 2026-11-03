package com.sms.studentmanagement.service;

import com.sms.studentmanagement.dto.RoutineDtos.TimeSlotRequest;
import com.sms.studentmanagement.dto.RoutineDtos.TimeSlotResponse;
import com.sms.studentmanagement.entity.TimeSlot;
import com.sms.studentmanagement.exception.ApiException;
import com.sms.studentmanagement.exception.NotFoundException;
import com.sms.studentmanagement.repository.TimeSlotRepository;
import com.sms.studentmanagement.repository.TimetableRepository;
import com.sms.studentmanagement.util.TimeSlotRules;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TimeSlotService {

    private final TimeSlotRepository slotRepository;
    private final TimetableRepository timetableRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<TimeSlotResponse> list() {
        return slotRepository.findAllByOrderByStartTime().stream().map(TimeSlotResponse::from).toList();
    }

    @Transactional
    public TimeSlotResponse create(TimeSlotRequest request) {
        String name = request.name().trim();
        TimeSlotRules.validateRange(request.startTime(), request.endTime());
        if (slotRepository.existsByName(name)) {
            throw conflict("এই নামে একটা স্লট আগে থেকেই আছে");
        }
        requireNoOverlap(null, request);

        TimeSlot slot = new TimeSlot();
        slot.setName(name);
        slot.setStartTime(request.startTime());
        slot.setEndTime(request.endTime());
        slot.setActive(request.active() == null || request.active());
        slotRepository.save(slot);
        auditService.log("TIME_SLOT_CREATED", "TIME_SLOT", slot.getId(),
                name + " | " + slot.getStartTime() + "–" + slot.getEndTime());
        return TimeSlotResponse.from(slot);
    }

    @Transactional
    public TimeSlotResponse update(Long id, TimeSlotRequest request) {
        TimeSlot slot = find(id);
        String name = request.name().trim();
        TimeSlotRules.validateRange(request.startTime(), request.endTime());
        if (slotRepository.existsByNameAndIdNot(name, id)) {
            throw conflict("এই নামে একটা স্লট আগে থেকেই আছে");
        }
        requireNoOverlap(id, request);

        slot.setName(name);
        slot.setStartTime(request.startTime());
        slot.setEndTime(request.endTime());
        if (request.active() != null) {
            slot.setActive(request.active());
        }
        auditService.log("TIME_SLOT_UPDATED", "TIME_SLOT", id,
                name + " | " + slot.getStartTime() + "–" + slot.getEndTime() + (slot.isActive() ? "" : " (নিষ্ক্রিয়)"));
        return TimeSlotResponse.from(slot);
    }

    @Transactional
    public void delete(Long id) {
        TimeSlot slot = find(id);
        if (timetableRepository.existsByTimeSlotId(id)) {
            throw conflict("এই স্লট রুটিনে ব্যবহার হচ্ছে, তাই মোছা যায় না। নিষ্ক্রিয় করে রাখতে পারেন");
        }
        slotRepository.delete(slot);
        auditService.log("TIME_SLOT_DELETED", "TIME_SLOT", id, slot.getName());
    }

    /** অন্য কোনো স্লটের সাথে (নিষ্ক্রিয় হলেও) সময় মিশে গেলে 409। */
    private void requireNoOverlap(Long ownId, TimeSlotRequest request) {
        for (TimeSlot other : slotRepository.findAllByOrderByStartTime()) {
            if (ownId != null && other.getId().equals(ownId)) {
                continue;
            }
            if (TimeSlotRules.overlaps(request.startTime(), request.endTime(), other.getStartTime(), other.getEndTime())) {
                throw conflict("এই সময় \"" + other.getName() + "\" স্লটের সাথে মিশে যাচ্ছে ("
                        + other.getStartTime() + "–" + other.getEndTime() + ")");
            }
        }
    }

    private TimeSlot find(Long id) {
        return slotRepository.findById(id).orElseThrow(() -> new NotFoundException("স্লট পাওয়া যায়নি"));
    }

    private static ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, message);
    }
}