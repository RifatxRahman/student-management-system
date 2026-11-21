package com.sms.studentmanagement.service;

import com.sms.studentmanagement.dto.MarkDtos.MarkEntryRequest;
import com.sms.studentmanagement.dto.MarkDtos.MarkRow;
import com.sms.studentmanagement.dto.MarkDtos.MarkSheet;
import com.sms.studentmanagement.dto.MarkDtos.MarkTarget;
import com.sms.studentmanagement.dto.MarkDtos.SaveMarksRequest;
import com.sms.studentmanagement.dto.MarkDtos.SaveMarksResult;
import com.sms.studentmanagement.entity.Enrollment;
import com.sms.studentmanagement.entity.EnrollmentStatus;
import com.sms.studentmanagement.entity.Exam;
import com.sms.studentmanagement.entity.ExamStatus;
import com.sms.studentmanagement.entity.ExamSubject;
import com.sms.studentmanagement.entity.Mark;
import com.sms.studentmanagement.entity.Section;
import com.sms.studentmanagement.entity.Teacher;
import com.sms.studentmanagement.entity.TeacherAssignment;
import com.sms.studentmanagement.exception.ApiException;
import com.sms.studentmanagement.exception.NotFoundException;
import com.sms.studentmanagement.repository.EnrollmentRepository;
import com.sms.studentmanagement.repository.ExamRepository;
import com.sms.studentmanagement.repository.ExamSubjectRepository;
import com.sms.studentmanagement.repository.MarkRepository;
import com.sms.studentmanagement.repository.SectionRepository;
import com.sms.studentmanagement.repository.TeacherAssignmentRepository;
import com.sms.studentmanagement.security.Actor;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MarksService {

    private static final int MIN_REASON_LENGTH = 5;

    private final ExamRepository examRepository;
    private final ExamSubjectRepository examSubjectRepository;
    private final SectionRepository sectionRepository;
    private final TeacherAssignmentRepository assignmentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final MarkRepository markRepository;
    private final AttendanceAccess access;
    private final ResultService resultService;
    private final AuditService auditService;

    // ---------- কোন সেকশন-বিষয়ে মার্কস দেওয়া যাবে ----------
    @Transactional(readOnly = true)
    public List<MarkTarget> targets(Actor actor, Long examId) {
        Exam exam = findExam(examId);
        List<ExamSubject> subjects = examSubjectRepository.findAllWithSubject(exam.getId());
        List<MarkTarget> result = new ArrayList<>();

        if (actor.admin()) {
            List<Section> sections = sectionRepository.findBySchoolClassIdOrderByName(exam.getSchoolClass().getId());
            for (int i = 0; i < sections.size(); i++) {
                Section section = sections.get(i);
                if (!section.isActive()) {
                    continue;
                }
                for (int j = 0; j < subjects.size(); j++) {
                    result.add(target(section, subjects.get(j)));
                }
            }
            return result;
        }

        Teacher teacher = access.activeTeacher(actor);
        Map<Long, ExamSubject> bySubject = new HashMap<>();
        for (int i = 0; i < subjects.size(); i++) {
            ExamSubject es = subjects.get(i);
            bySubject.put(es.getSubject().getId(), es);
        }

        List<TeacherAssignment> assignments = assignmentRepository.findAllForTeacher(teacher.getId(), exam.getSession().getId());
        for (int i = 0; i < assignments.size(); i++) {
            TeacherAssignment ta = assignments.get(i);
            ExamSubject es = bySubject.get(ta.getSubject().getId());
            boolean sameClass = ta.getSection().getSchoolClass().getId().equals(exam.getSchoolClass().getId());
            if (es != null && sameClass) {
                result.add(target(ta.getSection(), es));
            }
        }
        return result;
    }

    // ---------- মার্কসের খাতা (দেখা) ----------
    @Transactional(readOnly = true)
    public MarkSheet sheet(Actor actor, Long examId, Long sectionId, Long subjectId) {
        Exam exam = findExam(examId);
        ExamSubject es = findExamSubject(examId, subjectId);
        Section section = findSection(sectionId);
        requireSectionInExamClass(exam, section);
        requireEntryAccess(actor, exam, section.getId(), subjectId);

        Map<Long, Mark> saved = savedByEnrollment(es.getId(), section.getId());
        List<MarkRow> rows = new ArrayList<>();
        List<Enrollment> roster = sectionRoster(exam, section.getId());
        for (int i = 0; i < roster.size(); i++) {
            Enrollment e = roster.get(i);
            Mark mark = saved.get(e.getId());
            rows.add(new MarkRow(
                    e.getId(), e.getStudent().getId(), e.getStudent().getStudentCode(), e.getStudent().getFullName(),
                    e.getRollNumber(), mark == null ? null : mark.getMarksObtained(),
                    mark != null && mark.isAbsent(), mark != null));
        }

        boolean published = exam.getStatus() == ExamStatus.PUBLISHED;
        boolean editable = !published || actor.admin();
        String lock = (published && !actor.admin()) ? "ফল প্রকাশিত হয়ে গেছে। মার্কস বদলাতে Admin-কে বলুন।" : null;
        return new MarkSheet(
                exam.getId(), exam.getName(), exam.getStatus(),
                section.getId(), section.getName(), section.getSchoolClass().getName(),
                es.getSubject().getId(), es.getSubject().getName(), es.getFullMarks(), es.getPassMarks(), es.isOptional(),
                editable, lock, published, rows);
    }

    // ---------- মার্কস সেভ ও সংশোধন ----------
    @Transactional
    public SaveMarksResult save(Actor actor, SaveMarksRequest request) {
        Exam exam = findExam(request.examId());
        ExamSubject es = findExamSubject(exam.getId(), request.subjectId());
        Section section = findSection(request.sectionId());
        requireSectionInExamClass(exam, section);

        // (১) মালিকানা: এই শিক্ষকের কি এই সেকশনের এই বিষয়ে বরাদ্দ আছে?
        requireEntryAccess(actor, exam, section.getId(), request.subjectId());

        // (২) প্রকাশের পর শুধু Admin
        boolean published = exam.getStatus() == ExamStatus.PUBLISHED;
        if (published && !actor.admin()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ফল প্রকাশিত হয়ে গেছে। মার্কস বদলাতে Admin-কে বলুন");
        }

        // (৩) যাদের মার্কস পাঠানো হলো তারা কি সত্যিই এই সেকশনের চলমান শিক্ষার্থী? নম্বর কি ঠিক?
        Map<Long, Enrollment> roster = new HashMap<>();
        List<Enrollment> rosterList = sectionRoster(exam, section.getId());
        for (int i = 0; i < rosterList.size(); i++) {
            Enrollment e = rosterList.get(i);
            roster.put(e.getId(), e);
        }
        Set<Long> seen = new HashSet<>();
        List<MarkEntryRequest> entries = request.entries();
        for (int i = 0; i < entries.size(); i++) {
            MarkEntryRequest entry = entries.get(i);
            if (!roster.containsKey(entry.enrollmentId())) {
                throw bad("তালিকার বাইরের একজন শিক্ষার্থীর মার্কস পাঠানো হয়েছে");
            }
            if (!seen.add(entry.enrollmentId())) {
                throw bad("একই শিক্ষার্থীর মার্কস দুবার পাঠানো হয়েছে");
            }
            validateEntry(entry, es.getFullMarks());
        }

        // (৪) প্রকাশের পর যেকোনো বদল বা নতুন এন্ট্রিতে কারণ লাগবে
        Map<Long, Mark> saved = savedByEnrollment(es.getId(), section.getId());
        boolean anyChange = false;
        for (int i = 0; i < entries.size(); i++) {
            MarkEntryRequest entry = entries.get(i);
            Mark existing = saved.get(entry.enrollmentId());
            if (existing == null || differs(existing, entry)) {
                anyChange = true;
                break;
            }
        }
        String reason = request.reason() == null ? "" : request.reason().trim();
        if (published && anyChange && reason.length() < MIN_REASON_LENGTH) {
            throw bad("প্রকাশিত ফলের মার্কস বদলাতে কারণ লিখতে হবে (কমপক্ষে " + MIN_REASON_LENGTH + " অক্ষর)");
        }

        // (৫) সেভ ও অডিট
        List<Mark> toCreate = new ArrayList<>();
        int changed = 0;
        int unchanged = 0;
        for (int i = 0; i < entries.size(); i++) {
            MarkEntryRequest entry = entries.get(i);
            Enrollment enrollment = roster.get(entry.enrollmentId());
            Mark existing = saved.get(entry.enrollmentId());
            if (existing == null) {
                Mark mark = new Mark();
                mark.setExamSubject(es);
                mark.setEnrollment(enrollment);
                apply(mark, entry, actor);
                toCreate.add(mark);
                if (published) {
                    auditService.log("MARKS_CORRECTED", "EXAM", exam.getId(),
                            describe(enrollment, es, "নেই", entry) + " | কারণ: " + reason);
                }
            } else if (differs(existing, entry)) {
                String line = describe(enrollment, es, valueText(existing), entry);
                apply(existing, entry, actor);
                changed++;
                if (published) {
                    auditService.log("MARKS_CORRECTED", "EXAM", exam.getId(), line + " | কারণ: " + reason);
                }
            } else {
                unchanged++;
            }
        }

        try {
            markRepository.saveAll(toCreate);
            markRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "একই সময়ে অন্য কেউ এই বিষয়ের মার্কস দিয়েছেন। পাতা রিফ্রেশ করে আবার চেষ্টা করুন।");
        }

        if (!published && (!toCreate.isEmpty() || changed > 0)) {
            auditService.log("MARKS_SAVED", "EXAM", exam.getId(),
                    exam.getName() + " | " + section.getSchoolClass().getName() + " " + section.getName()
                            + " | " + es.getSubject().getName() + " | নতুন " + toCreate.size() + ", বদল " + changed);
        }
        if (published && (!toCreate.isEmpty() || changed > 0)) {
            resultService.recalculate(exam);
        }
        return new SaveMarksResult(toCreate.size(), changed, unchanged);
    }

    // ---------- ভেতরের সাহায্যকারী ----------
    private void requireEntryAccess(Actor actor, Exam exam, Long sectionId, Long subjectId) {
        if (actor.admin()) {
            return;
        }
        Teacher teacher = access.activeTeacher(actor);
        if (!assignmentRepository.existsByTeacherIdAndSessionIdAndSectionIdAndSubjectId(
                teacher.getId(), exam.getSession().getId(), sectionId, subjectId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "এই সেকশনের এই বিষয়ে মার্কস দেওয়ার অনুমতি আপনার নেই");
        }
    }

    private static void requireSectionInExamClass(Exam exam, Section section) {
        if (!section.getSchoolClass().getId().equals(exam.getSchoolClass().getId())) {
            throw bad("এই সেকশন এই পরীক্ষার ক্লাসের নয়");
        }
    }

    private static void validateEntry(MarkEntryRequest entry, BigDecimal fullMarks) {
        if (entry.absent()) {
            if (entry.marks() != null) {
                throw bad("অনুপস্থিত হলে নম্বর দেওয়া যাবে না");
            }
            return;
        }
        if (entry.marks() == null) {
            throw bad("নম্বর দিন, অথবা অনুপস্থিত চিহ্ন দিন");
        }
        if (entry.marks().signum() < 0) {
            throw bad("নম্বর ঋণাত্মক হতে পারে না");
        }
        if (entry.marks().compareTo(fullMarks) > 0) {
            throw bad("নম্বর ফুল মার্কস (" + fullMarks.stripTrailingZeros().toPlainString() + ")-এর বেশি হতে পারে না");
        }
    }

    private static boolean differs(Mark existing, MarkEntryRequest entry) {
        if (existing.isAbsent() != entry.absent()) {
            return true;
        }
        return !entry.absent() && existing.getMarksObtained().compareTo(entry.marks()) != 0;
    }

    private static void apply(Mark mark, MarkEntryRequest entry, Actor actor) {
        mark.setAbsent(entry.absent());
        mark.setMarksObtained(entry.absent() ? null : entry.marks().setScale(2, RoundingMode.HALF_UP));
        mark.setEnteredBy(actor.userId());
    }

    private static String valueText(Mark mark) {
        return mark.isAbsent() ? "অনুপস্থিত" : mark.getMarksObtained().stripTrailingZeros().toPlainString();
    }

    private static String describe(Enrollment e, ExamSubject es, String oldText, MarkEntryRequest entry) {
        String newText = entry.absent() ? "অনুপস্থিত" : entry.marks().stripTrailingZeros().toPlainString();
        return "রোল " + e.getRollNumber() + " (" + e.getStudent().getStudentCode() + ") "
                + es.getSubject().getName() + ": " + oldText + " → " + newText;
    }

    private MarkTarget target(Section section, ExamSubject es) {
        return new MarkTarget(
                section.getId(), section.getName(), section.getSchoolClass().getName(),
                es.getSubject().getId(), es.getSubject().getName(), es.getSubject().getCode(),
                es.getFullMarks(), es.getPassMarks(), es.isOptional());
    }

    private List<Enrollment> sectionRoster(Exam exam, Long sectionId) {
        return enrollmentRepository.findInClassAndSession(
                        exam.getSession().getId(), exam.getSchoolClass().getId(), EnrollmentStatus.ACTIVE).stream()
                .filter(e -> e.getSection().getId().equals(sectionId))
                .toList();
    }

    private Map<Long, Mark> savedByEnrollment(Long examSubjectId, Long sectionId) {
        Map<Long, Mark> map = new HashMap<>();
        List<Mark> marks = markRepository.findForSectionAndExamSubject(examSubjectId, sectionId);
        for (int i = 0; i < marks.size(); i++) {
            Mark mark = marks.get(i);
            map.put(mark.getEnrollment().getId(), mark);
        }
        return map;
    }

    // ---------- মার্কস এন্ট্রি অগ্রগতি (Progress / Status) ----------
    @Transactional(readOnly = true)
    public com.sms.studentmanagement.dto.MarkDtos.ExamMarksProgressResponse progress(Long examId) {
        Exam exam = findExam(examId);
        List<ExamSubject> examSubjects = examSubjectRepository.findAllWithSubject(exam.getId());
        List<Section> sections = sectionRepository.findBySchoolClassIdOrderByName(exam.getSchoolClass().getId());

        List<com.sms.studentmanagement.dto.MarkDtos.SubjectMarksProgress> progressList = new ArrayList<>();
        int totalSheets = 0;
        int completedSheets = 0;
        int totalExpectedMarks = 0;
        int totalEnteredMarks = 0;

        for (int i = 0; i < sections.size(); i++) {
            Section section = sections.get(i);
            if (!section.isActive()) {
                continue;
            }

            List<Enrollment> roster = sectionRoster(exam, section.getId());
            int studentCount = roster.size();

            for (int j = 0; j < examSubjects.size(); j++) {
                ExamSubject es = examSubjects.get(j);
                totalSheets++;

                Map<Long, Mark> saved = savedByEnrollment(es.getId(), section.getId());
                int entered = 0;
                int absent = 0;

                for (int k = 0; k < roster.size(); k++) {
                    Mark m = saved.get(roster.get(k).getId());
                    if (m != null) {
                        if (m.isAbsent()) {
                            absent++;
                        } else {
                            entered++;
                        }
                    }
                }

                int totalRecorded = entered + absent;
                int pending = Math.max(0, studentCount - totalRecorded);
                totalExpectedMarks += studentCount;
                totalEnteredMarks += totalRecorded;

                BigDecimal percentage = BigDecimal.ZERO;
                if (studentCount > 0) {
                    percentage = BigDecimal.valueOf(totalRecorded)
                            .multiply(BigDecimal.valueOf(100))
                            .divide(BigDecimal.valueOf(studentCount), 2, RoundingMode.HALF_UP);
                }

                String status = (totalRecorded == studentCount && studentCount > 0) ? "COMPLETED" :
                        (totalRecorded > 0 ? "PARTIAL" : "NOT_STARTED");
                if ("COMPLETED".equals(status)) {
                    completedSheets++;
                }

                String teacherName = "অনির্ধারিত";
                java.util.Optional<TeacherAssignment> ta = assignmentRepository
                        .findBySessionIdAndSectionIdAndSubjectId(exam.getSession().getId(), section.getId(), es.getSubject().getId());
                if (ta.isPresent()) {
                    teacherName = ta.get().getTeacher().getFullName();
                }

                progressList.add(new com.sms.studentmanagement.dto.MarkDtos.SubjectMarksProgress(
                        section.getId(),
                        section.getName(),
                        es.getSubject().getId(),
                        es.getSubject().getName(),
                        es.getSubject().getCode(),
                        teacherName,
                        studentCount,
                        entered,
                        absent,
                        pending,
                        percentage,
                        status));
            }
        }

        BigDecimal overallPercentage = BigDecimal.ZERO;
        if (totalExpectedMarks > 0) {
            overallPercentage = BigDecimal.valueOf(totalEnteredMarks)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalExpectedMarks), 2, RoundingMode.HALF_UP);
        }

        return new com.sms.studentmanagement.dto.MarkDtos.ExamMarksProgressResponse(
                exam.getId(),
                exam.getName(),
                exam.getSchoolClass().getName(),
                exam.getSession().getName(),
                totalSheets,
                completedSheets,
                totalExpectedMarks,
                totalEnteredMarks,
                overallPercentage,
                progressList);
    }

    private Exam findExam(Long id) {
        return examRepository.findWithRefs(id).orElseThrow(() -> new NotFoundException("পরীক্ষা পাওয়া যায়নি"));
    }

    private ExamSubject findExamSubject(Long examId, Long subjectId) {
        return examSubjectRepository.findByExamIdAndSubjectId(examId, subjectId)
                .orElseThrow(() -> new NotFoundException("এই বিষয় এই পরীক্ষায় নেই"));
    }

    private Section findSection(Long id) {
        return sectionRepository.findById(id).orElseThrow(() -> new NotFoundException("সেকশন পাওয়া যায়নি"));
    }

    private static ApiException bad(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }
}