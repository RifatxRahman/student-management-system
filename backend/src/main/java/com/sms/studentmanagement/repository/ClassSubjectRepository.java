package com.sms.studentmanagement.repository;

import com.sms.studentmanagement.entity.ClassSubject;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClassSubjectRepository extends JpaRepository<ClassSubject, Long> {

    @Query("select cs from ClassSubject cs join fetch cs.subject "
            + "where cs.schoolClass.id = :classId order by cs.subject.name")
    List<ClassSubject> findAllWithSubject(@Param("classId") Long classId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from ClassSubject cs where cs.schoolClass.id = :classId and cs.subject.id = :subjectId")
    int deleteAssignment(@Param("classId") Long classId, @Param("subjectId") Long subjectId);
}