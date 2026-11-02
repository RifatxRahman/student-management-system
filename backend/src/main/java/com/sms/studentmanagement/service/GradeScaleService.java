package com.sms.studentmanagement.service;

import com.sms.studentmanagement.dto.GradeScaleDtos.GradeBandRequest;
import com.sms.studentmanagement.dto.GradeScaleDtos.GradeBandResponse;
import com.sms.studentmanagement.dto.GradeScaleDtos.GradeScaleRequest;
import com.sms.studentmanagement.entity.GradeScale;
import com.sms.studentmanagement.repository.GradeScaleRepository;
import com.sms.studentmanagement.util.GradeBand;
import com.sms.studentmanagement.util.GradeScaleRules;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GradeScaleService {

    private static final BigDecimal HUNDRED = new BigDecimal("100.00");

    private final GradeScaleRepository repository;
    private final AuditService auditService;

    /** রেজাল্ট হিসাবের জন্য (২খ-তে কাজে লাগবে)। */
    @Transactional(readOnly = true)
    public List<GradeBand> currentBands() {
        return repository.findAllByOrderByMinPercentDesc().stream()
                .map(s -> new GradeBand(s.getMinPercent(), s.getGrade(), s.getGradePoint()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<GradeBandResponse> get() {
        return toResponses(repository.findAllByOrderByMinPercentDesc());
    }

    @Transactional
    public List<GradeBandResponse> replace(GradeScaleRequest request) {
        List<GradeBand> bands = request.bands().stream()
                .map(b -> new GradeBand(
                        b.minPercent().setScale(2), b.grade().trim(), b.gradePoint().setScale(2)))
                .toList();
        GradeScaleRules.validate(bands);

        repository.deleteAllInBatch();
        List<GradeScale> rows = new ArrayList<>();
        for (GradeBand band : bands) {
            GradeScale row = new GradeScale();
            row.setMinPercent(band.minPercent());
            row.setGrade(band.grade());
            row.setGradePoint(band.gradePoint());
            rows.add(row);
        }
        repository.saveAll(rows);

        String summary = bands.stream()
                .sorted((a, b) -> b.minPercent().compareTo(a.minPercent()))
                .map(b -> b.grade() + " " + b.minPercent().stripTrailingZeros().toPlainString() + "%+ = " + b.gradePoint())
                .collect(Collectors.joining(", "));
        auditService.log("GRADE_SCALE_CHANGED", "GRADE_SCALE", null, summary);

        return toResponses(repository.findAllByOrderByMinPercentDesc());
    }

    private List<GradeBandResponse> toResponses(List<GradeScale> highestFirst) {
        List<GradeBandResponse> result = new ArrayList<>();
        BigDecimal upper = HUNDRED;
        boolean inclusive = true;
        for (GradeScale scale : highestFirst) {
            result.add(new GradeBandResponse(
                    scale.getGrade(), scale.getGradePoint(), scale.getMinPercent(), upper, inclusive));
            upper = scale.getMinPercent();
            inclusive = false;
        }
        return result;
    }
}