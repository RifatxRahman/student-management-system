package com.sms.studentmanagement.repository;

import com.sms.studentmanagement.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubjectRepository extends JpaRepository<Subject, Long> {
}