package com.sms.studentmanagement.service;

import com.sms.studentmanagement.dto.AcademicEventDtos.AcademicEventRequest;
import com.sms.studentmanagement.dto.AcademicEventDtos.AcademicEventResponse;
import com.sms.studentmanagement.entity.AcademicEvent;
import com.sms.studentmanagement.entity.AcademicSession;
import com.sms.studentmanagement.exception.ApiException;
import com.sms.studentmanagement.exception.NotFoundException;
import com.sms.studentmanagement.repository.AcademicEventRepository;
import com.sms.studentmanagement.repository.AcademicSessionRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AcademicEventService {

    private final AcademicEventRepository eventRepository;
    private final AcademicSessionRepository sessionRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<AcademicEventResponse> list(Long sessionId, LocalDate from, LocalDate to) {
        List<AcademicEvent> list;
        if (from != null && to != null) {
            if (sessionId != null) {
                list = eventRepository.findBySessionIdAndRangeWithSession(sessionId, from, to);
            } else {
                list = eventRepository.findInRangeWithSession(from, to);
            }
        } else if (sessionId != null) {
            list = eventRepository.findBySessionIdWithSession(sessionId);
        } else {
            AcademicSession active = sessionRepository.findByActiveTrue()
                    .orElseThrow(() -> new NotFoundException("কোনো সেশন সক্রিয় নেই"));
            list = eventRepository.findBySessionIdWithSession(active.getId());
        }

        List<AcademicEventResponse> responses = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            responses.add(AcademicEventResponse.from(list.get(i)));
        }
        return responses;
    }

    @Transactional
    public AcademicEventResponse create(AcademicEventRequest request) {
        if (request.startDate().isAfter(request.endDate())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "শুরুর তারিখ শেষের তারিখের চেয়ে পরে হতে পারে না");
        }

        Long sessionId = request.sessionId();
        AcademicSession session;
        if (sessionId != null) {
            session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new NotFoundException("সেশন পাওয়া যায়নি"));
        } else {
            session = sessionRepository.findByActiveTrue()
                    .orElseThrow(() -> new NotFoundException("কোনো সেশন সক্রিয় নেই"));
        }

        AcademicEvent event = new AcademicEvent();
        event.setSession(session);
        event.setTitle(request.title().trim());
        event.setEventType(request.eventType());
        event.setStartDate(request.startDate());
        event.setEndDate(request.endDate());
        event.setHoliday(request.isHoliday() != null && request.isHoliday());
        event.setDescription(request.description() != null ? request.description().trim() : null);
        event.setTargetAudience(request.targetAudience() != null ? request.targetAudience() : com.sms.studentmanagement.entity.EventAudience.ALL);

        eventRepository.save(event);

        auditService.log("ACADEMIC_EVENT_CREATED", "ACADEMIC_EVENT", event.getId(),
                event.getTitle() + " (" + event.getStartDate() + " হতে " + event.getEndDate() + ")");

        return AcademicEventResponse.from(event);
    }

    @Transactional
    public AcademicEventResponse update(Long id, AcademicEventRequest request) {
        AcademicEvent event = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("একাডেমিক ইভেন্ট পাওয়া যায়নি"));

        if (request.startDate().isAfter(request.endDate())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "শুরুর তারিখ শেষের তারিখের চেয়ে পরে হতে পারে না");
        }

        event.setTitle(request.title().trim());
        event.setEventType(request.eventType());
        event.setStartDate(request.startDate());
        event.setEndDate(request.endDate());
        if (request.isHoliday() != null) {
            event.setHoliday(request.isHoliday());
        }
        event.setDescription(request.description() != null ? request.description().trim() : null);
        if (request.targetAudience() != null) {
            event.setTargetAudience(request.targetAudience());
        }

        eventRepository.save(event);

        auditService.log("ACADEMIC_EVENT_UPDATED", "ACADEMIC_EVENT", id, event.getTitle());

        return AcademicEventResponse.from(event);
    }

    @Transactional
    public void delete(Long id) {
        AcademicEvent event = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("একাডেমিক ইভেন্ট পাওয়া যায়নি"));

        eventRepository.delete(event);
        auditService.log("ACADEMIC_EVENT_DELETED", "ACADEMIC_EVENT", id, event.getTitle());
    }
}
