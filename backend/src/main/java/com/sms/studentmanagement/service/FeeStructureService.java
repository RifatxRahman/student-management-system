package com.sms.studentmanagement.service;

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
import com.sms.studentmanagement.exception.NotFoundException;
import com.sms.studentmanagement.repository.AcademicSessionRepository;
import com.sms.studentmanagement.repository.FeeHeadRepository;
import com.sms.studentmanagement.repository.FeeStructureRepository;
import com.sms.studentmanagement.repository.SchoolClassRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FeeStructureService {

    private final FeeHeadRepository feeHeadRepository;
    private final FeeStructureRepository feeStructureRepository;
    private final AcademicSessionRepository sessionRepository;
    private final SchoolClassRepository classRepository;
    private final AuditService auditService;

    // ==================== Fee Head Management ====================

    @Transactional(readOnly = true)
    public List<FeeHeadResponse> listFeeHeads(Boolean activeOnly) {
        List<FeeHead> list;
        if (Boolean.TRUE.equals(activeOnly)) {
            list = feeHeadRepository.findByActiveTrueOrderByNameAsc();
        } else {
            list = feeHeadRepository.findAllByOrderByNameAsc();
        }

        List<FeeHeadResponse> responses = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            responses.add(FeeHeadResponse.from(list.get(i)));
        }
        return responses;
    }

    @Transactional(readOnly = true)
    public FeeHeadResponse getFeeHead(Long id) {
        FeeHead head = findFeeHead(id);
        return FeeHeadResponse.from(head);
    }

    @Transactional
    public FeeHeadResponse createFeeHead(FeeHeadRequest request) {
        String name = request.name().trim();
        String code = request.code().trim().toUpperCase();

        if (feeHeadRepository.existsByNameIgnoreCase(name)) {
            throw new ApiException(HttpStatus.CONFLICT, "এই নামের ফি খাত ইতিমধ্যে বিদ্যমান: " + name);
        }
        if (feeHeadRepository.existsByCodeIgnoreCase(code)) {
            throw new ApiException(HttpStatus.CONFLICT, "এই কোডের ফি খাত ইতিমধ্যে বিদ্যমান: " + code);
        }

        FeeHead head = new FeeHead();
        head.setName(name);
        head.setCode(code);
        head.setDescription(request.description() != null ? request.description().trim() : null);
        head.setActive(request.active() == null || request.active());

        FeeHead saved = feeHeadRepository.save(head);
        auditService.log("FEE_HEAD_CREATED", "FEE_HEAD", saved.getId(),
                saved.getName() + " (" + saved.getCode() + ")");
        return FeeHeadResponse.from(saved);
    }

    @Transactional
    public FeeHeadResponse updateFeeHead(Long id, FeeHeadRequest request) {
        FeeHead head = findFeeHead(id);
        String name = request.name().trim();
        String code = request.code().trim().toUpperCase();

        if (feeHeadRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ApiException(HttpStatus.CONFLICT, "এই নামের ফি খাত অন্য একটি রেকর্ডে বিদ্যমান: " + name);
        }
        if (feeHeadRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new ApiException(HttpStatus.CONFLICT, "এই কোডের ফি খাত অন্য একটি রেকর্ডে বিদ্যমান: " + code);
        }

        head.setName(name);
        head.setCode(code);
        head.setDescription(request.description() != null ? request.description().trim() : null);
        if (request.active() != null) {
            head.setActive(request.active());
        }

        FeeHead updated = feeHeadRepository.save(head);
        auditService.log("FEE_HEAD_UPDATED", "FEE_HEAD", updated.getId(),
                updated.getName() + " (" + updated.getCode() + ")");
        return FeeHeadResponse.from(updated);
    }

    @Transactional
    public void deleteFeeHead(Long id) {
        FeeHead head = findFeeHead(id);
        if (feeStructureRepository.existsByFeeHeadId(id)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "এই ফি খাতের অধীনে শ্রেণিভিত্তিক ফি নির্ধারণ করা আছে। প্রথমে ফি নিয়মগুলো মুছুন অথবা খাতটি নিষ্ক্রিয় করুন।");
        }
        feeHeadRepository.delete(head);
        auditService.log("FEE_HEAD_DELETED", "FEE_HEAD", id, head.getName() + " (" + head.getCode() + ")");
    }

    // ==================== Fee Structure Management ====================

    @Transactional(readOnly = true)
    public List<FeeStructureResponse> listFeeStructures(Long sessionId, Long classId) {
        Long targetSessionId = resolveSessionId(sessionId);
        List<FeeStructure> list;
        if (classId != null) {
            list = feeStructureRepository.findBySessionIdAndSchoolClassId(targetSessionId, classId);
        } else {
            list = feeStructureRepository.findBySessionId(targetSessionId);
        }

        List<FeeStructureResponse> responses = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            responses.add(FeeStructureResponse.from(list.get(i)));
        }
        return responses;
    }

    @Transactional(readOnly = true)
    public FeeStructureResponse getFeeStructure(Long id) {
        FeeStructure fs = findFeeStructure(id);
        return FeeStructureResponse.from(fs);
    }

    @Transactional
    public FeeStructureResponse createFeeStructure(FeeStructureRequest request) {
        AcademicSession session = sessionRepository.findById(request.sessionId())
                .orElseThrow(() -> new NotFoundException("শিক্ষাবর্ষ (সেশন) পাওয়া যায়নি: " + request.sessionId()));
        SchoolClass schoolClass = classRepository.findById(request.classId())
                .orElseThrow(() -> new NotFoundException("শ্রেণি (ক্লাস) পাওয়া যায়নি: " + request.classId()));
        FeeHead feeHead = findFeeHead(request.feeHeadId());

        if (feeStructureRepository.existsBySessionIdAndSchoolClassIdAndFeeHeadId(
                session.getId(), schoolClass.getId(), feeHead.getId())) {
            throw new ApiException(HttpStatus.CONFLICT,
                    schoolClass.getName() + " শ্রেণিতে " + feeHead.getName() + " ফি নিয়ম ইতিমধ্যে নির্ধারিত আছে। প্রয়োজনে এডিট করুন।");
        }

        BigDecimal scaledAmount = request.amount().setScale(2, RoundingMode.HALF_UP);
        int dueDay = (request.dueDay() != null && request.dueDay() >= 1 && request.dueDay() <= 31)
                ? request.dueDay() : 10;

        FeeStructure fs = new FeeStructure();
        fs.setSession(session);
        fs.setSchoolClass(schoolClass);
        fs.setFeeHead(feeHead);
        fs.setAmount(scaledAmount);
        fs.setFrequency(request.frequency());
        fs.setDueDay(dueDay);
        fs.setDescription(request.description() != null ? request.description().trim() : null);
        fs.setActive(request.active() == null || request.active());

        FeeStructure saved = feeStructureRepository.save(fs);
        auditService.log("FEE_STRUCTURE_CREATED", "FEE_STRUCTURE", saved.getId(),
                session.getName() + " | " + schoolClass.getName() + " | " + feeHead.getName()
                        + " | ৳" + saved.getAmount() + " (" + saved.getFrequency().getBanglaLabel() + ")");
        return FeeStructureResponse.from(saved);
    }

    @Transactional
    public FeeStructureResponse updateFeeStructure(Long id, FeeStructureRequest request) {
        FeeStructure fs = findFeeStructure(id);
        AcademicSession session = sessionRepository.findById(request.sessionId())
                .orElseThrow(() -> new NotFoundException("শিক্ষাবর্ষ (সেশন) পাওয়া যায়নি: " + request.sessionId()));
        SchoolClass schoolClass = classRepository.findById(request.classId())
                .orElseThrow(() -> new NotFoundException("শ্রেণি (ক্লাস) পাওয়া যায়নি: " + request.classId()));
        FeeHead feeHead = findFeeHead(request.feeHeadId());

        if (feeStructureRepository.existsBySessionIdAndSchoolClassIdAndFeeHeadIdAndIdNot(
                session.getId(), schoolClass.getId(), feeHead.getId(), id)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    schoolClass.getName() + " শ্রেণিতে " + feeHead.getName() + " ফি নিয়ম অন্য একটি রেকর্ডে বিদ্যমান।");
        }

        BigDecimal scaledAmount = request.amount().setScale(2, RoundingMode.HALF_UP);
        int dueDay = (request.dueDay() != null && request.dueDay() >= 1 && request.dueDay() <= 31)
                ? request.dueDay() : 10;

        fs.setSession(session);
        fs.setSchoolClass(schoolClass);
        fs.setFeeHead(feeHead);
        fs.setAmount(scaledAmount);
        fs.setFrequency(request.frequency());
        fs.setDueDay(dueDay);
        fs.setDescription(request.description() != null ? request.description().trim() : null);
        if (request.active() != null) {
            fs.setActive(request.active());
        }

        FeeStructure updated = feeStructureRepository.save(fs);
        auditService.log("FEE_STRUCTURE_UPDATED", "FEE_STRUCTURE", updated.getId(),
                session.getName() + " | " + schoolClass.getName() + " | " + feeHead.getName()
                        + " | ৳" + updated.getAmount() + " (" + updated.getFrequency().getBanglaLabel() + ")");
        return FeeStructureResponse.from(updated);
    }

    @Transactional
    public void deleteFeeStructure(Long id) {
        FeeStructure fs = findFeeStructure(id);
        feeStructureRepository.delete(fs);
        auditService.log("FEE_STRUCTURE_DELETED", "FEE_STRUCTURE", id,
                fs.getSession().getName() + " | " + fs.getSchoolClass().getName() + " | " + fs.getFeeHead().getName());
    }

    // ==================== Summary & Matrix ====================

    @Transactional(readOnly = true)
    public ClassFeeSummaryResponse getClassFeeSummary(Long sessionId, Long classId) {
        Long targetSessionId = resolveSessionId(sessionId);
        SchoolClass schoolClass = classRepository.findById(classId)
                .orElseThrow(() -> new NotFoundException("শ্রেণি (ক্লাস) পাওয়া যায়নি: " + classId));
        AcademicSession session = sessionRepository.findById(targetSessionId)
                .orElseThrow(() -> new NotFoundException("শিক্ষাবর্ষ (সেশন) পাওয়া যায়নি: " + targetSessionId));

        List<FeeStructure> list = feeStructureRepository.findBySessionIdAndSchoolClassId(targetSessionId, classId);

        BigDecimal totalMonthly = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalTermly = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalYearly = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalOneTime = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        List<FeeStructureResponse> feeResponses = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            FeeStructure item = list.get(i);
            feeResponses.add(FeeStructureResponse.from(item));

            if (item.isActive()) {
                if (item.getFrequency() == FeeFrequency.MONTHLY) {
                    totalMonthly = totalMonthly.add(item.getAmount());
                } else if (item.getFrequency() == FeeFrequency.TERMLY) {
                    totalTermly = totalTermly.add(item.getAmount());
                } else if (item.getFrequency() == FeeFrequency.YEARLY) {
                    totalYearly = totalYearly.add(item.getAmount());
                } else if (item.getFrequency() == FeeFrequency.ONE_TIME) {
                    totalOneTime = totalOneTime.add(item.getAmount());
                }
            }
        }

        return new ClassFeeSummaryResponse(
                session.getId(),
                session.getName(),
                schoolClass.getId(),
                schoolClass.getName(),
                totalMonthly,
                totalTermly,
                totalYearly,
                totalOneTime,
                feeResponses.size(),
                feeResponses
        );
    }

    // ==================== Copy Across Sessions ====================

    @Transactional
    public int copySessionFees(CopyFeeStructureRequest request) {
        AcademicSession sourceSession = sessionRepository.findById(request.sourceSessionId())
                .orElseThrow(() -> new NotFoundException("উৎস শিক্ষাবর্ষ পাওয়া যায়নি: " + request.sourceSessionId()));
        AcademicSession targetSession = sessionRepository.findById(request.targetSessionId())
                .orElseThrow(() -> new NotFoundException("টার্গেট শিক্ষাবর্ষ পাওয়া যায়নি: " + request.targetSessionId()));

        if (sourceSession.getId().equals(targetSession.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "উৎস ও টার্গেট শিক্ষাবর্ষ একই হতে পারে না।");
        }

        List<FeeStructure> sourceList = feeStructureRepository.findBySessionId(sourceSession.getId());
        if (sourceList.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "উৎস সেশন (" + sourceSession.getName() + ")-এ কোনো ফি কাঠামো নির্ধারিত নেই।");
        }

        int copiedOrUpdated = 0;
        for (int i = 0; i < sourceList.size(); i++) {
            FeeStructure src = sourceList.get(i);
            Optional<FeeStructure> existingOpt = feeStructureRepository.findBySessionIdAndSchoolClassIdAndFeeHeadId(
                    targetSession.getId(), src.getSchoolClass().getId(), src.getFeeHead().getId());

            if (existingOpt.isPresent()) {
                if (request.overwriteExisting()) {
                    FeeStructure target = existingOpt.get();
                    target.setAmount(src.getAmount());
                    target.setFrequency(src.getFrequency());
                    target.setDueDay(src.getDueDay());
                    target.setDescription(src.getDescription());
                    target.setActive(src.isActive());
                    feeStructureRepository.save(target);
                    copiedOrUpdated++;
                }
            } else {
                FeeStructure target = new FeeStructure();
                target.setSession(targetSession);
                target.setSchoolClass(src.getSchoolClass());
                target.setFeeHead(src.getFeeHead());
                target.setAmount(src.getAmount());
                target.setFrequency(src.getFrequency());
                target.setDueDay(src.getDueDay());
                target.setDescription(src.getDescription());
                target.setActive(src.isActive());
                feeStructureRepository.save(target);
                copiedOrUpdated++;
            }
        }

        auditService.log("FEE_STRUCTURE_COPIED", "FEE_STRUCTURE", targetSession.getId(),
                sourceSession.getName() + " হতে " + targetSession.getName() + " সেশনে "
                        + copiedOrUpdated + " টি ফি কাঠামো অনুলিপি করা হয়েছে।");
        return copiedOrUpdated;
    }

    // ==================== Helper Methods ====================

    private FeeHead findFeeHead(Long id) {
        return feeHeadRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("ফি খাত পাওয়া যায়নি: " + id));
    }

    private FeeStructure findFeeStructure(Long id) {
        return feeStructureRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("ফি নিয়ম পাওয়া যায়নি: " + id));
    }

    private Long resolveSessionId(Long sessionId) {
        if (sessionId != null) {
            return sessionId;
        }
        return sessionRepository.findByActiveTrue()
                .map(AcademicSession::getId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "কোনো সক্রিয় শিক্ষাবর্ষ নেই, অনুগ্রহ করে সেশন আইডি দিন"));
    }
}
