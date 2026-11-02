package com.sms.studentmanagement.service;

import com.sms.studentmanagement.config.ResultProperties;
import com.sms.studentmanagement.dto.ExamDtos.ExamResponse;
import com.sms.studentmanagement.dto.ResultDtos.MissingMark;
import com.sms.studentmanagement.dto.ResultDtos.PublishResult;
import com.sms.studentmanagement.dto.ResultDtos.ResultPreview;
import com.sms.studentmanagement.dto.ResultDtos.ResultRow;
import com.sms.studentmanagement.dto.ResultDtos.ResultSubjectRow;
import com.sms.studentmanagement.entity.Enrollment;
import com.sms.studentmanagement.entity.EnrollmentStatus;
import com.sms.studentmanagement.entity.Exam;
import com.sms.studentmanagement.entity.ExamResult;
import com.sms.studentmanagement.entity.ExamStatus;
import com.sms.studentmanagement.entity.ExamSubject;
import com.sms.studentmanagement.entity.Mark;
import com.sms.studentmanagement.entity.ResultSubject;
import com.sms.studentmanagement.entity.Teacher;
import com.sms.studentmanagement.exception.ApiException;
import com.sms.studentmanagement.exception.NotFoundException;
import com.sms.studentmanagement.repository.EnrollmentRepository;
import com.sms.studentmanagement.repository.ExamRepository;
import com.sms.studentmanagement.repository.ExamResultRepository;
import com.sms.studentmanagement.repository.ExamSubjectRepository;
import com.sms.studentmanagement.repository.MarkRepository;
import com.sms.studentmanagement.repository.ResultSubjectRepository;
import com.sms.studentmanagement.repository.TeacherAssignmentRepository;
import com.sms.studentmanagement.security.Actor;
import com.sms.studentmanagement.util.GradeBand;
import com.sms.studentmanagement.util.MeritRanker;
import com.sms.studentmanagement.util.ResultCalculator;
import com.sms.studentmanagement.util.ResultCalculator.SubjectInput;
import com.sms.studentmanagement.util.ResultCalculator.SubjectResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ResultService {

    private static final int MAX_MISSING_SHOWN = 20;

    private final ExamRepository examRepository;
    private final ExamSubjectRepository examSubjectRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final MarkRepository markRepository;
    private final ExamResultRepository resultRepository;
    private final ResultSubjectRepository resultSubjectRepository;
    private final TeacherAssignmentRepository assignmentRepository;
    private final GradeScaleService gradeScaleService;
    private final ResultProperties properties;
    private final AttendanceAccess access;
    private final AuditService auditService;

    private record Computed(Enrollment enrollment, ResultCalculator.Result result) {
    }

    private record Plan(List<ExamSubject> subjects, List<Computed> computed, List<MissingMark> missing) {
    }

    private record Positions(Map<Long, Integer> inClass, Map<Long, Integer> inSection) {
    }

    // ---------- প্রকাশের আগে প্রিভিউ (শুধু Admin, Controller-এ তালা) ----------
    @Transactional(readOnly = true)
    public ResultPreview preview(Long examId) {
        Exam exam = findExam(examId);
        if (exam.getStatus() != ExamStatus.DRAFT) {
            throw conflict("এই পরীক্ষার ফল প্রকাশিত হয়ে গেছে। ফলাফল পাতা থেকে দেখুন");
        }
        List<Enrollment> roster = activeRoster(exam);
        Plan plan = plan(exam, roster);
        Positions positions = positions(plan.computed());
        Map<Long, ExamSubject> subjectById = plan.subjects().stream()
                .collect(Collectors.toMap(ExamSubject::getId, s -> s));

        List<ResultRow> rows = plan.computed().stream()
                .map(c -> previewRow(c, subjectById, positions))
                .sorted(rowOrder(false))
                .toList();
        List<MissingMark> shown = plan.missing().stream().limit(MAX_MISSING_SHOWN).toList();
        return new ResultPreview(ExamResponse.from(exam), roster.size(), plan.computed().size(),
                shown, plan.missing().size(), rows);
    }

    // ---------- প্রকাশ ----------
    @Transactional
    public PublishResult publish(Long examId) {
        Exam exam = findExam(examId);
        if (exam.getStatus() != ExamStatus.DRAFT) {
            throw conflict("এই পরীক্ষার ফল আগেই প্রকাশিত হয়েছে");
        }
        List<Enrollment> roster = activeRoster(exam);
        if (roster.isEmpty()) {
            throw bad("এই ক্লাসে এই সেশনে কোনো চলমান শিক্ষার্থী নেই");
        }
        Plan plan = plan(exam, roster);
        if (!plan.missing().isEmpty()) {
            String sample = plan.missing().stream().limit(3)
                    .map(m -> m.studentName() + " (" + m.subjectName() + ")")
                    .collect(Collectors.joining(", "));
            throw conflict(plan.missing().size() + "টা মার্কস এন্ট্রি বাকি, তাই প্রকাশ করা যাচ্ছে না। যেমন: " + sample);
        }

        store(exam, plan);
        exam.setStatus(ExamStatus.PUBLISHED);
        exam.setPublishedAt(Instant.now());

        int passed = (int) plan.computed().stream().filter(c -> c.result().passed()).count();
        int failed = plan.computed().size() - passed;
        auditService.log("EXAM_PUBLISHED", "EXAM", exam.getId(),
                exam.getName() + " | " + exam.getSchoolClass().getName() + " | "
                        + plan.computed().size() + " জন, পাস " + passed + ", ফেল " + failed);
        return new PublishResult(plan.computed().size(), passed, failed);
    }

    /** প্রকাশের পর মার্কস সংশোধন হলে MarksService এটা ডাকে। যারা আগে ফলের তালিকায় ছিল শুধু তাদের হিসাব। */
    @Transactional
    public void recalculate(Exam exam) {
        List<Enrollment> roster = resultRepository.findAllForExam(exam.getId()).stream()
                .map(ExamResult::getEnrollment)
                .toList();
        Plan plan = plan(exam, roster);
        if (!plan.missing().isEmpty()) {
            throw conflict("ফল নতুন করে হিসাব করা যায়নি: কিছু মার্কস পাওয়া যাচ্ছে না");
        }
        store(exam, plan);
    }

    // ---------- প্রকাশিত ফল দেখা ----------
    @Transactional(readOnly = true)
    public List<ResultRow> results(Actor actor, Long examId, Long sectionId) {
        Exam exam = findExam(examId);
        if (exam.getStatus() != ExamStatus.PUBLISHED) {
            throw conflict("এই পরীক্ষার ফল এখনো প্রকাশিত হয়নি");
        }
        if (!actor.admin()) {
            if (sectionId == null) {
                throw bad("সেকশন বাছুন");
            }
            Teacher teacher = access.activeTeacher(actor);
            if (!assignmentRepository.existsByTeacherIdAndSessionIdAndSectionId(
                    teacher.getId(), exam.getSession().getId(), sectionId)) {
                throw new ApiException(HttpStatus.FORBIDDEN, "এই সেকশনের ফল দেখার অনুমতি আপনার নেই");
            }
        }

        Map<Long, List<ResultSubject>> subjectsByResult = resultSubjectRepository.findAllForExam(examId).stream()
                .collect(Collectors.groupingBy(rs -> rs.getResult().getId()));
        return resultRepository.findAllForExam(examId).stream()
                .filter(r -> sectionId == null || r.getEnrollment().getSection().getId().equals(sectionId))
                .map(r -> storedRow(r, subjectsByResult.getOrDefault(r.getId(), List.of())))
                .sorted(rowOrder(sectionId != null))
                .toList();
    }

    // ---------- ভেতরের হিসাব ----------
    private Plan plan(Exam exam, List<Enrollment> roster) {
        List<ExamSubject> subjects = examSubjectRepository.findAllWithSubject(exam.getId());
        if (subjects.stream().noneMatch(s -> !s.isOptional())) {
            throw bad("পরীক্ষায় কমপক্ষে একটা আবশ্যিক বিষয় লাগবে");
        }
        Map<String, Mark> markByKey = new HashMap<>();
        for (Mark mark : markRepository.findAllForExam(exam.getId())) {
            markByKey.put(mark.getEnrollment().getId() + ":" + mark.getExamSubject().getId(), mark);
        }
        List<GradeBand> bands = gradeScaleService.currentBands();

        List<Computed> computed = new ArrayList<>();
        List<MissingMark> missing = new ArrayList<>();
        for (Enrollment enrollment : roster) {
            List<SubjectInput> inputs = new ArrayList<>();
            boolean complete = true;
            for (ExamSubject es : subjects) {
                Mark mark = markByKey.get(enrollment.getId() + ":" + es.getId());
                if (mark == null && !es.isOptional()) {
                    complete = false;
                    missing.add(new MissingMark(enrollment.getId(), enrollment.getStudent().getFullName(),
                            enrollment.getRollNumber(), enrollment.getSection().getName(), es.getSubject().getName()));
                }
                inputs.add(new SubjectInput(
                        es.getId(), es.getFullMarks(), es.getPassMarks(), es.isOptional(),
                        mark != null, mark != null && mark.isAbsent(), mark == null ? null : mark.getMarksObtained()));
            }
            if (complete) {
                computed.add(new Computed(enrollment, ResultCalculator.calculate(
                        inputs, bands, properties.getOptionalBaseGpa(), properties.getMaxGpa())));
            }
        }
        return new Plan(subjects, computed, missing);
    }

    private Positions positions(List<Computed> computed) {
        Map<Long, Integer> inClass = MeritRanker.rank(entries(computed));
        Map<Long, Integer> inSection = new HashMap<>();
        Map<Long, List<Computed>> bySection = computed.stream()
                .collect(Collectors.groupingBy(c -> c.enrollment().getSection().getId()));
        for (List<Computed> group : bySection.values()) {
            inSection.putAll(MeritRanker.rank(entries(group)));
        }
        return new Positions(inClass, inSection);
    }

    private static List<MeritRanker.Entry> entries(List<Computed> list) {
        return list.stream()
                .map(c -> new MeritRanker.Entry(
                        c.enrollment().getId(), c.result().passed(), c.result().gpa(), c.result().totalMarks()))
                .toList();
    }

    /** আগের ফল মুছে নতুন করে সেভ (প্রকাশ ও সংশোধনের পর হিসাব, দুই জায়গাতেই) */
    private void store(Exam exam, Plan plan) {
        Positions positions = positions(plan.computed());
        Map<Long, ExamSubject> subjectById = plan.subjects().stream()
                .collect(Collectors.toMap(ExamSubject::getId, s -> s));

        resultSubjectRepository.deleteByResultExamId(exam.getId());
        resultSubjectRepository.flush();
        resultRepository.deleteByExamId(exam.getId());
        resultRepository.flush();

        List<ResultSubject> subjectRows = new ArrayList<>();
        for (Computed c : plan.computed()) {
            ResultCalculator.Result calc = c.result();
            ExamResult row = new ExamResult();
            row.setExam(exam);
            row.setEnrollment(c.enrollment());
            row.setTotalMarks(calc.totalMarks().setScale(2, RoundingMode.HALF_UP));
            row.setTotalFullMarks(calc.totalFullMarks().setScale(2, RoundingMode.HALF_UP));
            row.setAverageMarks(calc.averageMarks());
            row.setGpa(calc.gpa());
            row.setGrade(calc.grade());
            row.setPassed(calc.passed());
            row.setPositionInClass(positions.inClass().get(c.enrollment().getId()));
            row.setPositionInSection(positions.inSection().get(c.enrollment().getId()));
            resultRepository.save(row);

            for (SubjectResult sr : calc.subjects()) {
                if (!sr.counted()) {
                    continue;
                }
                ResultSubject rs = new ResultSubject();
                rs.setResult(row);
                rs.setExamSubject(subjectById.get(sr.examSubjectId()));
                rs.setMarksObtained(sr.marks());
                rs.setAbsent(sr.absent());
                rs.setGrade(sr.grade());
                rs.setGradePoint(sr.gradePoint());
                rs.setPassed(sr.passed());
                subjectRows.add(rs);
            }
        }
        resultSubjectRepository.saveAll(subjectRows);
    }

    // ---------- ResultRow বানানো ----------
    private ResultRow previewRow(Computed c, Map<Long, ExamSubject> subjectById, Positions positions) {
        Enrollment e = c.enrollment();
        ResultCalculator.Result r = c.result();
        List<ResultSubjectRow> subjects = r.subjects().stream()
                .filter(SubjectResult::counted)
                .map(sr -> {
                    ExamSubject es = subjectById.get(sr.examSubjectId());
                    return new ResultSubjectRow(
                            es.getId(), es.getSubject().getName(), es.getSubject().getCode(), es.isOptional(),
                            es.getFullMarks(), es.getPassMarks(), sr.marks(), sr.absent(),
                            sr.grade(), sr.gradePoint(), sr.passed());
                })
                .sorted(Comparator.comparing(ResultSubjectRow::subjectName))
                .toList();
        return new ResultRow(
                null, e.getId(), e.getStudent().getId(), e.getStudent().getStudentCode(), e.getStudent().getFullName(),
                e.getSection().getId(), e.getSection().getName(), e.getRollNumber(),
                r.totalMarks(), r.totalFullMarks(), r.averageMarks(), r.gpa(), r.grade(), r.passed(),
                positions.inClass().get(e.getId()), positions.inSection().get(e.getId()), subjects);
    }

    private ResultRow storedRow(ExamResult r, List<ResultSubject> storedSubjects) {
        Enrollment e = r.getEnrollment();
        List<ResultSubjectRow> subjects = storedSubjects.stream()
                .map(rs -> {
                    ExamSubject es = rs.getExamSubject();
                    return new ResultSubjectRow(
                            es.getId(), es.getSubject().getName(), es.getSubject().getCode(), es.isOptional(),
                            es.getFullMarks(), es.getPassMarks(), rs.getMarksObtained(), rs.isAbsent(),
                            rs.getGrade(), rs.getGradePoint(), rs.isPassed());
                })
                .sorted(Comparator.comparing(ResultSubjectRow::subjectName))
                .toList();
        return new ResultRow(
                r.getId(), e.getId(), e.getStudent().getId(), e.getStudent().getStudentCode(), e.getStudent().getFullName(),
                e.getSection().getId(), e.getSection().getName(), e.getRollNumber(),
                r.getTotalMarks(), r.getTotalFullMarks(), r.getAverageMarks(), r.getGpa(), r.getGrade(), r.isPassed(),
                r.getPositionInClass(), r.getPositionInSection(), subjects);
    }

    /** পাস করারা আগে (অবস্থান অনুযায়ী), ফেলরা পরে (রোল অনুযায়ী) */
    private static Comparator<ResultRow> rowOrder(boolean bySection) {
        return Comparator.comparing((ResultRow r) -> !r.passed())
                .thenComparing(
                        (ResultRow r) -> bySection ? r.positionInSection() : r.positionInClass(),
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparingInt(ResultRow::rollNumber);
    }

    private List<Enrollment> activeRoster(Exam exam) {
        return enrollmentRepository.findInClassAndSession(
                exam.getSession().getId(), exam.getSchoolClass().getId(), EnrollmentStatus.ACTIVE);
    }

    private Exam findExam(Long id) {
        return examRepository.findWithRefs(id).orElseThrow(() -> new NotFoundException("পরীক্ষা পাওয়া যায়নি"));
    }

    private static ApiException bad(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }

    private static ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, message);
    }
}