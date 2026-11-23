package com.sms.studentmanagement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sms.studentmanagement.dto.FeeDtos.ClassFeeSummaryResponse;
import com.sms.studentmanagement.dto.FeeDtos.CopyFeeStructureRequest;
import com.sms.studentmanagement.dto.FeeDtos.FeeHeadRequest;
import com.sms.studentmanagement.dto.FeeDtos.FeeHeadResponse;
import com.sms.studentmanagement.dto.FeeDtos.FeeStructureRequest;
import com.sms.studentmanagement.dto.FeeDtos.FeeStructureResponse;
import com.sms.studentmanagement.entity.AcademicSession;
import com.sms.studentmanagement.entity.FeeFrequency;
import com.sms.studentmanagement.entity.FeeHead;
import com.sms.studentmanagement.entity.FeeStructure;
import com.sms.studentmanagement.entity.SchoolClass;
import com.sms.studentmanagement.exception.ApiException;
import com.sms.studentmanagement.repository.AcademicSessionRepository;
import com.sms.studentmanagement.repository.FeeHeadRepository;
import com.sms.studentmanagement.repository.FeeStructureRepository;
import com.sms.studentmanagement.repository.SchoolClassRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class FeeStructureServiceTest {

    @Mock
    FeeHeadRepository feeHeadRepository;

    @Mock
    FeeStructureRepository feeStructureRepository;

    @Mock
    AcademicSessionRepository sessionRepository;

    @Mock
    SchoolClassRepository classRepository;

    @Mock
    AuditService auditService;

    FeeStructureService service;

    @BeforeEach
    void setUp() {
        service = new FeeStructureService(
                feeHeadRepository,
                feeStructureRepository,
                sessionRepository,
                classRepository,
                auditService
        );
    }

    @Test
    void createFeeHead_success() {
        FeeHeadRequest req = new FeeHeadRequest("টিউশন ফি", "TUITION", "মাসিক বেতন", true);
        when(feeHeadRepository.existsByNameIgnoreCase("টিউশন ফি")).thenReturn(false);
        when(feeHeadRepository.existsByCodeIgnoreCase("TUITION")).thenReturn(false);

        FeeHead saved = new FeeHead();
        saved.setId(1L);
        saved.setName("টিউশন ফি");
        saved.setCode("TUITION");
        saved.setDescription("মাসিক বেতন");
        saved.setActive(true);

        when(feeHeadRepository.save(any(FeeHead.class))).thenReturn(saved);

        FeeHeadResponse res = service.createFeeHead(req);
        assertNotNull(res);
        assertEquals(1L, res.id());
        assertEquals("TUITION", res.code());
        verify(auditService).log(any(), any(), any(), any());
    }

    @Test
    void createFeeHead_conflictName_throwsException() {
        FeeHeadRequest req = new FeeHeadRequest("টিউশন ফি", "TUITION", "মাসিক বেতন", true);
        when(feeHeadRepository.existsByNameIgnoreCase("টিউশন ফি")).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> service.createFeeHead(req));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(feeHeadRepository, never()).save(any());
    }

    @Test
    void deleteFeeHead_whenReferencedInStructures_throwsBadRequest() {
        FeeHead head = new FeeHead();
        head.setId(1L);
        head.setName("ভর্তি ফি");
        head.setCode("ADM");

        when(feeHeadRepository.findById(1L)).thenReturn(Optional.of(head));
        when(feeStructureRepository.existsByFeeHeadId(1L)).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> service.deleteFeeHead(1L));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(feeHeadRepository, never()).delete(any());
    }

    @Test
    void createFeeStructure_success() {
        AcademicSession session = new AcademicSession();
        session.setId(1L);
        session.setName("2026");

        SchoolClass sc = new SchoolClass();
        sc.setId(2L);
        sc.setName("Class 6");

        FeeHead head = new FeeHead();
        head.setId(3L);
        head.setName("টিউশন ফি");
        head.setCode("TUITION");

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(classRepository.findById(2L)).thenReturn(Optional.of(sc));
        when(feeHeadRepository.findById(3L)).thenReturn(Optional.of(head));
        when(feeStructureRepository.existsBySessionIdAndSchoolClassIdAndFeeHeadId(1L, 2L, 3L)).thenReturn(false);

        FeeStructure saved = new FeeStructure();
        saved.setId(10L);
        saved.setSession(session);
        saved.setSchoolClass(sc);
        saved.setFeeHead(head);
        saved.setAmount(new BigDecimal("1500.00"));
        saved.setFrequency(FeeFrequency.MONTHLY);
        saved.setDueDay(10);
        saved.setActive(true);

        when(feeStructureRepository.save(any(FeeStructure.class))).thenReturn(saved);

        FeeStructureRequest req = new FeeStructureRequest(
                1L, 2L, 3L, new BigDecimal("1500.00"), FeeFrequency.MONTHLY, 10, "মাসিক টিউশন ফি", true
        );

        FeeStructureResponse res = service.createFeeStructure(req);
        assertNotNull(res);
        assertEquals(10L, res.id());
        assertEquals("Class 6", res.className());
        assertEquals(new BigDecimal("1500.00"), res.amount());
        assertEquals(FeeFrequency.MONTHLY, res.frequency());
        verify(auditService).log(any(), any(), any(), any());
    }

    @Test
    void getClassFeeSummary_calculatesCorrectTotals() {
        AcademicSession session = new AcademicSession();
        session.setId(1L);
        session.setName("2026");

        SchoolClass sc = new SchoolClass();
        sc.setId(2L);
        sc.setName("Class 10");

        FeeHead h1 = new FeeHead();
        h1.setId(1L);
        h1.setName("মাসিক বেতন");
        h1.setCode("TUI");

        FeeHead h2 = new FeeHead();
        h2.setId(2L);
        h2.setName("পরীক্ষা ফি");
        h2.setCode("EXAM");

        FeeHead h3 = new FeeHead();
        h3.setId(3L);
        h3.setName("সেশন ফি");
        h3.setCode("SES");

        FeeHead h4 = new FeeHead();
        h4.setId(4L);
        h4.setName("ভর্তি ফি");
        h4.setCode("ADM");

        List<FeeStructure> list = new ArrayList<>();

        FeeStructure fs1 = new FeeStructure();
        fs1.setId(101L);
        fs1.setSession(session);
        fs1.setSchoolClass(sc);
        fs1.setFeeHead(h1);
        fs1.setAmount(new BigDecimal("1800.00"));
        fs1.setFrequency(FeeFrequency.MONTHLY);
        fs1.setActive(true);
        list.add(fs1);

        FeeStructure fs2 = new FeeStructure();
        fs2.setId(102L);
        fs2.setSession(session);
        fs2.setSchoolClass(sc);
        fs2.setFeeHead(h2);
        fs2.setAmount(new BigDecimal("600.00"));
        fs2.setFrequency(FeeFrequency.TERMLY);
        fs2.setActive(true);
        list.add(fs2);

        FeeStructure fs3 = new FeeStructure();
        fs3.setId(103L);
        fs3.setSession(session);
        fs3.setSchoolClass(sc);
        fs3.setFeeHead(h3);
        fs3.setAmount(new BigDecimal("4500.00"));
        fs3.setFrequency(FeeFrequency.YEARLY);
        fs3.setActive(true);
        list.add(fs3);

        FeeStructure fs4 = new FeeStructure();
        fs4.setId(104L);
        fs4.setSession(session);
        fs4.setSchoolClass(sc);
        fs4.setFeeHead(h4);
        fs4.setAmount(new BigDecimal("8000.00"));
        fs4.setFrequency(FeeFrequency.ONE_TIME);
        fs4.setActive(true);
        list.add(fs4);

        when(classRepository.findById(2L)).thenReturn(Optional.of(sc));
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(feeStructureRepository.findBySessionIdAndSchoolClassId(1L, 2L)).thenReturn(list);

        ClassFeeSummaryResponse summary = service.getClassFeeSummary(1L, 2L);
        assertNotNull(summary);
        assertEquals("Class 10", summary.className());
        assertEquals(new BigDecimal("1800.00"), summary.totalMonthlyAmount());
        assertEquals(new BigDecimal("600.00"), summary.totalTermlyAmount());
        assertEquals(new BigDecimal("4500.00"), summary.totalYearlyAmount());
        assertEquals(new BigDecimal("8000.00"), summary.totalOneTimeAmount());
        assertEquals(4, summary.totalHeadsCount());
    }

    @Test
    void copySessionFees_success() {
        AcademicSession session1 = new AcademicSession();
        session1.setId(1L);
        session1.setName("2025");

        AcademicSession session2 = new AcademicSession();
        session2.setId(2L);
        session2.setName("2026");

        SchoolClass sc = new SchoolClass();
        sc.setId(5L);
        sc.setName("Class 9");

        FeeHead head = new FeeHead();
        head.setId(6L);
        head.setName("আইসিটি ফি");
        head.setCode("ICT");

        FeeStructure src = new FeeStructure();
        src.setId(201L);
        src.setSession(session1);
        src.setSchoolClass(sc);
        src.setFeeHead(head);
        src.setAmount(new BigDecimal("500.00"));
        src.setFrequency(FeeFrequency.MONTHLY);
        src.setDueDay(10);
        src.setActive(true);

        List<FeeStructure> sourceList = new ArrayList<>();
        sourceList.add(src);

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session1));
        when(sessionRepository.findById(2L)).thenReturn(Optional.of(session2));
        when(feeStructureRepository.findBySessionId(1L)).thenReturn(sourceList);
        when(feeStructureRepository.findBySessionIdAndSchoolClassIdAndFeeHeadId(2L, 5L, 6L)).thenReturn(Optional.empty());

        CopyFeeStructureRequest req = new CopyFeeStructureRequest(1L, 2L, false);
        int copied = service.copySessionFees(req);

        assertEquals(1, copied);
        verify(feeStructureRepository).save(any(FeeStructure.class));
        verify(auditService).log(any(), any(), any(), any());
    }
}
