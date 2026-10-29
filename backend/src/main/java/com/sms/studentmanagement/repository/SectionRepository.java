package com.sms.studentmanagement.repository;

import com.sms.studentmanagement.entity.Section;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SectionRepository extends JpaRepository<Section, Long> {

    List<Section> findBySchoolClassIdOrderByName(Long classId);
}