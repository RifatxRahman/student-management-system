package com.sms.studentmanagement.service;

import com.sms.studentmanagement.config.AttendanceProperties;
import com.sms.studentmanagement.dto.AttendanceDtos.MarkEntry;
import com.sms.studentmanagement.dto.AttendanceDtos.MarkRequest;
import com.sms.studentmanagement.dto.AttendanceDtos.MarkResult;
import com.sms.studentmanagement.dto.AttendanceDtos.SectionOption;
import com.sms.studentmanagement.dto.AttendanceDtos.Sheet;
import com.sms.studentmanagement.dto.AttendanceDtos.SheetRow;
import com.sms.studentmanagement.entity.Attendance;
import com.sms.studentmanagement.entity.AttendanceStatus;
import com.sms.studentmanagement.entity.Enrollment;
import com.sms.studentmanagement.entity.Section;
import com.sms.studentmanagement.entity.Teacher;
import com.sms.studentmanagement.entity.TeacherAssignment;
import com.sms.studentmanagement.exception.ApiException;
import com.sms.studentmanagement.exception.NotFoundException;
import com.sms.studentmanagement.repository.AttendanceRepository;
import com.sms.studentmanagement.repository.EnrollmentRepository;
import com.sms.studentmanagement.repository.SectionRepository;
import com.sms.studentmanagement.repository.TeacherAssignmentRepository;
import com.sms.studentmanagement.security.Actor;
import com.sms.studentmanagement.util.DhakaTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private static final int MIN_REASON_LENGTH = 5;

    private final SectionRepository sectionRepository;
    private final TeacherAssignmentRepository assignmentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AttendanceRepository attendanceRepository;
    private final AttendanceAccess access;
    private final AttendanceCalendar calendar;
    private final AttendanceProperties properties;
    private final AuditService auditService;
    private final NotificationService notificationService;

    // ---------- কোন সেকশনের হাজিরা নেওয়া যাবে ----------
    @Transactional(readOnly = true)
    public List<SectionOption> mySections(Actor actor, LocalDate date) {
        LocalDate day = date != null ? date : DhakaTime.today();

        if (actor.admin()) {
            return sectionRepository.findAll().stream()
                    .filter(s -> s.isActive() && s.getSchoolClass().isActive())
                    .sorted(Comparator
                            .comparing((Section s) -> s.getSchoolClass().getDisplayOrder())
                            .thenComparing(Section::getName))
                    .map(SectionOption::from)
                    .toList();
        }

        Teacher teacher = access.activeTeacher(actor);
        List<TeacherAssignment> assignments = assignmentRepository.findForTeacherOnDate(teacher.getId(), day);
        Map<Long, SectionOption> unique = new LinkedHashMap<>();
        for (int i = 0; i < assignments.size(); i++) {
            TeacherAssignment ta = assignments.get(i);
            unique.putIfAbsent(ta.getSection().getId(), SectionOption.from(ta.getSection()));
        }
        return List.copyOf(unique.values());
    }

    // ---------- হাজিরার খাতা (দেখা) ----------
    @Transactional(readOnly = true)
    public Sheet sheet(Actor actor, Long sectionId, LocalDate date) {
        Section section = findSection(sectionId);
        access.requireSection(actor, section.getId(), date, date);

        Map<Long, Attendance> saved = savedByEnrollment(section.getId(), date);
        boolean isFinalized = false;
        List<Attendance> savedList = attendanceRepository.findForSectionOnDate(section.getId(), date);
        for (int i = 0; i < savedList.size(); i++) {
            if (savedList.get(i).isFinalized()) {
                isFinalized = true;
                break;
            }
        }

        List<Enrollment> enrolled = enrollmentRepository.findInSection(section.getId(), date, date);
        List<SheetRow> rows = new ArrayList<>();
        for (int i = 0; i < enrolled.size(); i++) {
            Enrollment e = enrolled.get(i);
            Attendance a = saved.get(e.getId());
            rows.add(SheetRow.from(e, a == null ? null : a.getStatus(), a == null ? null : a.getRemark()));
        }

        String lock = lockReason(actor, date, isFinalized);
        return new Sheet(
                section.getId(), section.getName(), section.getSchoolClass().getName(), date,
                lock == null, isFinalized, lock, !saved.isEmpty(), rows);
    }

    // ---------- হাজিরা নেওয়া ও সংশোধন ----------
    @Transactional
    public MarkResult mark(Actor actor, MarkRequest request) {
        Section section = findSection(request.sectionId());
        LocalDate date = request.date();

        // (১) অনুমতি
        access.requireSection(actor, section.getId(), date, date);

        Map<Long, Attendance> saved = savedByEnrollment(section.getId(), date);
        boolean isFinalized = false;
        List<Attendance> savedList = attendanceRepository.findForSectionOnDate(section.getId(), date);
        for (int i = 0; i < savedList.size(); i++) {
            if (savedList.get(i).isFinalized()) {
                isFinalized = true;
                break;
            }
        }

        // (২) দিনটা কি খোলা, লক কি না?
        String lock = lockReason(actor, date, isFinalized);
        if (lock != null) {
            throw bad(lock);
        }

        // (৩) যাদের পাঠানো হলো তারা কি সত্যিই এই সেকশনের ওই দিনের তালিকায়?
        List<Enrollment> enrolled = enrollmentRepository.findInSection(section.getId(), date, date);
        Map<Long, Enrollment> roster = new HashMap<>();
        for (int i = 0; i < enrolled.size(); i++) {
            Enrollment e = enrolled.get(i);
            roster.put(e.getId(), e);
        }
        if (roster.isEmpty()) {
            throw bad("এই তারিখে এই সেকশনে কোনো শিক্ষার্থী নেই");
        }
        Set<Long> seen = new HashSet<>();
        List<MarkEntry> entries = request.entries();
        for (int i = 0; i < entries.size(); i++) {
            MarkEntry entry = entries.get(i);
            if (!roster.containsKey(entry.enrollmentId())) {
                throw bad("তালিকার বাইরের একজন শিক্ষার্থীর হাজিরা পাঠানো হয়েছে");
            }
            if (!seen.add(entry.enrollmentId())) {
                throw bad("একই শিক্ষার্থীর হাজিরা দুবার পাঠানো হয়েছে");
            }
        }

        // (৪) আগের হাজিরা বদলাচ্ছে? তাহলে কারণ লাগবে
        boolean anyCorrection = false;
        for (int i = 0; i < entries.size(); i++) {
            MarkEntry entry = entries.get(i);
            Attendance existing = saved.get(entry.enrollmentId());
            if (existing != null && differs(existing, entry)) {
                anyCorrection = true;
                break;
            }
        }
        String reason = request.reason() == null ? "" : request.reason().trim();
        if (anyCorrection && reason.length() < MIN_REASON_LENGTH) {
            throw bad("আগে নেওয়া হাজিরা বদলাতে কারণ লিখতে হবে (কমপক্ষে " + MIN_REASON_LENGTH + " অক্ষর)");
        }

        // (৫) সেভ ও Audit Log
        List<Attendance> createdRows = new ArrayList<>();
        List<Enrollment> becameAbsent = new ArrayList<>();
        List<Enrollment> noLongerAbsent = new ArrayList<>();
        int changed = 0;
        int unchanged = 0;
        for (int i = 0; i < entries.size(); i++) {
            MarkEntry entry = entries.get(i);
            Enrollment enrollment = roster.get(entry.enrollmentId());
            Attendance existing = saved.get(entry.enrollmentId());
            String remark = normalizeRemark(entry.remark());

            if (existing == null) {
                Attendance row = new Attendance();
                row.setEnrollment(enrollment);
                row.setAttendanceDate(date);
                row.setStatus(entry.status());
                row.setRemark(remark);
                row.setMarkedBy(actor.userId());
                createdRows.add(row);
                if (entry.status() == AttendanceStatus.ABSENT) {
                    becameAbsent.add(enrollment);
                }
            } else if (differs(existing, entry)) {
                String line = describeChange(enrollment, date, existing, entry);
                AttendanceStatus oldStatus = existing.getStatus();
                existing.setStatus(entry.status());
                existing.setRemark(remark);
                existing.setMarkedBy(actor.userId());
                changed++;
                auditService.log("ATTENDANCE_CORRECTED", "ATTENDANCE", existing.getId(),
                        line + " | কারণ: " + reason);
                if (oldStatus != AttendanceStatus.ABSENT && entry.status() == AttendanceStatus.ABSENT) {
                    becameAbsent.add(enrollment);
                } else if (oldStatus == AttendanceStatus.ABSENT && entry.status() != AttendanceStatus.ABSENT) {
                    noLongerAbsent.add(enrollment);
                }
            } else {
                unchanged++;
            }
        }

        try {
            attendanceRepository.saveAll(createdRows);
            attendanceRepository.flush();
        } catch (DataIntegrityViolationException e) {
            // দুজন একসাথে একই দিনের প্রথম হাজিরা নিতে গেলে ডেটাবেস একজনকে আটকায়
            throw new ApiException(HttpStatus.CONFLICT,
                    "একই সময়ে অন্য কেউ এই দিনের হাজিরা নিয়েছেন। পাতা রিফ্রেশ করে আবার চেষ্টা করুন।");
        }

        if (!createdRows.isEmpty()) {
            auditService.log("ATTENDANCE_MARKED", "SECTION", section.getId(),
                    date + " | " + section.getSchoolClass().getName() + " " + section.getName()
                            + " | " + createdRows.size() + " জন (" + summarize(createdRows) + ")");
        }

        // (৬) অভিভাবকের বার্তা: নতুন Absent-দের জন্য জমা, Absent থেকে সরে যাওয়াদের না-যাওয়া বার্তা বাতিল
        for (int i = 0; i < becameAbsent.size(); i++) {
            notificationService.queueAbsence(becameAbsent.get(i).getStudent(), date);
        }
        for (int i = 0; i < noLongerAbsent.size(); i++) {
            notificationService.cancelUnsentAbsence(noLongerAbsent.get(i).getStudent().getId(), date);
        }

        return new MarkResult(createdRows.size(), changed, unchanged);
    }

    // ---------- হাজিরা ফাইনাল ও লক করা ----------
    @Transactional
    public void finalizeAttendance(Actor actor, Long sectionId, LocalDate date) {
        Section section = findSection(sectionId);
        access.requireSection(actor, section.getId(), date, date);

        int count = attendanceRepository.finalizeBySectionAndDate(section.getId(), date);
        if (count == 0) {
            throw bad("এই দিনের হাজিরা নেওয়া হয়নি, তাই ফাইনাল করা যাবে না");
        }

        auditService.log("ATTENDANCE_FINALIZED", "SECTION", section.getId(),
                date + " তারিখের হাজিরা ফাইনাল ও লক করা হয়েছে।");
    }

    // ---------- ভেতরের সাহায্যকারী ----------
    /** দিনটা বন্ধ বা লক হলে কারণ ফেরায়, খোলা হলে null। */
    private String lockReason(Actor actor, LocalDate date, boolean isFinalized) {
        LocalDate today = DhakaTime.today();
        if (date.isAfter(today)) {
            return "ভবিষ্যতের তারিখে হাজিরা নেওয়া যায় না";
        }
        String closed = calendar.closedReason(date);
        if (closed != null) {
            return closed + " (এই দিনে হাজিরা নেই)";
        }
        if (isFinalized && !actor.admin()) {
            return "এই দিনের হাজিরা ফাইনাল ও লক করা হয়েছে। পরিবর্তনের জন্য Admin-এর অনুমতি প্রয়োজন।";
        }
        int window = properties.getEditWindowDays();
        if (!actor.admin() && date.isBefore(today.minusDays(window))) {
            return "সময়সীমা (" + window + " দিন) পেরিয়ে গেছে। বদলাতে Admin-কে বলুন।";
        }
        return null;
    }

    private Map<Long, Attendance> savedByEnrollment(Long sectionId, LocalDate date) {
        Map<Long, Attendance> map = new HashMap<>();
        List<Attendance> list = attendanceRepository.findForSectionOnDate(sectionId, date);
        for (int i = 0; i < list.size(); i++) {
            Attendance a = list.get(i);
            map.put(a.getEnrollment().getId(), a);
        }
        return map;
    }

    private boolean differs(Attendance existing, MarkEntry entry) {
        return existing.getStatus() != entry.status()
                || !Objects.equals(existing.getRemark(), normalizeRemark(entry.remark()));
    }

    private String describeChange(Enrollment e, LocalDate date, Attendance old, MarkEntry entry) {
        String text = "রোল " + e.getRollNumber() + " (" + e.getStudent().getStudentCode() + ") " + date
                + ": " + old.getStatus() + " → " + entry.status();
        if (old.getStatus() == entry.status()) {
            text += " (মন্তব্য বদলেছে)";
        }
        return text;
    }

    private String summarize(List<Attendance> rows) {
        long present = 0;
        long late = 0;
        long absent = 0;
        long leave = 0;
        for (int i = 0; i < rows.size(); i++) {
            AttendanceStatus s = rows.get(i).getStatus();
            if (s == AttendanceStatus.PRESENT) present++;
            else if (s == AttendanceStatus.LATE) late++;
            else if (s == AttendanceStatus.ABSENT) absent++;
            else if (s == AttendanceStatus.LEAVE) leave++;
        }
        return "উপস্থিত " + present + ", দেরি " + late + ", অনুপস্থিত " + absent + ", ছুটি " + leave;
    }

    private Section findSection(Long id) {
        return sectionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("সেকশন পাওয়া যায়নি"));
    }

    private static String normalizeRemark(String remark) {
        return remark == null || remark.isBlank() ? null : remark.trim();
    }

    private static ApiException bad(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }
}