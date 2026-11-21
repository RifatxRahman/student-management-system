package com.sms.studentmanagement.repository;

import com.sms.studentmanagement.entity.FeeStructure;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FeeStructureRepository extends JpaRepository<FeeStructure, Long> {

    @Query("SELECT fs FROM FeeStructure fs JOIN FETCH fs.session JOIN FETCH fs.schoolClass JOIN FETCH fs.feeHead WHERE fs.session.id = :sessionId AND fs.schoolClass.id = :classId ORDER BY fs.id ASC")
    List<FeeStructure> findBySessionIdAndSchoolClassId(@Param("sessionId") Long sessionId, @Param("classId") Long classId);

    @Query("SELECT fs FROM FeeStructure fs JOIN FETCH fs.session JOIN FETCH fs.schoolClass JOIN FETCH fs.feeHead WHERE fs.session.id = :sessionId ORDER BY fs.schoolClass.displayOrder ASC, fs.feeHead.name ASC")
    List<FeeStructure> findBySessionId(@Param("sessionId") Long sessionId);

    Optional<FeeStructure> findBySessionIdAndSchoolClassIdAndFeeHeadId(Long sessionId, Long classId, Long feeHeadId);

    boolean existsByFeeHeadId(Long feeHeadId);

    boolean existsBySessionIdAndSchoolClassIdAndFeeHeadId(Long sessionId, Long classId, Long feeHeadId);

    boolean existsBySessionIdAndSchoolClassIdAndFeeHeadIdAndIdNot(Long sessionId, Long classId, Long feeHeadId, Long id);
}
