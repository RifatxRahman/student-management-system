package com.sms.studentmanagement.service;

import com.sms.studentmanagement.config.AttendanceProperties;
import com.sms.studentmanagement.dto.RoutineDtos.TimetableRequest;
import com.sms.studentmanagement.dto.RoutineDtos.TimetableResponse;
import com.sms.studentmanagement.dto.RoutineDtos.TimetableUpdateRequest;
import com.sms.studentmanagement.entity.AcademicSession;
import com.sms.studentmanagement.entity.Room;
import com.sms.studentmanagement.entity.Section;
import com.sms.studentmanagement.entity.Subject;
import com.sms.studentmanagement.entity.Teacher;
import com.sms.studentmanagement.entity.TimeSlot;
import com.sms.studentmanagement.entity.TimetableEntry;
import com.sms.studentmanagement.exception.ApiException;
import com.sms.studentmanagement.exception.NotFoundException;
import com.sms.studentmanagement.repository.AcademicSessionRepository;
import com.sms.studentmanagement.repository.RoomRepository;
import com.sms.studentmanagement.repository.SectionRepository;
import com.sms.studentmanagement.repository.SubjectRepository;
import com.sms.studentmanagement.repository.TeacherAssignmentRepository;
import com.sms.studentmanagement.repository.TeacherRepository;
import com.sms.studentmanagement.repository.TimeSlotRepository;
import com.sms.studentmanagement.repository.TimetableRepository;
import com.sms.studentmanagement.security.Actor;
import java.time.DayOfWeek;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.sms.studentmanagement.dto.RoutineDtos.RoutineOption;

@Service
@RequiredArgsConstructor
public class TimetableService {

    /** বাংলাদেশের সপ্তাহের ক্রম: শনিবার থেকে শুক্রবার */
    private static final List<DayOfWeek> DAY_ORDER = List.of(
            DayOfWeek.SATURDAY, DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);

    private static final Map<DayOfWeek, String> DAY_NAMES = Map.of(
            DayOfWeek.SATURDAY, "শনিবার", DayOfWeek.SUNDAY, "রবিবার", DayOfWeek.MONDAY, "সোমবার",
            DayOfWeek.TUESDAY, "মঙ্গলবার", DayOfWeek.WEDNESDAY, "বুধবার", DayOfWeek.THURSDAY, "বৃহস্পতিবার",
            DayOfWeek.FRIDAY, "শুক্রবার");

    private final TimetableRepository timetableRepository;
    private final TimeSlotRepository slotRepository;
    private final RoomRepository roomRepository;
    private final SectionRepository sectionRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;
    private final AcademicSessionRepository sessionRepository;
    private final TeacherAssignmentRepository assignmentRepository;
    private final AttendanceProperties attendanceProperties;
    private final AttendanceAccess access;
    private final AuditService auditService;

    // ---------- দেখা (ব্যক্তিগত তথ্য নয়, লগইন করা যে কেউ) ----------
    /** sessionId না দিলে সক্রিয় সেশনের। */
    @Transactional(readOnly = true)
    public List<TimetableResponse> sectionRoutine(Long sectionId, Long sessionId) {
        Long session = resolveSession(sessionId);
        return sorted(timetableRepository.findForSection(session, sectionId));
    }

    @Transactional(readOnly = true)
    public List<TimetableResponse> teacherRoutine(Long teacherId, Long sessionId) {
        Long session = resolveSession(sessionId);
        return sorted(timetableRepository.findForTeacher(session, teacherId));
    }

    /** লগইন করা শিক্ষকের নিজের রুটিন। */
    @Transactional(readOnly = true)
    public List<TimetableResponse> myRoutine(Actor actor, Long sessionId) {
        Teacher teacher = access.activeTeacher(actor);
        return teacherRoutine(teacher.getId(), sessionId);
    }
    /** এই সেকশনে এই সেশনে কোন শিক্ষক কোন বিষয় পড়ান। রুটিনের ঘরে শুধু এই জোড়াগুলোই বসানো যায়। */
    @Transactional(readOnly = true)
    public List<RoutineOption> options(Long sectionId, Long sessionId) {
        Long session = resolveSession(sessionId);
        return assignmentRepository.findForSectionAndSession(session, sectionId).stream()
                .map(a -> new RoutineOption(
                        a.getSubject().getId(), a.getSubject().getName(), a.getSubject().getCode(),
                        a.getTeacher().getId(), a.getTeacher().getFullName()))
                .toList();
    }

    // ---------- তৈরি, বদল, মোছা (শুধু Admin) ----------
    @Transactional
    public TimetableResponse create(TimetableRequest request) {
        AcademicSession session = sessionRepository.findById(request.sessionId())
                .orElseThrow(() -> new NotFoundException("সেশন পাওয়া যায়নি"));
        Section section = sectionRepository.findById(request.sectionId())
                .orElseThrow(() -> new NotFoundException("সেকশন পাওয়া যায়নি"));
        TimeSlot slot = slotRepository.findById(request.timeSlotId())
                .orElseThrow(() -> new NotFoundException("সময়ের স্লট পাওয়া যায়নি"));
        if (!section.isActive()) {
            throw bad("নিষ্ক্রিয় সেকশনে রুটিন বসানো যায় না");
        }
        if (!slot.isActive()) {
            throw bad("নিষ্ক্রিয় স্লটে রুটিন বসানো যায় না");
        }
        if (attendanceProperties.getWeeklyOffDays().contains(request.dayOfWeek())) {
            throw bad(DAY_NAMES.get(request.dayOfWeek()) + " সাপ্তাহিক ছুটির দিন, ওই দিনে ক্লাস রাখা যায় না");
        }

        Subject subject = findSubject(request.subjectId());
        Teacher teacher = findTeacher(request.teacherId());
        Room room = findRoom(request.roomId());
        requireAssignment(session, section, subject, teacher);

        // সংঘাত: সেকশন, শিক্ষক, রুম
        timetableRepository
                .findBySessionIdAndSectionIdAndDayOfWeekAndTimeSlotId(
                        session.getId(), section.getId(), request.dayOfWeek(), slot.getId())
                .ifPresent(existing -> {
                    throw conflict("এই সেকশনে ওই সময়ে আগে থেকেই ক্লাস আছে: " + existing.getSubject().getName());
                });
        checkTeacherAndRoomFree(null, session.getId(), request.dayOfWeek(), slot.getId(), teacher, room);

        TimetableEntry entry = new TimetableEntry();
        entry.setSession(session);
        entry.setSection(section);
        entry.setDayOfWeek(request.dayOfWeek());
        entry.setTimeSlot(slot);
        entry.setSubject(subject);
        entry.setTeacher(teacher);
        entry.setRoom(room);
        timetableRepository.save(entry);

        auditService.log("ROUTINE_ADDED", "ROUTINE", entry.getId(), describe(entry));
        return TimetableResponse.from(entry);
    }

    @Transactional
    public TimetableResponse update(Long id, TimetableUpdateRequest request) {
        TimetableEntry entry = findEntry(id);
        Subject subject = findSubject(request.subjectId());
        Teacher teacher = findTeacher(request.teacherId());
        Room room = findRoom(request.roomId());
        requireAssignment(entry.getSession(), entry.getSection(), subject, teacher);
        checkTeacherAndRoomFree(
                entry.getId(), entry.getSession().getId(), entry.getDayOfWeek(), entry.getTimeSlot().getId(), teacher, room);

        String before = describe(entry);
        entry.setSubject(subject);
        entry.setTeacher(teacher);
        entry.setRoom(room);
        auditService.log("ROUTINE_UPDATED", "ROUTINE", entry.getId(), before + " → " + describe(entry));
        return TimetableResponse.from(entry);
    }

    @Transactional
    public void delete(Long id) {
        TimetableEntry entry = findEntry(id);
        String text = describe(entry);
        timetableRepository.delete(entry);
        auditService.log("ROUTINE_REMOVED", "ROUTINE", id, text);
    }

    // ---------- ভেতরের সাহায্যকারী ----------
    /** শিক্ষক ও রুম ওই সময়ে খালি কি না। ownId = এই রুটিন নিজে (বদলের সময় নিজের সাথে সংঘাত ধরা হয় না)। */
    private void checkTeacherAndRoomFree(
            Long ownId, Long sessionId, DayOfWeek day, Long slotId, Teacher teacher, Room room) {
        timetableRepository
                .findBySessionIdAndTeacherIdAndDayOfWeekAndTimeSlotId(sessionId, teacher.getId(), day, slotId)
                .filter(other -> !other.getId().equals(ownId))
                .ifPresent(other -> {
                    throw conflict(teacher.getFullName() + " ওই সময়ে " + other.getSection().getSchoolClass().getName()
                            + " " + other.getSection().getName() + "-এ " + other.getSubject().getName()
                            + " ক্লাস নিচ্ছেন");
                });
        if (room != null) {
            timetableRepository
                    .findBySessionIdAndRoomIdAndDayOfWeekAndTimeSlotId(sessionId, room.getId(), day, slotId)
                    .filter(other -> !other.getId().equals(ownId))
                    .ifPresent(other -> {
                        throw conflict("রুম " + room.getName() + " ওই সময়ে " + other.getSection().getSchoolClass().getName()
                                + " " + other.getSection().getName() + "-এর জন্য নেওয়া আছে");
                    });
        }
    }

    /** শিক্ষক এই সেশনে এই সেকশনে এই বিষয় পড়ান (Teacher Assignment আছে), নইলে রুটিনে বসানো যায় না। */
    private void requireAssignment(AcademicSession session, Section section, Subject subject, Teacher teacher) {
        if (!assignmentRepository.existsByTeacherIdAndSessionIdAndSectionIdAndSubjectId(
                teacher.getId(), session.getId(), section.getId(), subject.getId())) {
            throw bad(teacher.getFullName() + "-এর এই সেকশনে " + subject.getName()
                    + " বিষয়ের বরাদ্দ নেই। আগে শিক্ষক পাতা থেকে বরাদ্দ দিন");
        }
    }

    private Subject findSubject(Long id) {
        Subject subject = subjectRepository.findById(id).orElseThrow(() -> new NotFoundException("বিষয় পাওয়া যায়নি"));
        if (!subject.isActive()) {
            throw bad("নিষ্ক্রিয় বিষয় রুটিনে বসানো যায় না");
        }
        return subject;
    }

    private Teacher findTeacher(Long id) {
        Teacher teacher = teacherRepository.findById(id).orElseThrow(() -> new NotFoundException("শিক্ষক পাওয়া যায়নি"));
        if (!teacher.isActive()) {
            throw bad("নিষ্ক্রিয় শিক্ষককে রুটিনে বসানো যায় না");
        }
        return teacher;
    }

    private Room findRoom(Long id) {
        if (id == null) {
            return null;
        }
        Room room = roomRepository.findById(id).orElseThrow(() -> new NotFoundException("রুম পাওয়া যায়নি"));
        if (!room.isActive()) {
            throw bad("নিষ্ক্রিয় রুম রুটিনে বসানো যায় না");
        }
        return room;
    }

    private TimetableEntry findEntry(Long id) {
        return timetableRepository.findWithRefs(id).orElseThrow(() -> new NotFoundException("রুটিনের এই সারি পাওয়া যায়নি"));
    }

    private Long resolveSession(Long sessionId) {
        if (sessionId != null) {
            return sessionId;
        }
        return sessionRepository.findByActiveTrue()
                .orElseThrow(() -> new NotFoundException("কোনো সেশন সক্রিয় নেই"))
                .getId();
    }

    /** বার (শনিবার থেকে) ও তারপর পিরিয়ডের সময় অনুযায়ী */
    private static List<TimetableResponse> sorted(List<TimetableEntry> entries) {
        return entries.stream()
                .sorted(Comparator
                        .comparingInt((TimetableEntry t) -> DAY_ORDER.indexOf(t.getDayOfWeek()))
                        .thenComparing(t -> t.getTimeSlot().getStartTime()))
                .map(TimetableResponse::from)
                .toList();
    }

    private static String describe(TimetableEntry t) {
        return t.getSection().getSchoolClass().getName() + " " + t.getSection().getName()
                + " | " + DAY_NAMES.get(t.getDayOfWeek()) + " " + t.getTimeSlot().getName()
                + " | " + t.getSubject().getName() + " | " + t.getTeacher().getFullName()
                + (t.getRoom() == null ? "" : " | রুম " + t.getRoom().getName());
    }

    private static ApiException bad(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }

    private static ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, message);
    }
}