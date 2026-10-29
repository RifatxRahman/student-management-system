package com.sms.studentmanagement.repository;

import com.sms.studentmanagement.entity.SchoolClass;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchoolClassRepository extends JpaRepository<SchoolClass, Long> {
}