package com.sms.studentmanagement.service;

import com.sms.studentmanagement.dto.AcademicDtos.SessionResponse;
import com.sms.studentmanagement.entity.AcademicSession;
import com.sms.studentmanagement.exception.ApiException;
import com.sms.studentmanagement.exception.NotFoundException;
import com.sms.studentmanagement.repository.AcademicSessionRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AcademicSessionService {

    private final AcademicSessionRepository repository;

    @Transactional(readOnly = true)
    public List<SessionResponse> list() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "startDate")).stream()
                .map(SessionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SessionResponse getActive() {
        return repository.findByActiveTrue()
                .map(SessionResponse::from)
                .orElseThrow(() -> new NotFoundException("কোনো সক্রিয় সেশন নেই"));
    }

    @Transactional
    public SessionResponse create(String name, LocalDate startDate, LocalDate endDate) {
        validateDates(startDate, endDate);
        AcademicSession session = new AcademicSession();
        session.setName(name.trim());
        session.setStartDate(startDate);
        session.setEndDate(endDate);
        session.setActive(false);
        return SessionResponse.from(repository.saveAndFlush(session));
    }

    @Transactional
    public SessionResponse update(Long id, String name, LocalDate startDate, LocalDate endDate) {
        validateDates(startDate, endDate);
        AcademicSession session = find(id);
        session.setName(name.trim());
        session.setStartDate(startDate);
        session.setEndDate(endDate);
        repository.flush();
        return SessionResponse.from(session);
    }

    @Transactional
    public SessionResponse activate(Long id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("সেশন পাওয়া যায়নি");
        }
        // আগে বাকি সব নিষ্ক্রিয়, তারপর এটা সক্রিয় (একসাথে একটাই সক্রিয় থাকবে)
        repository.deactivateAll(Instant.now());
        AcademicSession session = find(id);
        session.setActive(true);
        repository.flush();
        return SessionResponse.from(session);
    }

    @Transactional
    public SessionResponse deactivate(Long id) {
        AcademicSession session = find(id);
        session.setActive(false);
        repository.flush();
        return SessionResponse.from(session);
    }

    private AcademicSession find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("সেশন পাওয়া যায়নি"));
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (!endDate.isAfter(startDate)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "শেষ তারিখ শুরুর তারিখের পরে হতে হবে");
        }
    }
}