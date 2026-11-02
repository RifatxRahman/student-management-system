package com.sms.studentmanagement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "result_subjects")
@Getter
@Setter
@NoArgsConstructor
public class ResultSubject extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "result_id", nullable = false)
    private ExamResult result;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_subject_id", nullable = false)
    private ExamSubject examSubject;

    @Column(name = "marks_obtained", precision = 6, scale = 2)
    private BigDecimal marksObtained;

    @Column(name = "is_absent", nullable = false)
    private boolean absent;

    @Column(nullable = false, length = 10)
    private String grade;

    @Column(name = "grade_point", nullable = false, precision = 3, scale = 2)
    private BigDecimal gradePoint;

    @Column(name = "is_passed", nullable = false)
    private boolean passed;
}