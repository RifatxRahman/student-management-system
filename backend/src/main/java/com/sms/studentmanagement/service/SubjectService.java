package com.sms.studentmanagement.service;

import com.sms.studentmanagement.dto.AcademicDtos.SubjectResponse;
import com.sms.studentmanagement.entity.Subject;
import com.sms.studentmanagement.exception.NotFoundException;
import com.sms.studentmanagement.repository.SubjectRepository;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubjectService {

    private final SubjectRepository repository;

    @Transactional(readOnly = true)
    public List<SubjectResponse> list() {
        return repository.findAll(Sort.by("name")).stream()
                .map(SubjectResponse::from)
                .toList();
    }

    @Transactional
    public SubjectResponse create(String name, String code) {
        Subject subject = new Subject();
        subject.setName(name.trim());
        subject.setCode(normalizeCode(code));
        subject.setActive(true);
        return SubjectResponse.from(repository.saveAndFlush(subject));
    }

    @Transactional
    public SubjectResponse update(Long id, String name, String code) {
        Subject subject = find(id);
        subject.setName(name.trim());
        subject.setCode(normalizeCode(code));
        repository.flush();
        return SubjectResponse.from(subject);
    }

    @Transactional
    public SubjectResponse setActive(Long id, boolean active) {
        Subject subject = find(id);
        subject.setActive(active);
        repository.flush();
        return SubjectResponse.from(subject);
    }

    private Subject find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Subject পাওয়া যায়নি"));
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }
}