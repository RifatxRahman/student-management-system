package com.sms.studentmanagement.repository;

import com.sms.studentmanagement.entity.SchoolClass;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchoolClassRepository extends JpaRepository<SchoolClass, Long> {

    List<SchoolClass> findAllByOrderByDisplayOrderAsc();
}