package com.sms.studentmanagement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sms.studentmanagement.dto.ExamDtos.ExamDetail;
import com.sms.studentmanagement.dto.ExamDtos.ExamRequest;
import com.sms.studentmanagement.dto.ExamDtos.ExamSubjectRequest;
import com.sms.studentmanagement.dto.ExamDtos.ExamUpdateRequest;
import com.sms.studentmanagement.entity.AcademicSession;
import com.sms.studentmanagement.entity.ClassSubject;
import com.sms.studentmanagement.entity.Exam;
import com.sms.studentmanagement.entity.ExamStatus;
import com.sms.studentmanagement.entity.ExamSubject;
import com.sms.studentmanagement.entity.ExamType;
import com.sms.studentmanagement.entity.SchoolClass;
import com.sms.studentmanagement.entity.Subject;
import com.sms.studentmanagement.exception.ApiException;
import com.sms.studentmanagement.repository.AcademicSessionRepository;
import com.sms.studentmanagement.repository.ClassSubjectRepository;
import com.sms.studentmanagement.repository.ExamRepository;
import com.sms.studentmanagement.repository.ExamSubjectRepository;
import com.sms.studentmanagement.repository.SchoolClassRepository;
import com.sms.studentmanagement.repository.SubjectRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExamServiceTest {

    @Mock
    ExamRepository examRepository;

    @Mock
    ExamSubjectRepository examSubjectRepository;

    @Mock
    AcademicSessionRepository sessionRepository;

    @Mock
    SchoolClassRepository classRepository;

    @Mock
    SubjectRepository subjectRepository;

    @Mock
    ClassSubjectRepository classSubjectRepository;

    @Mock
    AuditService auditService;

    ExamService service;

    AcademicSession session;
    SchoolClass schoolClass;

    @BeforeEach
    void setUp() {
        service = new ExamService(
                examRepository, examSubjectRepository, sessionRepository, classRepository,
                subjectRepository, classSubjectRepository, auditService);

        session = new AcademicSession();
        session.setId(1L);
        session.setName("2026");
        schoolClass = new SchoolClass();
        schoolClass.setId(5L);
        schoolClass.setName("Class 6");
        schoolClass.setActive(true);
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(classRepository.findById(5L)).thenReturn(Optional.of(schoolClass));
    }

    private Subject subject(long id, String name) {
        Subject subject = new Subject();
        subject.setId(id);
        subject.setName(name);
        subject.setCode(name.toUpperCase());
        subject.setActive(true);
        return subject;
    }

    private ClassSubject classSubject(Subject subject) {
        ClassSubject cs = new ClassSubject();
        cs.setSchoolClass(schoolClass);
        cs.setSubject(subject);
        return cs;
    }

    private Exam exam(ExamStatus status) {
        Exam exam = new Exam();
        exam.setId(7L);
        exam.setSession(session);
        exam.setSchoolClass(schoolClass);
        exam.setName("Midterm");
        exam.setExamType(ExamType.MIDTERM);
        exam.setStatus(status);
        when(examRepository.findWithRefs(7L)).thenReturn(Optional.of(exam));
        return exam;
    }

    private ExamSubjectRequest subjectRequest(String full, String pass) {
        return new ExamSubjectRequest(9L, new BigDecimal(full), new BigDecimal(pass), false);
    }

    @Test
    @SuppressWarnings("unchecked")
    void createWithCopySubjectsAddsAllActiveClassSubjects() {
        Subject math = subject(1L, "Math");
        Subject art = subject(2L, "Art");
        Subject old = subject(3L, "Old");
        old.setActive(false);
        when(classSubjectRepository.findAllWithSubject(5L))
                .thenReturn(List.of(classSubject(math), classSubject(art), classSubject(old)));

        service.create(new ExamRequest(1L, 5L, "Midterm", ExamType.MIDTERM, true));

        ArgumentCaptor<List<ExamSubject>> captor = ArgumentCaptor.forClass(List.class);
        verify(examSubjectRepository).saveAll(captor.capture());
        List<ExamSubject> saved = captor.getValue();
        assertEquals(2, saved.size());
        assertEquals(0, new BigDecimal("100").compareTo(saved.get(0).getFullMarks()));
        assertEquals(0, new BigDecimal("33").compareTo(saved.get(0).getPassMarks()));
    }

    @Test
    void createWithoutCopyAddsNoSubjects() {
        ExamDetail detail = service.create(new ExamRequest(1L, 5L, "  Final  ", ExamType.FINAL, false));

        verify(examSubjectRepository, never()).saveAll(any());
        assertEquals("Final", detail.exam().name());
        assertEquals(ExamStatus.DRAFT, detail.exam().status());
    }

    @Test
    void duplicateNameInSameSessionAndClassIsRejected() {
        when(examRepository.existsBySessionIdAndSchoolClassIdAndName(1L, 5L, "Midterm")).thenReturn(true);

        ApiException error = assertThrows(ApiException.class,
                () -> service.create(new ExamRequest(1L, 5L, "Midterm", ExamType.MIDTERM, false)));
        assertEquals(HttpStatus.CONFLICT, error.getStatus());
        verify(examRepository, never()).save(any());
    }

    @Test
    void inactiveClassCannotHaveExams() {
        schoolClass.setActive(false);

        assertThrows(ApiException.class,
                () -> service.create(new ExamRequest(1L, 5L, "Midterm", ExamType.MIDTERM, false)));
    }

    @Test
    void publishedExamCannotBeRenamedOrDeleted() {
        exam(ExamStatus.PUBLISHED);

        assertThrows(ApiException.class, () -> service.update(7L, new ExamUpdateRequest("New", ExamType.FINAL)));
        assertThrows(ApiException.class, () -> service.delete(7L));
        verify(examRepository, never()).delete(any());
    }

    @Test
    void publishedExamCannotGetNewSubjects() {
        exam(ExamStatus.PUBLISHED);

        assertThrows(ApiException.class, () -> service.addSubject(7L, subjectRequest("100", "33")));
    }

    @Test
    void subjectOutsideTheClassIsRejected() {
        exam(ExamStatus.DRAFT);
        when(subjectRepository.findById(9L)).thenReturn(Optional.of(subject(9L, "Music")));
        when(classSubjectRepository.existsBySchoolClassIdAndSubjectId(5L, 9L)).thenReturn(false);

        ApiException error = assertThrows(ApiException.class, () -> service.addSubject(7L, subjectRequest("100", "33")));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
        verify(examSubjectRepository, never()).save(any());
    }

    @Test
    void sameSubjectTwiceIsRejected() {
        exam(ExamStatus.DRAFT);
        when(subjectRepository.findById(9L)).thenReturn(Optional.of(subject(9L, "Music")));
        when(classSubjectRepository.existsBySchoolClassIdAndSubjectId(5L, 9L)).thenReturn(true);
        when(examSubjectRepository.existsByExamIdAndSubjectId(7L, 9L)).thenReturn(true);

        ApiException error = assertThrows(ApiException.class, () -> service.addSubject(7L, subjectRequest("100", "33")));
        assertEquals(HttpStatus.CONFLICT, error.getStatus());
    }

    @Test
    void passMarksAboveFullMarksIsRejected() {
        exam(ExamStatus.DRAFT);
        when(subjectRepository.findById(9L)).thenReturn(Optional.of(subject(9L, "Music")));
        when(classSubjectRepository.existsBySchoolClassIdAndSubjectId(5L, 9L)).thenReturn(true);

        assertThrows(ApiException.class, () -> service.addSubject(7L, subjectRequest("50", "60")));
        verify(examSubjectRepository, never()).save(any());
    }

    @Test
    void validSubjectIsAddedAndLogged() {
        exam(ExamStatus.DRAFT);
        when(subjectRepository.findById(9L)).thenReturn(Optional.of(subject(9L, "Music")));
        when(classSubjectRepository.existsBySchoolClassIdAndSubjectId(5L, 9L)).thenReturn(true);

        service.addSubject(7L, subjectRequest("50", "17"));

        verify(examSubjectRepository).save(any(ExamSubject.class));
        verify(auditService).log(org.mockito.ArgumentMatchers.eq("EXAM_SUBJECT_ADDED"), anyString(),
                org.mockito.ArgumentMatchers.eq(7L), anyString());
    }
}