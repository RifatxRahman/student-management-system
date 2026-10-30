package com.sms.studentmanagement.repository;

import com.sms.studentmanagement.entity.Teacher;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    Optional<Teacher> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    @Query("select t from Teacher t left join fetch t.user order by t.fullName")
    List<Teacher> findAllWithUser();
}