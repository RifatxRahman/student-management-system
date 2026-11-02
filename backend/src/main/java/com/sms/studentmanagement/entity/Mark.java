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
@Table(name = "marks")
@Getter
@Setter
@NoArgsConstructor
public class Mark extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_subject_id", nullable = false)
    private ExamSubject examSubject;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private Enrollment enrollment;

    /** অনুপস্থিত হলে null */
    @Column(name = "marks_obtained", precision = 6, scale = 2)
    private BigDecimal marksObtained;

    @Column(name = "is_absent", nullable = false)
    private boolean absent;

    @Column(name = "entered_by", nullable = false)
    private Long enteredBy;
}