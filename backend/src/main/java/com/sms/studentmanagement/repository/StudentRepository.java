package com.sms.studentmanagement.repository;

import com.sms.studentmanagement.entity.EnrollmentStatus;
import com.sms.studentmanagement.entity.Student;
import com.sms.studentmanagement.entity.StudentStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentRepository extends JpaRepository<Student, Long> {

    @Query("""
            select s from Student s
            where (:status is null or s.status = :status)
              and (:q is null
                   or lower(s.fullName) like lower(concat('%', :q, '%'))
                   or s.studentCode like concat('%', :q, '%'))
              and (:enrolledFilter = false or exists (
                    select 1 from Enrollment e
                    where e.student = s
                      and e.session.id = :sessionId
                      and e.status = :activeStatus
                      and (:classId is null or e.schoolClass.id = :classId)
                      and (:sectionId is null or e.section.id = :sectionId)))
            """)
    Page<Student> search(
            @Param("status") StudentStatus status,
            @Param("q") String q,
            @Param("enrolledFilter") boolean enrolledFilter,
            @Param("sessionId") Long sessionId,
            @Param("classId") Long classId,
            @Param("sectionId") Long sectionId,
            @Param("activeStatus") EnrollmentStatus activeStatus,
            Pageable pageable);

    // নেটিভ কুয়েরি @SQLRestriction মানে না, তাই মোছা শিক্ষার্থী এখান থেকেই পাওয়া যায়
    @Query(value = "select * from students where deleted_at is not null order by deleted_at desc",
            nativeQuery = true)
    List<Student> findDeleted();

    @Query(value = "select * from students where id = :id and deleted_at is not null",
            nativeQuery = true)
    Optional<Student> findDeletedById(@Param("id") Long id);
}