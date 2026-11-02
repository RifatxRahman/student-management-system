package com.sms.studentmanagement.repository;

import com.sms.studentmanagement.entity.GradeScale;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GradeScaleRepository extends JpaRepository<GradeScale, Long> {

    List<GradeScale> findAllByOrderByMinPercentDesc();
}