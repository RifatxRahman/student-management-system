package com.sms.studentmanagement.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sms.studentmanagement.exception.ApiException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class GradeScaleRulesTest {

    private static GradeBand band(String min, String grade, String point) {
        return new GradeBand(new BigDecimal(min), grade, new BigDecimal(point));
    }

    @Test
    void defaultStyleScaleIsValid() {
        List<GradeBand> scale = List.of(
                band("80", "A+", "5.00"), band("70", "A", "4.00"), band("60", "A-", "3.50"),
                band("50", "B", "3.00"), band("40", "C", "2.00"), band("33", "D", "1.00"), band("0", "F", "0.00"));
        assertDoesNotThrow(() -> GradeScaleRules.validate(scale));
    }

    @Test
    void singleGradeIsRejected() {
        assertThrows(ApiException.class, () -> GradeScaleRules.validate(List.of(band("0", "F", "0.00"))));
    }

    @Test
    void mustStartFromZeroPercent() {
        List<GradeBand> scale = List.of(band("80", "A", "5.00"), band("33", "D", "1.00"));
        assertThrows(ApiException.class, () -> GradeScaleRules.validate(scale));
    }

    @Test
    void duplicateStartIsRejected() {
        List<GradeBand> scale = List.of(band("0", "F", "0.00"), band("50", "B", "3.00"), band("50.00", "C", "2.00"));
        assertThrows(ApiException.class, () -> GradeScaleRules.validate(scale));
    }

    @Test
    void duplicateGradeNameIsRejectedEvenWithDifferentCase() {
        List<GradeBand> scale = List.of(band("0", "F", "0.00"), band("50", "b", "3.00"), band("80", "B", "5.00"));
        assertThrows(ApiException.class, () -> GradeScaleRules.validate(scale));
    }

    @Test
    void higherMarksCannotGetLowerGradePoint() {
        List<GradeBand> scale = List.of(band("0", "F", "0.00"), band("40", "C", "2.00"), band("80", "A", "1.00"));
        assertThrows(ApiException.class, () -> GradeScaleRules.validate(scale));
    }

    @Test
    void equalGradePointsAreAllowed() {
        List<GradeBand> scale = List.of(band("0", "F", "0.00"), band("40", "C", "2.00"), band("60", "B", "2.00"));
        assertDoesNotThrow(() -> GradeScaleRules.validate(scale));
    }
}