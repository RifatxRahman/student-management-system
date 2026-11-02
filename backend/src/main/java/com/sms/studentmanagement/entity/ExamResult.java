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
@Table(name = "results")
@Getter
@Setter
@NoArgsConstructor
public class ExamResult extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private Enrollment enrollment;

    @Column(name = "total_marks", nullable = false, precision = 8, scale = 2)
    private BigDecimal totalMarks;

    @Column(name = "total_full_marks", nullable = false, precision = 8, scale = 2)
    private BigDecimal totalFullMarks;

    @Column(name = "average_marks", nullable = false, precision = 6, scale = 2)
    private BigDecimal averageMarks;

    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal gpa;

    @Column(nullable = false, length = 10)
    private String grade;

    @Column(name = "is_passed", nullable = false)
    private boolean passed;

    /** ফেল হলে null */
    @Column(name = "position_in_class")
    private Integer positionInClass;

    @Column(name = "position_in_section")
    private Integer positionInSection;
}