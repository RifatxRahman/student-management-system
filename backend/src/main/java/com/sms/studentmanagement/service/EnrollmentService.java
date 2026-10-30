package com.sms.studentmanagement.service;

import com.sms.studentmanagement.dto.PeopleDtos.EnrollmentResponse;
import com.sms.studentmanagement.entity.AcademicSession;
import com.sms.studentmanagement.entity.Enrollment;
import com.sms.studentmanagement.entity.EnrollmentStatus;
import com.sms.studentmanagement.entity.SchoolClass;
import com.sms.studentmanagement.entity.Section;
import com.sms.studentmanagement.entity.Student;
import com.sms.studentmanagement.entity.StudentStatus;
import com.sms.studentmanagement.exception.ApiException;
import com.sms.studentmanagement.exception.NotFoundException;
import com.sms.studentmanagement.repository.AcademicSessionRepository;
import com.sms.studentmanagement.repository.EnrollmentRepository;
import com.sms.studentmanagement.repository.SchoolClassRepository;
import com.sms.studentmanagement.repository.SectionRepository;
import com.sms.studentmanagement.repository.StudentRepository;
import com.sms.studentmanagement.util.DhakaTime;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final StudentRepository studentRepository;
    private final AcademicSessionRepository sessionRepository;
    private final SchoolClassRepository classRepository;
    private final SectionRepository sectionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AuditService auditService;

    @Transactional
    public EnrollmentResponse enroll(
            Long studentId, Long sessionId, Long classId, Long sectionId, Integer rollNumber, LocalDate startDate) {

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new NotFoundException("শিক্ষার্থী পাওয়া যায়নি"));
        if (student.getStatus() != StudentStatus.ACTIVE) {
            throw bad("শুধু সক্রিয় শিক্ষার্থীকে ভর্তি করা যায়");
        }
        AcademicSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("সেশন পাওয়া যায়নি"));
        SchoolClass schoolClass = classRepository.findById(classId)
                .orElseThrow(() -> new NotFoundException("ক্লাস পাওয়া যায়নি"));
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new NotFoundException("সেকশন পাওয়া যায়নি"));

        validatePlacement(schoolClass, section);

        LocalDate start = startDate != null ? startDate : DhakaTime.today();
        validateWithinSession(session, start);

        if (enrollmentRepository.existsByStudentIdAndSessionIdAndStatus(
                studentId, sessionId, EnrollmentStatus.ACTIVE)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "এই শিক্ষার্থী এই সেশনে আগে থেকেই ভর্তি আছে। বদলি করতে চাইলে 'বদলি' ব্যবহার করুন।");
        }

        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setSession(session);
        enrollment.setSchoolClass(schoolClass);
        enrollment.setSection(section);
        enrollment.setRollNumber(resolveRoll(rollNumber, sessionId, sectionId));
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        enrollment.setStartDate(start);
        enrollmentRepository.saveAndFlush(enrollment);

        auditService.log("ENROLLED", "STUDENT", studentId,
                session.getName() + " / " + schoolClass.getName() + " " + section.getName()
                        + " / রোল " + enrollment.getRollNumber());
        return EnrollmentResponse.from(enrollment);
    }

    @Transactional
    public EnrollmentResponse transfer(
            Long enrollmentId, Long classId, Long sectionId, Integer rollNumber, LocalDate effectiveDate) {

        Enrollment current = findActive(enrollmentId);
        SchoolClass schoolClass = classRepository.findById(classId)
                .orElseThrow(() -> new NotFoundException("ক্লাস পাওয়া যায়নি"));
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new NotFoundException("সেকশন পাওয়া যায়নি"));
        validatePlacement(schoolClass, section);

        if (current.getSection().getId().equals(sectionId)) {
            throw bad("শিক্ষার্থী আগে থেকেই এই সেকশনে আছে");
        }
        LocalDate date = effectiveDate != null ? effectiveDate : DhakaTime.today();
        if (date.isBefore(current.getStartDate())) {
            throw bad("বদলির তারিখ বর্তমান ভর্তির শুরুর আগে হতে পারে না");
        }
        validateWithinSession(current.getSession(), date);

        String from = current.getSchoolClass().getName() + " " + current.getSection().getName()
                + " / রোল " + current.getRollNumber();

        // পুরনো ভর্তি আগে বন্ধ করে সেভ না করলে নতুনটা ঢোকার সময় "একই শিক্ষার্থী দুবার সক্রিয়" বলে আটকাবে
        current.setStatus(EnrollmentStatus.TRANSFERRED);
        current.setEndDate(date);
        enrollmentRepository.flush();

        Enrollment next = new Enrollment();
        next.setStudent(current.getStudent());
        next.setSession(current.getSession());
        next.setSchoolClass(schoolClass);
        next.setSection(section);
        next.setRollNumber(resolveRoll(rollNumber, current.getSession().getId(), sectionId));
        next.setStatus(EnrollmentStatus.ACTIVE);
        next.setStartDate(date);
        enrollmentRepository.saveAndFlush(next);

        auditService.log("TRANSFERRED", "STUDENT", current.getStudent().getId(),
                from + " থেকে " + schoolClass.getName() + " " + section.getName()
                        + " / রোল " + next.getRollNumber());
        return EnrollmentResponse.from(next);
    }

    @Transactional
    public EnrollmentResponse withdraw(Long enrollmentId, LocalDate endDate) {
        Enrollment enrollment = findActive(enrollmentId);
        LocalDate date = endDate != null ? endDate : DhakaTime.today();
        if (date.isBefore(enrollment.getStartDate())) {
            throw bad("শেষ তারিখ ভর্তির শুরুর আগে হতে পারে না");
        }
        enrollment.setStatus(EnrollmentStatus.WITHDRAWN);
        enrollment.setEndDate(date);
        enrollmentRepository.flush();

        auditService.log("WITHDRAWN", "STUDENT", enrollment.getStudent().getId(),
                enrollment.getSchoolClass().getName() + " " + enrollment.getSection().getName());
        return EnrollmentResponse.from(enrollment);
    }

    // ---------- ভেতরের সাহায্যকারী ----------
    private Enrollment findActive(Long id) {
        Enrollment enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("ভর্তি পাওয়া যায়নি"));
        if (enrollment.getStatus() != EnrollmentStatus.ACTIVE) {
            throw bad("এই ভর্তি আর সক্রিয় নয়");
        }
        return enrollment;
    }

    private void validatePlacement(SchoolClass schoolClass, Section section) {
        if (!schoolClass.isActive() || !section.isActive()) {
            throw bad("নিষ্ক্রিয় ক্লাস বা সেকশনে ভর্তি করা যায় না");
        }
        if (!section.getSchoolClass().getId().equals(schoolClass.getId())) {
            throw bad("এই সেকশন এই ক্লাসের নয়");
        }
    }

    private void validateWithinSession(AcademicSession session, LocalDate date) {
        if (date.isBefore(session.getStartDate()) || date.isAfter(session.getEndDate())) {
            throw bad("তারিখ সেশনের সময়ের মধ্যে হতে হবে");
        }
    }

    private int resolveRoll(Integer requested, Long sessionId, Long sectionId) {
        if (requested != null) {
            return requested;
        }
        return enrollmentRepository.maxRoll(sessionId, sectionId, EnrollmentStatus.ACTIVE) + 1;
    }

    private static ApiException bad(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }
}