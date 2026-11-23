package com.sms.studentmanagement.repository;

import com.sms.studentmanagement.entity.Section;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SectionRepository extends JpaRepository<Section, Long> {

    List<Section> findBySchoolClassIdOrderByName(Long classId);

    Optional<Section> findBySchoolClassIdAndNameIgnoreCase(Long classId, String name);
}