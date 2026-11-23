package com.sms.studentmanagement.service;

import com.sms.studentmanagement.dto.PromotionDtos.PromotionAction;
import com.sms.studentmanagement.dto.PromotionDtos.PromotionExecuteRequest;
import com.sms.studentmanagement.dto.PromotionDtos.PromotionExecuteResponse;
import com.sms.studentmanagement.dto.PromotionDtos.PromotionPreviewItem;
import com.sms.studentmanagement.dto.PromotionDtos.PromotionPreviewResponse;
import com.sms.studentmanagement.dto.PromotionDtos.StudentPromotionItem;
import com.sms.studentmanagement.entity.AcademicSession;
import com.sms.studentmanagement.entity.Enrollment;
import com.sms.studentmanagement.entity.EnrollmentStatus;
import com.sms.studentmanagement.entity.ExamResult;
import com.sms.studentmanagement.entity.SchoolClass;
import com.sms.studentmanagement.entity.Section;
import com.sms.studentmanagement.entity.Student;
import com.sms.studentmanagement.entity.StudentStatus;
import com.sms.studentmanagement.exception.ApiException;
import com.sms.studentmanagement.exception.NotFoundException;
import com.sms.studentmanagement.repository.AcademicSessionRepository;
import com.sms.studentmanagement.repository.EnrollmentRepository;
import com.sms.studentmanagement.repository.ExamResultRepository;
import com.sms.studentmanagement.repository.SchoolClassRepository;
import com.sms.studentmanagement.repository.SectionRepository;
import com.sms.studentmanagement.repository.StudentRepository;
import com.sms.studentmanagement.util.DhakaTime;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentPromotionService {

    private final AcademicSessionRepository sessionRepository;
    private final SchoolClassRepository classRepository;
    private final SectionRepository sectionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ExamResultRepository examResultRepository;
    private final StudentRepository studentRepository;
    private final AuditService auditService;

    /**
     * প্রমোশন প্রিভিউ তৈরি: রেজাল্ট, মেধা ও পরবর্তী শ্রেণি অনুযায়ী প্রমোশন খসড়া
     */
    @Transactional(readOnly = true)
    public PromotionPreviewResponse preview(
            Long sourceSessionId,
            Long sourceClassId,
            Long sourceSectionId,
            Long targetSessionId,
            Long targetClassId,
            Long finalExamId,
            boolean meritRoll) {

        AcademicSession sourceSession = sessionRepository.findById(sourceSessionId)
                .orElseThrow(() -> new NotFoundException("পূর্ববর্তী সেশন পাওয়া যায়নি"));
        AcademicSession targetSession = sessionRepository.findById(targetSessionId)
                .orElseThrow(() -> new NotFoundException("পরবর্তী সেশন পাওয়া যায়নি"));
        SchoolClass sourceClass = classRepository.findById(sourceClassId)
                .orElseThrow(() -> new NotFoundException("পূর্ববর্তী শ্রেণি পাওয়া যায়নি"));

        // পরবর্তী শ্রেণি নির্ধারণ (যদি ইউজার সরাসরি না দেন, তাহলে ডিসপ্লে অর্ডার অনুযায়ী পরের শ্রেণি)
        List<SchoolClass> allOrderedClasses = classRepository.findAllByOrderByDisplayOrderAsc();
        SchoolClass targetClass = null;
        if (targetClassId != null) {
            targetClass = classRepository.findById(targetClassId)
                    .orElseThrow(() -> new NotFoundException("পরবর্তী শ্রেণি পাওয়া যায়নি"));
        } else {
            int currentIndex = -1;
            for (int i = 0; i < allOrderedClasses.size(); i++) {
                if (allOrderedClasses.get(i).getId().equals(sourceClassId)) {
                    currentIndex = i;
                    break;
                }
            }
            if (currentIndex >= 0 && currentIndex + 1 < allOrderedClasses.size()) {
                targetClass = allOrderedClasses.get(currentIndex + 1);
            }
        }

        // পূর্ববর্তী সেশনের সক্রিয় শিক্ষার্থীদের ভর্তি তথ্য
        List<Enrollment> sourceEnrollments;
        if (sourceSectionId != null) {
            sourceEnrollments = enrollmentRepository.findBySessionIdAndSectionIdAndStatus(
                    sourceSessionId, sourceSectionId, EnrollmentStatus.ACTIVE);
        } else {
            sourceEnrollments = enrollmentRepository.findInClassAndSession(
                    sourceSessionId, sourceClassId, EnrollmentStatus.ACTIVE);
        }

        // পরীক্ষার ফলাফল ম্যাপিং (যদি ফাইনাল পরীক্ষার আইডি দেওয়া থাকে)
        Map<Long, ExamResult> resultsByEnrollmentId = new HashMap<>();
        if (finalExamId != null) {
            List<ExamResult> results = examResultRepository.findAllForExam(finalExamId);
            for (int i = 0; i < results.size(); i++) {
                ExamResult r = results.get(i);
                resultsByEnrollmentId.put(r.getEnrollment().getId(), r);
            }
        }

        // পরবর্তী ক্লাসের সেকশন তালিকা
        List<Section> targetSections = targetClass != null
                ? sectionRepository.findBySchoolClassIdOrderByName(targetClass.getId())
                : List.of();
        List<Section> sourceSections = sectionRepository.findBySchoolClassIdOrderByName(sourceClass.getId());

        List<PromotionPreviewItem> items = new ArrayList<>();
        int passedCount = 0;
        int failedCount = 0;

        for (int i = 0; i < sourceEnrollments.size(); i++) {
            Enrollment e = sourceEnrollments.get(i);
            Student s = e.getStudent();
            Long sId = s.getId();

            boolean alreadyEnrolledInTarget = enrollmentRepository.existsByStudentIdAndSessionIdAndStatus(
                    sId, targetSessionId, EnrollmentStatus.ACTIVE);

            ExamResult res = resultsByEnrollmentId.get(e.getId());
            BigDecimal gpa = res != null ? res.getGpa() : null;
            String grade = res != null ? res.getGrade() : null;
            Boolean passed = res != null ? res.isPassed() : null;
            Integer meritPos = res != null ? (res.getPositionInClass() != null ? res.getPositionInClass() : res.getPositionInSection()) : null;

            if (passed != null) {
                if (passed) {
                    passedCount++;
                } else {
                    failedCount++;
                }
            }

            // প্রস্তাবিত অ্যাকশন ও টার্গেট ক্লাস/সেকশন নির্ধারণ
            PromotionAction action;
            Long suggestedClassId = null;
            String suggestedClassName = null;
            Long suggestedSecId = null;
            String suggestedSecName = null;

            if (alreadyEnrolledInTarget) {
                action = PromotionAction.SKIP;
            } else if (targetClass == null) {
                // সর্বোচ্চ ক্লাস উত্তীর্ণ হলে গ্র্যাজুয়েট
                action = PromotionAction.GRADUATE;
            } else if (passed != null && !passed) {
                // ফেল করলে একই ক্লাসে রিটেইন
                action = PromotionAction.RETAIN;
                suggestedClassId = sourceClass.getId();
                suggestedClassName = sourceClass.getName();
                suggestedSecId = e.getSection() != null ? e.getSection().getId() : null;
                suggestedSecName = e.getSection() != null ? e.getSection().getName() : null;
            } else {
                // পাস বা ফলাফল ছাড়া সাধারণ প্রমোশন
                action = PromotionAction.PROMOTE;
                suggestedClassId = targetClass.getId();
                suggestedClassName = targetClass.getName();

                // শাখা ম্যাপিং: বর্তমান শাখার একই নামের শাখা থাকলে সেটি, নয়তো ১ম শাখা
                String curSecName = e.getSection() != null ? e.getSection().getName() : "";
                Section matchedTargetSec = null;
                for (int j = 0; j < targetSections.size(); j++) {
                    Section sec = targetSections.get(j);
                    if (sec.getName().equalsIgnoreCase(curSecName)) {
                        matchedTargetSec = sec;
                        break;
                    }
                }
                if (matchedTargetSec == null && !targetSections.isEmpty()) {
                    matchedTargetSec = targetSections.get(0);
                }
                if (matchedTargetSec != null) {
                    suggestedSecId = matchedTargetSec.getId();
                    suggestedSecName = matchedTargetSec.getName();
                }
            }

            int suggestedRoll = e.getRollNumber();

            items.add(new PromotionPreviewItem(
                    sId,
                    s.getStudentCode(),
                    s.getFullName(),
                    e.getId(),
                    e.getSection() != null ? e.getSection().getId() : null,
                    e.getSection() != null ? e.getSection().getName() : "—",
                    e.getRollNumber(),
                    gpa,
                    grade,
                    passed,
                    meritPos,
                    alreadyEnrolledInTarget,
                    action,
                    suggestedClassId,
                    suggestedClassName,
                    suggestedSecId,
                    suggestedSecName,
                    suggestedRoll
            ));
        }

        // যদি মেধা রোল অপশন চালু থাকে এবং মেধা পজিশন থাকে, তবে রোল পুনরায় সাজানো
        if (meritRoll && !items.isEmpty()) {
            // প্রমোট হওয়া শিক্ষার্থীদের মেধা অবস্থান অনুযায়ী সাজিয়ে রোল ১, ২, ৩... দেওয়া
            List<PromotionPreviewItem> passingList = new ArrayList<>();
            for (int i = 0; i < items.size(); i++) {
                PromotionPreviewItem itm = items.get(i);
                if (itm.suggestedAction() == PromotionAction.PROMOTE && itm.meritPosition() != null) {
                    passingList.add(itm);
                }
            }
            passingList.sort(Comparator.comparingInt(PromotionPreviewItem::meritPosition));

            Map<Long, Integer> newRollMap = new HashMap<>();
            for (int i = 0; i < passingList.size(); i++) {
                newRollMap.put(passingList.get(i).studentId(), i + 1);
            }

            for (int i = 0; i < items.size(); i++) {
                PromotionPreviewItem itm = items.get(i);
                if (newRollMap.containsKey(itm.studentId())) {
                    items.set(i, new PromotionPreviewItem(
                            itm.studentId(),
                            itm.studentCode(),
                            itm.fullName(),
                            itm.currentEnrollmentId(),
                            itm.currentSectionId(),
                            itm.currentSectionName(),
                            itm.currentRollNumber(),
                            itm.gpa(),
                            itm.grade(),
                            itm.passed(),
                            itm.meritPosition(),
                            itm.alreadyEnrolledInTarget(),
                            itm.suggestedAction(),
                            itm.suggestedTargetClassId(),
                            itm.suggestedTargetClassName(),
                            itm.suggestedTargetSectionId(),
                            itm.suggestedTargetSectionName(),
                            newRollMap.get(itm.studentId())
                    ));
                }
            }
        }

        return new PromotionPreviewResponse(
                sourceSession.getId(),
                sourceSession.getName(),
                sourceClass.getId(),
                sourceClass.getName(),
                targetSession.getId(),
                targetSession.getName(),
                targetClass != null ? targetClass.getId() : null,
                targetClass != null ? targetClass.getName() : "সমাপ্ত/গ্র্যাজুয়েট",
                items.size(),
                passedCount,
                failedCount,
                items
        );
    }

    /**
     * প্রমোশন কার্যকরকরণ: শিক্ষার্থী স্থানান্তর, আগের সেশনের স্থিতি আপডেট ও নতুন ক্লাসে ভর্তি তৈরি
     */
    @Transactional
    public PromotionExecuteResponse execute(PromotionExecuteRequest request) {
        if (request == null || request.students() == null || request.students().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "প্রমোশনের জন্য কোনো শিক্ষার্থী নির্বাচন করা হয়নি");
        }

        AcademicSession sourceSession = sessionRepository.findById(request.sourceSessionId())
                .orElseThrow(() -> new NotFoundException("পূর্ববর্তী সেশন পাওয়া যায়নি"));
        AcademicSession targetSession = sessionRepository.findById(request.targetSessionId())
                .orElseThrow(() -> new NotFoundException("পরবর্তী সেশন পাওয়া যায়নি"));

        LocalDate today = DhakaTime.today();
        LocalDate startDate = targetSession.getStartDate() != null ? targetSession.getStartDate() : today;

        int promotedCount = 0;
        int retainedCount = 0;
        int graduatedCount = 0;
        int skippedCount = 0;

        List<StudentPromotionItem> studentList = request.students();
        for (int i = 0; i < studentList.size(); i++) {
            StudentPromotionItem item = studentList.get(i);
            Long studentId = item.studentId();
            PromotionAction action = item.action();

            if (action == PromotionAction.SKIP) {
                skippedCount++;
                continue;
            }

            // শিক্ষার্থীর পূর্ববর্তী সক্রিয় ভর্তি খুঁজে শেষ করা
            List<Enrollment> currentActiveEnrollments = enrollmentRepository.findByStudentAndStatus(
                    studentId, EnrollmentStatus.ACTIVE);
            for (int j = 0; j < currentActiveEnrollments.size(); j++) {
                Enrollment activeEnr = currentActiveEnrollments.get(j);
                if (activeEnr.getSession().getId().equals(sourceSession.getId())) {
                    activeEnr.setStatus(EnrollmentStatus.COMPLETED);
                    activeEnr.setEndDate(today);
                    enrollmentRepository.save(activeEnr);
                }
            }

            Student student = studentRepository.findById(studentId).orElse(null);
            if (student == null) {
                continue;
            }

            if (action == PromotionAction.GRADUATE) {
                student.setStatus(StudentStatus.GRADUATED);
                studentRepository.save(student);
                graduatedCount++;
                continue;
            }

            if (action == PromotionAction.PROMOTE || action == PromotionAction.RETAIN) {
                if (item.targetClassId() == null || item.targetSectionId() == null) {
                    skippedCount++;
                    continue;
                }

                // ইতিমধ্যে নতুন সেশনে ভর্তি থাকলে ডুপ্লিকেট এড়ানো
                if (enrollmentRepository.existsByStudentIdAndSessionIdAndStatus(
                        studentId, targetSession.getId(), EnrollmentStatus.ACTIVE)) {
                    skippedCount++;
                    continue;
                }

                SchoolClass targetClass = classRepository.findById(item.targetClassId()).orElse(null);
                Section targetSection = sectionRepository.findById(item.targetSectionId()).orElse(null);
                if (targetClass == null || targetSection == null) {
                    skippedCount++;
                    continue;
                }

                int roll = item.newRollNumber() != null && item.newRollNumber() > 0
                        ? item.newRollNumber()
                        : (enrollmentRepository.maxRoll(targetSession.getId(), targetSection.getId(), EnrollmentStatus.ACTIVE) + 1);

                Enrollment newEnrollment = new Enrollment();
                newEnrollment.setStudent(student);
                newEnrollment.setSession(targetSession);
                newEnrollment.setSchoolClass(targetClass);
                newEnrollment.setSection(targetSection);
                newEnrollment.setRollNumber(roll);
                newEnrollment.setStartDate(startDate);
                newEnrollment.setStatus(EnrollmentStatus.ACTIVE);
                enrollmentRepository.save(newEnrollment);

                if (action == PromotionAction.PROMOTE) {
                    promotedCount++;
                } else {
                    retainedCount++;
                }
            }
        }

        int total = promotedCount + retainedCount + graduatedCount + skippedCount;
        auditService.log("SESSION_PROMOTION_EXECUTED", "ACADEMIC", targetSession.getId(),
                String.format("সেশন %s থেকে %s এ প্রমোশন: মোট %d জন (উত্তীর্ণ: %d, রিটেইন: %d, গ্র্যাজুয়েট: %d)",
                        sourceSession.getName(), targetSession.getName(), total, promotedCount, retainedCount, graduatedCount));

        return new PromotionExecuteResponse(
                total,
                promotedCount,
                retainedCount,
                graduatedCount,
                skippedCount,
                String.format("প্রমোশন সফলভাবে সম্পন্ন হয়েছে! উত্তীর্ণ: %d জন, একই ক্লাসে বহাল: %d জন, সমাপ্ত/গ্র্যাজুয়েট: %d জন।",
                        promotedCount, retainedCount, graduatedCount)
        );
    }
}
