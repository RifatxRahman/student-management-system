package com.sms.studentmanagement.service;

import com.sms.studentmanagement.dto.ExamDtos.ExamDetail;
import com.sms.studentmanagement.dto.ExamDtos.ExamRequest;
import com.sms.studentmanagement.dto.ExamDtos.ExamResponse;
import com.sms.studentmanagement.dto.ExamDtos.ExamSubjectRequest;
import com.sms.studentmanagement.dto.ExamDtos.ExamSubjectResponse;
import com.sms.studentmanagement.dto.ExamDtos.ExamSubjectUpdateRequest;
import com.sms.studentmanagement.dto.ExamDtos.ExamUpdateRequest;
import com.sms.studentmanagement.entity.AcademicSession;
import com.sms.studentmanagement.entity.Exam;
import com.sms.studentmanagement.entity.ExamStatus;
import com.sms.studentmanagement.entity.ExamSubject;
import com.sms.studentmanagement.entity.SchoolClass;
import com.sms.studentmanagement.entity.Subject;
import com.sms.studentmanagement.exception.ApiException;
import com.sms.studentmanagement.exception.NotFoundException;
import com.sms.studentmanagement.repository.AcademicSessionRepository;
import com.sms.studentmanagement.repository.ClassSubjectRepository;
import com.sms.studentmanagement.repository.ExamRepository;
import com.sms.studentmanagement.repository.ExamSubjectRepository;
import com.sms.studentmanagement.repository.SchoolClassRepository;
import com.sms.studentmanagement.repository.SubjectRepository;
import com.sms.studentmanagement.util.Changes;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExamService {

    private static final BigDecimal DEFAULT_FULL_MARKS = new BigDecimal("100");
    private static final BigDecimal DEFAULT_PASS_MARKS = new BigDecimal("33");

    private final ExamRepository examRepository;
    private final ExamSubjectRepository examSubjectRepository;
    private final AcademicSessionRepository sessionRepository;
    private final SchoolClassRepository classRepository;
    private final SubjectRepository subjectRepository;
    private final ClassSubjectRepository classSubjectRepository;
    private final AuditService auditService;

    // ---------- দেখা ----------
    @Transactional(readOnly = true)
    public List<ExamResponse> list(Long sessionId, Long classId) {
        return examRepository.search(sessionId, classId).stream().map(ExamResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ExamDetail get(Long id) {
        return detail(findExam(id));
    }

    // ---------- পরীক্ষা তৈরি, বদল, মোছা ----------
    @Transactional
    public ExamDetail create(ExamRequest request) {
        AcademicSession session = sessionRepository.findById(request.sessionId())
                .orElseThrow(() -> new NotFoundException("সেশন পাওয়া যায়নি"));
        SchoolClass schoolClass = classRepository.findById(request.classId())
                .orElseThrow(() -> new NotFoundException("ক্লাস পাওয়া যায়নি"));
        if (!schoolClass.isActive()) {
            throw bad("নিষ্ক্রিয় ক্লাসের জন্য পরীক্ষা বানানো যায় না");
        }
        String name = request.name().trim();
        if (examRepository.existsBySessionIdAndSchoolClassIdAndName(session.getId(), schoolClass.getId(), name)) {
            throw conflict("এই সেশনে এই ক্লাসে একই নামের পরীক্ষা আগে থেকেই আছে");
        }

        Exam exam = new Exam();
        exam.setSession(session);
        exam.setSchoolClass(schoolClass);
        exam.setName(name);
        exam.setExamType(request.type());
        exam.setStatus(ExamStatus.DRAFT);
        examRepository.save(exam);

        int copied = 0;
        if (request.copySubjects()) {
            List<ExamSubject> rows = classSubjectRepository.findAllWithSubject(schoolClass.getId()).stream()
                    .filter(cs -> cs.getSubject().isActive())
                    .map(cs -> newExamSubject(exam, cs.getSubject(), DEFAULT_FULL_MARKS, DEFAULT_PASS_MARKS, false))
                    .toList();
            examSubjectRepository.saveAll(rows);
            copied = rows.size();
        }

        auditService.log("EXAM_CREATED", "EXAM", exam.getId(),
                name + " | " + schoolClass.getName() + " | " + session.getName()
                        + (copied > 0 ? " | " + copied + "টি বিষয় যোগ" : ""));
        return detail(exam);
    }

    @Transactional
    public ExamDetail update(Long id, ExamUpdateRequest request) {
        Exam exam = findExam(id);
        requireDraft(exam);
        String name = request.name().trim();
        if (examRepository.existsBySessionIdAndSchoolClassIdAndNameAndIdNot(
                exam.getSession().getId(), exam.getSchoolClass().getId(), name, exam.getId())) {
            throw conflict("এই সেশনে এই ক্লাসে একই নামের পরীক্ষা আগে থেকেই আছে");
        }

        Changes changes = new Changes()
                .track("নাম", exam.getName(), name)
                .track("ধরন", exam.getExamType(), request.type());
        exam.setName(name);
        exam.setExamType(request.type());
        if (!changes.isEmpty()) {
            auditService.log("EXAM_UPDATED", "EXAM", exam.getId(), name + " | " + changes.describe());
        }
        return detail(exam);
    }

    @Transactional
    public void delete(Long id) {
        Exam exam = findExam(id);
        requireDraft(exam);
        // ২খ-তে এখানে "মার্কস এন্ট্রি হয়ে থাকলে মোছা যাবে না" নিয়ম যোগ হবে
        examSubjectRepository.deleteByExamId(exam.getId());
        examRepository.delete(exam);
        auditService.log("EXAM_DELETED", "EXAM", id, exam.getName());
    }

    // ---------- পরীক্ষার বিষয় ----------
    @Transactional
    public ExamDetail addSubject(Long examId, ExamSubjectRequest request) {
        Exam exam = findExam(examId);
        requireDraft(exam);
        Subject subject = subjectRepository.findById(request.subjectId())
                .orElseThrow(() -> new NotFoundException("বিষয় পাওয়া যায়নি"));
        if (!subject.isActive()) {
            throw bad("নিষ্ক্রিয় বিষয় যোগ করা যায় না");
        }
        if (!classSubjectRepository.existsBySchoolClassIdAndSubjectId(exam.getSchoolClass().getId(), subject.getId())) {
            throw bad("এই বিষয়টা " + exam.getSchoolClass().getName() + " ক্লাসের তালিকায় নেই। আগে ক্লাসে বিষয়টা যোগ করুন");
        }
        if (examSubjectRepository.existsByExamIdAndSubjectId(exam.getId(), subject.getId())) {
            throw conflict("এই বিষয় এই পরীক্ষায় আগে থেকেই আছে");
        }
        validateMarks(request.fullMarks(), request.passMarks());

        examSubjectRepository.save(
                newExamSubject(exam, subject, request.fullMarks(), request.passMarks(), request.optional()));
        auditService.log("EXAM_SUBJECT_ADDED", "EXAM", exam.getId(),
                subject.getName() + " (" + subject.getCode() + ") | ফুল " + request.fullMarks()
                        + ", পাস " + request.passMarks() + (request.optional() ? ", ঐচ্ছিক" : ""));
        return detail(exam);
    }

    @Transactional
    public ExamDetail updateSubject(Long examSubjectId, ExamSubjectUpdateRequest request) {
        ExamSubject examSubject = findExamSubject(examSubjectId);
        Exam exam = examSubject.getExam();
        requireDraft(exam);
        validateMarks(request.fullMarks(), request.passMarks());

        // ২খ-তে এখানে "এন্ট্রি হওয়া মার্কস নতুন ফুল মার্কসের বেশি হলে আটকানো" নিয়ম যোগ হবে
        Changes changes = new Changes()
                .track("ফুল মার্কস", examSubject.getFullMarks().stripTrailingZeros(), request.fullMarks().stripTrailingZeros())
                .track("পাস মার্কস", examSubject.getPassMarks().stripTrailingZeros(), request.passMarks().stripTrailingZeros())
                .track("ঐচ্ছিক", examSubject.isOptional(), request.optional());
        examSubject.setFullMarks(request.fullMarks());
        examSubject.setPassMarks(request.passMarks());
        examSubject.setOptional(request.optional());
        if (!changes.isEmpty()) {
            auditService.log("EXAM_SUBJECT_UPDATED", "EXAM", exam.getId(),
                    examSubject.getSubject().getName() + " | " + changes.describe());
        }
        return detail(exam);
    }

    @Transactional
    public ExamDetail removeSubject(Long examSubjectId) {
        ExamSubject examSubject = findExamSubject(examSubjectId);
        Exam exam = examSubject.getExam();
        requireDraft(exam);
        // ২খ-তে এখানে "মার্কস এন্ট্রি হয়ে থাকলে সরানো যাবে না" নিয়ম যোগ হবে
        examSubjectRepository.delete(examSubject);
        auditService.log("EXAM_SUBJECT_REMOVED", "EXAM", exam.getId(), examSubject.getSubject().getName());
        return detail(exam);
    }

    // ---------- ভেতরের সাহায্যকারী ----------
    private ExamDetail detail(Exam exam) {
        List<ExamSubjectResponse> subjects = examSubjectRepository.findAllWithSubject(exam.getId()).stream()
                .map(ExamSubjectResponse::from)
                .toList();
        BigDecimal total = subjects.stream()
                .filter(s -> !s.optional())
                .map(ExamSubjectResponse::fullMarks)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ExamDetail(ExamResponse.from(exam), subjects, total);
    }

    private static ExamSubject newExamSubject(
            Exam exam, Subject subject, BigDecimal fullMarks, BigDecimal passMarks, boolean optional) {
        ExamSubject row = new ExamSubject();
        row.setExam(exam);
        row.setSubject(subject);
        row.setFullMarks(fullMarks);
        row.setPassMarks(passMarks);
        row.setOptional(optional);
        return row;
    }

    private Exam findExam(Long id) {
        return examRepository.findWithRefs(id).orElseThrow(() -> new NotFoundException("পরীক্ষা পাওয়া যায়নি"));
    }

    private ExamSubject findExamSubject(Long id) {
        return examSubjectRepository.findWithRefs(id)
                .orElseThrow(() -> new NotFoundException("পরীক্ষার বিষয় পাওয়া যায়নি"));
    }

    private static void requireDraft(Exam exam) {
        if (exam.getStatus() != ExamStatus.DRAFT) {
            throw conflict("প্রকাশিত পরীক্ষা আর বদলানো যায় না");
        }
    }

    private static void validateMarks(BigDecimal fullMarks, BigDecimal passMarks) {
        if (fullMarks.signum() <= 0) {
            throw bad("ফুল মার্কস ০-র বেশি হতে হবে");
        }
        if (passMarks.signum() < 0 || passMarks.compareTo(fullMarks) > 0) {
            throw bad("পাস মার্কস ০ থেকে ফুল মার্কসের মধ্যে হতে হবে");
        }
    }

    private static ApiException bad(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }

    private static ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, message);
    }
}