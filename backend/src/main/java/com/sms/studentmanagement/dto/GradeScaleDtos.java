package com.sms.studentmanagement.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public final class GradeScaleDtos {

    private GradeScaleDtos() {
    }

    public record GradeBandRequest(
            @NotNull @DecimalMin("0.00") @DecimalMax("100.00") @Digits(integer = 3, fraction = 2) BigDecimal minPercent,
            @NotBlank @Size(max = 10) String grade,
            @NotNull @DecimalMin("0.00") @DecimalMax("10.00") @Digits(integer = 2, fraction = 2) BigDecimal gradePoint) {
    }

    /** পুরো তালিকা একসাথে পাঠাতে হয় (পুরনোটা বদলে নতুনটা বসে)। */
    public record GradeScaleRequest(@NotEmpty @Size(max = 20) List<@Valid GradeBandRequest> bands) {
    }

    /**
     * fromPercent থেকে toPercent পর্যন্ত। toInclusive = false মানে toPercent-এর ঠিক আগ পর্যন্ত
     * (যেমন A: ৭০ থেকে ৮০-র আগ পর্যন্ত); সবচেয়ে উঁচু গ্রেডে toPercent = ১০০ ও toInclusive = true।
     */
    public record GradeBandResponse(
            String grade, BigDecimal gradePoint, BigDecimal fromPercent, BigDecimal toPercent, boolean toInclusive) {
    }
}