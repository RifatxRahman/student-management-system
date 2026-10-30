package com.sms.studentmanagement.service;

import com.sms.studentmanagement.dto.PageResponse;
import com.sms.studentmanagement.dto.PeopleDtos.EnrollmentResponse;
import com.sms.studentmanagement.dto.PeopleDtos.GuardianLink;
import com.sms.studentmanagement.dto.PeopleDtos.StudentCreateRequest;
import com.sms.studentmanagement.dto.PeopleDtos.StudentDetail;
import com.sms.studentmanagement.dto.PeopleDtos.StudentResponse;
import com.sms.studentmanagement.dto.PeopleDtos.StudentSummary;
import com.sms.studentmanagement.dto.PeopleDtos.StudentUpdateRequest;
import com.sms.studentmanagement.entity.AcademicSession;
import com.sms.studentmanagement.entity.Enrollment;
import com.sms.studentmanagement.entity.EnrollmentStatus;
import com.sms.studentmanagement.entity.Guardian;
import com.sms.studentmanagement.entity.GuardianRelation;
import com.sms.studentmanagement.entity.Student;
import com.sms.studentmanagement.entity.StudentGuardian;
import com.sms.studentmanagement.entity.StudentStatus;
import com.sms.studentmanagement.exception.ApiException;
import com.sms.studentmanagement.exception.NotFoundException;
import com.sms.studentmanagement.repository.AcademicSessionRepository;
import com.sms.studentmanagement.repository.EnrollmentRepository;
import com.sms.studentmanagement.repository.GuardianRepository;
import com.sms.studentmanagement.repository.StudentGuardianRepository;
import com.sms.studentmanagement.repository.StudentRepository;
import com.sms.studentmanagement.util.Changes;
import com.sms.studentmanagement.util.DhakaTime;
import com.sms.studentmanagement.util.PhoneNumbers;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final GuardianRepository guardianRepository;
    private final StudentGuardianRepository studentGuardianRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AcademicSessionRepository sessionRepository;
    private final StudentCodeGenerator codeGenerator;
    private final AuditService auditService;

    // ---------- খোঁজা ----------
    @Transactional(readOnly = true)
    public PageResponse<StudentSummary> search(
            String q, StudentStatus status, Long sessionId, Long classId, Long sectionId, int page, int size) {

        boolean enrolledFilter = classId != null || sectionId != null;
        Long effectiveSessionId = sessionId;
        if (effectiveSessionId == null) {
            effectiveSessionId = sessionRepository.findByActiveTrue().map(AcademicSession::getId).orElse(null);
        }
        if (enrolledFilter && effectiveSessionId == null) {
            throw bad("ক্লাস বা সেকশন দিয়ে খুঁজতে একটা সেশন বেছে নিন বা একটা সেশন সক্রিয় করুন");
        }

        String term = (q == null || q.isBlank()) ? null : q.trim();
        Pageable pageable = PageRequest.of(
                Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by("fullName").and(Sort.by("id")));

        Page<Student> result = studentRepository.search(
                status, term, enrolledFilter, effectiveSessionId, classId, sectionId,
                EnrollmentStatus.ACTIVE, pageable);

        // পুরো পাতার শিক্ষার্থীদের ভর্তি একসাথে আনা হয় (প্রতিজনের জন্য আলাদা প্রশ্ন নয়)
        Map<Long, Enrollment> currentByStudent = new HashMap<>();
        if (effectiveSessionId != null && !result.isEmpty()) {
            List<Long> ids = result.stream().map(Student::getId).toList();
            for (Enrollment e : enrollmentRepository.findForStudents(ids, effectiveSessionId, EnrollmentStatus.ACTIVE)) {
                currentByStudent.put(e.getStudent().getId(), e);
            }
        }

        List<StudentSummary> items = result.stream()
                .map(s -> toSummary(s, currentByStudent.get(s.getId())))
                .toList();
        return PageResponse.of(result, items);
    }

    @Transactional(readOnly = true)
    public StudentDetail get(Long id) {
        return toDetail(find(id));
    }

    // ---------- ভর্তি (নতুন শিক্ষার্থী) ----------
    @Transactional
    public StudentDetail create(StudentCreateRequest request) {
        Guardian guardian = guardianRepository.findById(request.guardianId())
                .orElseThrow(() -> new NotFoundException("অভিভাবক পাওয়া যায়নি"));

        LocalDate admissionDate = request.admissionDate() != null ? request.admissionDate() : DhakaTime.today();
        if (admissionDate.isAfter(DhakaTime.today())) {
            throw bad("ভর্তির তারিখ ভবিষ্যতের হতে পারে না");
        }

        Student student = new Student();
        student.setStudentCode(codeGenerator.next(admissionDate.getYear()));
        student.setFullName(request.fullName().trim());
        student.setDateOfBirth(request.dateOfBirth());
        student.setGender(request.gender());
        student.setPhone(PhoneNumbers.normalize(request.phone()));
        student.setAddress(blankToNull(request.address()));
        student.setAdmissionDate(admissionDate);
        student.setStatus(StudentStatus.ACTIVE);
        studentRepository.saveAndFlush(student);

        StudentGuardian link = new StudentGuardian();
        link.setStudent(student);
        link.setGuardian(guardian);
        link.setRelation(request.guardianRelation());
        link.setPrimaryContact(true);
        studentGuardianRepository.saveAndFlush(link);

        auditService.log("STUDENT_CREATED", "STUDENT", student.getId(),
                student.getStudentCode() + " — " + student.getFullName());
        return toDetail(student);
    }

    @Transactional
    public StudentDetail update(Long id, StudentUpdateRequest request) {
        Student student = find(id);
        String fullName = request.fullName().trim();
        String phone = PhoneNumbers.normalize(request.phone());
        String address = blankToNull(request.address());

        Changes changes = new Changes()
                .track("নাম", student.getFullName(), fullName)
                .track("জন্মতারিখ", student.getDateOfBirth(), request.dateOfBirth())
                .track("লিঙ্গ", student.getGender(), request.gender())
                .track("ফোন", student.getPhone(), phone)
                .track("ঠিকানা", student.getAddress(), address);

        student.setFullName(fullName);
        student.setDateOfBirth(request.dateOfBirth());
        student.setGender(request.gender());
        student.setPhone(phone);
        student.setAddress(address);
        studentRepository.flush();

        if (!changes.isEmpty()) {
            auditService.log("STUDENT_UPDATED", "STUDENT", id, changes.describe());
        }
        return toDetail(student);
    }

    // ---------- অবস্থা (Status) ----------
    @Transactional
    public StudentDetail changeStatus(Long id, StudentStatus newStatus) {
        Student student = find(id);
        StudentStatus oldStatus = student.getStatus();
        if (oldStatus == newStatus) {
            return toDetail(student);
        }

        // সক্রিয় নয় এমন অবস্থায় গেলে চলতি ভর্তিও বন্ধ হয়ে যায়
        if (newStatus != StudentStatus.ACTIVE) {
            LocalDate today = DhakaTime.today();
            for (Enrollment enrollment : enrollmentRepository.findByStudentAndStatus(id, EnrollmentStatus.ACTIVE)) {
                enrollment.setStatus(newStatus == StudentStatus.GRADUATED
                        ? EnrollmentStatus.COMPLETED : EnrollmentStatus.WITHDRAWN);
                enrollment.setEndDate(today.isBefore(enrollment.getStartDate()) ? enrollment.getStartDate() : today);
            }
        }
        student.setStatus(newStatus);
        studentRepository.flush();

        auditService.log("STUDENT_STATUS_CHANGED", "STUDENT", id, oldStatus + " থেকে " + newStatus);
        return toDetail(student);
    }

    // ---------- নিরাপদ মোছা ও ফেরানো ----------
    @Transactional
    public void softDelete(Long id) {
        Student student = find(id);
        if (enrollmentRepository.existsByStudentIdAndStatus(id, EnrollmentStatus.ACTIVE)) {
            throw bad("আগে শিক্ষার্থীর চলতি ভর্তি বন্ধ করুন (ছাড়পত্র দিন), তারপর মুছুন");
        }
        student.setDeletedAt(Instant.now());
        studentRepository.flush();
        auditService.log("STUDENT_DELETED", "STUDENT", id, student.getStudentCode());
    }

    @Transactional
    public StudentDetail restore(Long id) {
        Student student = studentRepository.findDeletedById(id)
                .orElseThrow(() -> new NotFoundException("মোছা শিক্ষার্থী পাওয়া যায়নি"));
        student.setDeletedAt(null);
        studentRepository.flush();
        auditService.log("STUDENT_RESTORED", "STUDENT", id, student.getStudentCode());
        return toDetail(student);
    }

    @Transactional(readOnly = true)
    public List<StudentSummary> listDeleted() {
        return studentRepository.findDeleted().stream().map(s -> toSummary(s, null)).toList();
    }

    // ---------- অভিভাবক যুক্ত ও বিযুক্ত ----------
    @Transactional
    public StudentDetail linkGuardian(Long studentId, Long guardianId, GuardianRelation relation, boolean primary) {
        Student student = find(studentId);
        Guardian guardian = guardianRepository.findById(guardianId)
                .orElseThrow(() -> new NotFoundException("অভিভাবক পাওয়া যায়নি"));
        if (studentGuardianRepository.findByStudentIdAndGuardianId(studentId, guardianId).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "এই অভিভাবক আগে থেকেই যুক্ত");
        }

        boolean makePrimary = primary || studentGuardianRepository.countByStudentId(studentId) == 0;
        if (makePrimary) {
            studentGuardianRepository.clearPrimary(studentId, Instant.now());
        }

        StudentGuardian link = new StudentGuardian();
        link.setStudent(studentRepository.getReferenceById(studentId));
        link.setGuardian(guardianRepository.getReferenceById(guardianId));
        link.setRelation(relation);
        link.setPrimaryContact(makePrimary);
        studentGuardianRepository.saveAndFlush(link);

        auditService.log("GUARDIAN_LINKED", "STUDENT", studentId,
                guardian.getFullName() + " (" + relation + ")" + (makePrimary ? " — প্রধান" : ""));
        return toDetail(student);
    }

    @Transactional
    public StudentDetail unlinkGuardian(Long studentId, Long guardianId) {
        Student student = find(studentId);
        StudentGuardian link = studentGuardianRepository.findByStudentIdAndGuardianId(studentId, guardianId)
                .orElseThrow(() -> new NotFoundException("এই অভিভাবক এই শিক্ষার্থীর সাথে যুক্ত নন"));

        if (studentGuardianRepository.countByStudentId(studentId) <= 1) {
            throw bad("কমপক্ষে একজন অভিভাবক থাকতে হবে");
        }
        if (link.isPrimaryContact()) {
            throw bad("আগে অন্য একজনকে প্রধান অভিভাবক করুন, তারপর এঁকে সরান");
        }
        studentGuardianRepository.delete(link);
        studentGuardianRepository.flush();

        auditService.log("GUARDIAN_UNLINKED", "STUDENT", studentId, "অভিভাবক নম্বর " + guardianId);
        return toDetail(student);
    }

    @Transactional
    public StudentDetail setPrimaryGuardian(Long studentId, Long guardianId) {
        Student student = find(studentId);
        Long linkId = studentGuardianRepository.findByStudentIdAndGuardianId(studentId, guardianId)
                .orElseThrow(() -> new NotFoundException("এই অভিভাবক এই শিক্ষার্থীর সাথে যুক্ত নন"))
                .getId();

        studentGuardianRepository.clearPrimary(studentId, Instant.now());
        StudentGuardian link = studentGuardianRepository.findById(linkId).orElseThrow();
        link.setPrimaryContact(true);
        studentGuardianRepository.flush();

        auditService.log("PRIMARY_GUARDIAN_CHANGED", "STUDENT", studentId, "অভিভাবক নম্বর " + guardianId);
        return toDetail(student);
    }

    // ---------- ভেতরের সাহায্যকারী ----------
    private Student find(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("শিক্ষার্থী পাওয়া যায়নি"));
    }

    private StudentDetail toDetail(Student student) {
        List<GuardianLink> guardians = studentGuardianRepository.findAllWithGuardian(student.getId()).stream()
                .map(link -> new GuardianLink(
                        link.getGuardian().getId(),
                        link.getGuardian().getFullName(),
                        link.getGuardian().getPhone(),
                        link.getRelation(),
                        link.isPrimaryContact()))
                .toList();
        List<EnrollmentResponse> enrollments = enrollmentRepository.findAllByStudent(student.getId()).stream()
                .map(EnrollmentResponse::from)
                .toList();
        return new StudentDetail(StudentResponse.from(student), guardians, enrollments);
    }

    private StudentSummary toSummary(Student s, Enrollment current) {
        return new StudentSummary(
                s.getId(), s.getStudentCode(), s.getFullName(), s.getGender(), s.getStatus(),
                current == null ? null : current.getSchoolClass().getName(),
                current == null ? null : current.getSection().getName(),
                current == null ? null : current.getRollNumber());
    }

    private static ApiException bad(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}