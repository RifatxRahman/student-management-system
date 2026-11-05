package com.sms.studentmanagement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.sms.studentmanagement.config.AttendanceProperties;
import com.sms.studentmanagement.dto.RoutineDtos.TimetableRequest;
import com.sms.studentmanagement.dto.RoutineDtos.TimetableUpdateRequest;
import com.sms.studentmanagement.entity.AcademicSession;
import com.sms.studentmanagement.entity.Room;
import com.sms.studentmanagement.entity.SchoolClass;
import com.sms.studentmanagement.entity.Section;
import com.sms.studentmanagement.entity.Subject;
import com.sms.studentmanagement.entity.Teacher;
import com.sms.studentmanagement.entity.TimeSlot;
import com.sms.studentmanagement.entity.TimetableEntry;
import com.sms.studentmanagement.exception.ApiException;
import com.sms.studentmanagement.repository.AcademicSessionRepository;
import com.sms.studentmanagement.repository.RoomRepository;
import com.sms.studentmanagement.repository.SectionRepository;
import com.sms.studentmanagement.repository.SubjectRepository;
import com.sms.studentmanagement.repository.TeacherAssignmentRepository;
import com.sms.studentmanagement.repository.TeacherRepository;
import com.sms.studentmanagement.repository.TimeSlotRepository;
import com.sms.studentmanagement.repository.TimetableRepository;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import com.sms.studentmanagement.dto.RoutineDtos.RoutineOption;
import com.sms.studentmanagement.entity.TeacherAssignment;
import java.util.List;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TimetableServiceTest {

    @Mock
    TimetableRepository timetableRepository;

    @Mock
    TimeSlotRepository slotRepository;

    @Mock
    RoomRepository roomRepository;

    @Mock
    SectionRepository sectionRepository;

    @Mock
    SubjectRepository subjectRepository;

    @Mock
    TeacherRepository teacherRepository;

    @Mock
    AcademicSessionRepository sessionRepository;

    @Mock
    TeacherAssignmentRepository assignmentRepository;

    @Mock
    AttendanceAccess access;

    @Mock
    AuditService auditService;

    TimetableService service;

    AcademicSession session;
    SchoolClass schoolClass;
    Section section;
    TimeSlot slot;
    Subject subject;
    Teacher teacher;
    Room room;

    @BeforeEach
    void setUp() {
        service = new TimetableService(
                timetableRepository, slotRepository, roomRepository, sectionRepository, subjectRepository,
                teacherRepository, sessionRepository, assignmentRepository, new AttendanceProperties(), access,
                auditService);

        session = new AcademicSession();
        session.setId(1L);
        schoolClass = new SchoolClass();
        schoolClass.setId(5L);
        schoolClass.setName("Class 6");
        section = new Section();
        section.setId(10L);
        section.setName("A");
        section.setActive(true);
        section.setSchoolClass(schoolClass);
        slot = new TimeSlot();
        slot.setId(20L);
        slot.setName("1st");
        slot.setStartTime(LocalTime.of(9, 0));
        slot.setEndTime(LocalTime.of(9, 45));
        subject = new Subject();
        subject.setId(9L);
        subject.setName("Math");
        subject.setActive(true);
        teacher = new Teacher();
        teacher.setId(3L);
        teacher.setFullName("Rahim Sir");
        teacher.setActive(true);
        room = new Room();
        room.setId(30L);
        room.setName("101");

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(sectionRepository.findById(10L)).thenReturn(Optional.of(section));
        when(slotRepository.findById(20L)).thenReturn(Optional.of(slot));
        when(subjectRepository.findById(9L)).thenReturn(Optional.of(subject));
        when(teacherRepository.findById(3L)).thenReturn(Optional.of(teacher));
        when(roomRepository.findById(30L)).thenReturn(Optional.of(room));
        when(assignmentRepository.existsByTeacherIdAndSessionIdAndSectionIdAndSubjectId(3L, 1L, 10L, 9L))
                .thenReturn(true);
    }

    private TimetableRequest request(DayOfWeek day, Long roomId) {
        return new TimetableRequest(1L, 10L, day, 20L, 9L, 3L, roomId);
    }

    /** অন্য সেকশনে আগে থেকে বসানো একটা রুটিন-সারি */
    private TimetableEntry otherEntry(long id) {
        Section other = new Section();
        other.setId(11L);
        other.setName("B");
        other.setSchoolClass(schoolClass);
        TimetableEntry entry = new TimetableEntry();
        entry.setId(id);
        entry.setSection(other);
        entry.setSubject(subject);
        entry.setTeacher(teacher);
        entry.setRoom(room);
        entry.setTimeSlot(slot);
        entry.setSession(session);
        entry.setDayOfWeek(DayOfWeek.SUNDAY);
        return entry;
    }

    @Test
    void validRoutineIsSavedAndLogged() {
        service.create(request(DayOfWeek.SUNDAY, 30L));

        verify(timetableRepository).save(any(TimetableEntry.class));
        verify(auditService).log(eq("ROUTINE_ADDED"), eq("ROUTINE"), any(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void routineWithoutRoomIsAllowedAndSkipsRoomCheck() {
        service.create(request(DayOfWeek.SUNDAY, null));

        verify(timetableRepository).save(any(TimetableEntry.class));
        verify(timetableRepository, never())
                .findBySessionIdAndRoomIdAndDayOfWeekAndTimeSlotId(anyLong(), anyLong(), any(), anyLong());
    }

    @Test
    void teacherWithoutAssignmentCannotBePlaced() {
        when(assignmentRepository.existsByTeacherIdAndSessionIdAndSectionIdAndSubjectId(3L, 1L, 10L, 9L))
                .thenReturn(false);

        ApiException error = assertThrows(ApiException.class, () -> service.create(request(DayOfWeek.SUNDAY, 30L)));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
        verify(timetableRepository, never()).save(any());
    }

    @Test
    void teacherCannotBeInTwoPlacesAtTheSameTime() {
        when(timetableRepository.findBySessionIdAndTeacherIdAndDayOfWeekAndTimeSlotId(1L, 3L, DayOfWeek.SUNDAY, 20L))
                .thenReturn(Optional.of(otherEntry(500L)));

        ApiException error = assertThrows(ApiException.class, () -> service.create(request(DayOfWeek.SUNDAY, 30L)));
        assertEquals(HttpStatus.CONFLICT, error.getStatus());
        assertTrue(error.getMessage().contains("Rahim Sir"));
        verify(timetableRepository, never()).save(any());
    }

    @Test
    void roomCannotBeBookedTwiceAtTheSameTime() {
        when(timetableRepository.findBySessionIdAndRoomIdAndDayOfWeekAndTimeSlotId(1L, 30L, DayOfWeek.SUNDAY, 20L))
                .thenReturn(Optional.of(otherEntry(500L)));

        ApiException error = assertThrows(ApiException.class, () -> service.create(request(DayOfWeek.SUNDAY, 30L)));
        assertEquals(HttpStatus.CONFLICT, error.getStatus());
        verify(timetableRepository, never()).save(any());
    }

    @Test
    void sectionCannotHaveTwoClassesAtTheSameTime() {
        when(timetableRepository.findBySessionIdAndSectionIdAndDayOfWeekAndTimeSlotId(1L, 10L, DayOfWeek.SUNDAY, 20L))
                .thenReturn(Optional.of(otherEntry(500L)));

        ApiException error = assertThrows(ApiException.class, () -> service.create(request(DayOfWeek.SUNDAY, 30L)));
        assertEquals(HttpStatus.CONFLICT, error.getStatus());
    }

    @Test
    void weeklyOffDayIsRejected() {
        ApiException error = assertThrows(ApiException.class, () -> service.create(request(DayOfWeek.FRIDAY, 30L)));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
    }

    @Test
    void inactiveSlotTeacherAndRoomAreRejected() {
        slot.setActive(false);
        assertThrows(ApiException.class, () -> service.create(request(DayOfWeek.SUNDAY, 30L)));
        slot.setActive(true);

        teacher.setActive(false);
        assertThrows(ApiException.class, () -> service.create(request(DayOfWeek.SUNDAY, 30L)));
        teacher.setActive(true);

        room.setActive(false);
        assertThrows(ApiException.class, () -> service.create(request(DayOfWeek.SUNDAY, 30L)));
    }

    @Test
    void updateIsNotBlockedByTheRoutineItself() {
        TimetableEntry self = otherEntry(500L);
        self.setSection(section);
        when(timetableRepository.findWithRefs(500L)).thenReturn(Optional.of(self));
        // নিজের সারিই শিক্ষক ও রুমের "সংঘাত" হিসেবে ফেরত আসছে, কিন্তু সেটা নিজে, তাই ঠিক
        when(timetableRepository.findBySessionIdAndTeacherIdAndDayOfWeekAndTimeSlotId(1L, 3L, DayOfWeek.SUNDAY, 20L))
                .thenReturn(Optional.of(self));
        when(timetableRepository.findBySessionIdAndRoomIdAndDayOfWeekAndTimeSlotId(1L, 30L, DayOfWeek.SUNDAY, 20L))
                .thenReturn(Optional.of(self));

        service.update(500L, new TimetableUpdateRequest(9L, 3L, 30L));

        verify(auditService).log(eq("ROUTINE_UPDATED"), eq("ROUTINE"), eq(500L), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void updateToATeacherWhoIsBusyElsewhereIsRejected() {
        TimetableEntry self = otherEntry(500L);
        self.setSection(section);
        when(timetableRepository.findWithRefs(500L)).thenReturn(Optional.of(self));
        when(timetableRepository.findBySessionIdAndTeacherIdAndDayOfWeekAndTimeSlotId(1L, 3L, DayOfWeek.SUNDAY, 20L))
                .thenReturn(Optional.of(otherEntry(501L)));

        ApiException error = assertThrows(ApiException.class,
                () -> service.update(500L, new TimetableUpdateRequest(9L, 3L, 30L)));
        assertEquals(HttpStatus.CONFLICT, error.getStatus());
    }

    @Test
    void optionsAreBuiltFromTheAssignmentsOfThatSectionAndSession() {
        TeacherAssignment assignment = new TeacherAssignment();
        assignment.setSubject(subject);
        assignment.setTeacher(teacher);
        when(assignmentRepository.findForSectionAndSession(1L, 10L)).thenReturn(List.of(assignment));

        List<RoutineOption> options = service.options(10L, 1L);

        assertEquals(1, options.size());
        assertEquals("Math", options.get(0).subjectName());
        assertEquals("Rahim Sir", options.get(0).teacherName());
        assertEquals(3L, options.get(0).teacherId());
    }
}