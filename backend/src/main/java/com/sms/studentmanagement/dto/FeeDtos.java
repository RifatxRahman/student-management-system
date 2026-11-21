package com.sms.studentmanagement.dto;

import com.sms.studentmanagement.entity.FeeFrequency;
import com.sms.studentmanagement.entity.FeeHead;
import com.sms.studentmanagement.entity.FeeStructure;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public final class FeeDtos {

    private FeeDtos() {}

    public record FeeHeadRequest(
            @NotBlank(message = "ফি খাতের নাম দেওয়া আবশ্যক") String name,
            @NotBlank(message = "ফি খাতের কোড দেওয়া আবশ্যক") String code,
            String description,
            Boolean active
    ) {}

    public record FeeHeadResponse(
            Long id,
            String name,
            String code,
            String description,
            boolean active
    ) {
        public static FeeHeadResponse from(FeeHead head) {
            return new FeeHeadResponse(
                    head.getId(),
                    head.getName(),
                    head.getCode(),
                    head.getDescription(),
                    head.isActive()
            );
        }
    }

    public record FeeStructureRequest(
            @NotNull(message = "সেশন নির্বাচন আবশ্যক") Long sessionId,
            @NotNull(message = "ক্লাস নির্বাচন আবশ্যক") Long classId,
            @NotNull(message = "ফি খাত নির্বাচন আবশ্যক") Long feeHeadId,
            @NotNull(message = "টাকার পরিমাণ আবশ্যক")
            @DecimalMin(value = "0.00", message = "ফি ঋণাত্মক হতে পারে না")
            BigDecimal amount,
            @NotNull(message = "ফ্রিকোয়েন্সি নির্বাচন আবশ্যক") FeeFrequency frequency,
            Integer dueDay,
            String description,
            Boolean active
    ) {}

    public record FeeStructureResponse(
            Long id,
            Long sessionId,
            String sessionName,
            Long classId,
            String className,
            Long feeHeadId,
            String feeHeadName,
            String feeHeadCode,
            BigDecimal amount,
            FeeFrequency frequency,
            String frequencyBangla,
            int dueDay,
            String description,
            boolean active
    ) {
        public static FeeStructureResponse from(FeeStructure fs) {
            return new FeeStructureResponse(
                    fs.getId(),
                    fs.getSession().getId(),
                    fs.getSession().getName(),
                    fs.getSchoolClass().getId(),
                    fs.getSchoolClass().getName(),
                    fs.getFeeHead().getId(),
                    fs.getFeeHead().getName(),
                    fs.getFeeHead().getCode(),
                    fs.getAmount(),
                    fs.getFrequency(),
                    fs.getFrequency().getBanglaLabel(),
                    fs.getDueDay(),
                    fs.getDescription(),
                    fs.isActive()
            );
        }
    }

    public record ClassFeeSummaryResponse(
            Long sessionId,
            String sessionName,
            Long classId,
            String className,
            BigDecimal totalMonthlyAmount,
            BigDecimal totalTermlyAmount,
            BigDecimal totalYearlyAmount,
            BigDecimal totalOneTimeAmount,
            int totalHeadsCount,
            List<FeeStructureResponse> feeItems
    ) {}

    public record CopyFeeStructureRequest(
            @NotNull(message = "উৎস সেশন নির্বাচন আবশ্যক") Long sourceSessionId,
            @NotNull(message = "টার্গেট সেশন নির্বাচন আবশ্যক") Long targetSessionId,
            boolean overwriteExisting
    ) {}
}
