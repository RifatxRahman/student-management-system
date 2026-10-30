package com.sms.studentmanagement.repository;

import com.sms.studentmanagement.entity.Guardian;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GuardianRepository extends JpaRepository<Guardian, Long> {

    @Query("""
            select g from Guardian g
            where lower(g.fullName) like lower(concat('%', :q, '%'))
               or g.phone like concat('%', :q, '%')
            order by g.fullName
            """)
    List<Guardian> search(@Param("q") String q, Pageable pageable);
}