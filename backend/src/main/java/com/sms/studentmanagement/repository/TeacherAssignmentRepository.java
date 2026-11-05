package com.sms.studentmanagement.repository;

import com.sms.studentmanagement.entity.TeacherAssignment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;

public interface TeacherAssignmentRepository extends JpaRepository<TeacherAssignment, Long> {

    boolean existsBySessionIdAndSectionIdAndSubjectId(Long sessionId, Long sectionId, Long subjectId);

    @Query("""
            select ta from TeacherAssignment ta
            join fetch ta.session ses
            join fetch ta.section sec
            join fetch sec.schoolClass cls
            join fetch ta.subject sub
            where ta.teacher.id = :teacherId
              and (:sessionId is null or ses.id = :sessionId)
            order by ses.startDate desc, cls.displayOrder, sec.name, sub.name
            """)
    List<TeacherAssignment> findAllForTeacher(
            @Param("teacherId") Long teacherId, @Param("sessionId") Long sessionId);

    /** এই শিক্ষকের এই সেকশনে (ওই সময়ে চালু সেশনে) কয়টা বরাদ্দ আছে। 0 মানে অনুমতি নেই। */
    @Query("""
            select count(ta) from TeacherAssignment ta
            where ta.teacher.id = :teacherId
              and ta.section.id = :sectionId
              and ta.session.startDate <= :toDate
              and ta.session.endDate >= :fromDate
            """)
    long countCoverage(
            @Param("teacherId") Long teacherId,
            @Param("sectionId") Long sectionId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    @Query("""
            select ta from TeacherAssignment ta
            join fetch ta.section sec
            join fetch sec.schoolClass cls
            where ta.teacher.id = :teacherId
              and ta.session.startDate <= :date
              and ta.session.endDate >= :date
            order by cls.displayOrder, sec.name
            """)
    List<TeacherAssignment> findForTeacherOnDate(
            @Param("teacherId") Long teacherId, @Param("date") LocalDate date);


    boolean existsByTeacherIdAndSessionIdAndSectionIdAndSubjectId(
            Long teacherId, Long sessionId, Long sectionId, Long subjectId);

    boolean existsByTeacherIdAndSessionIdAndSectionId(Long teacherId, Long sessionId, Long sectionId);


    /** এই সেশনে এই সেকশনে কোন (সক্রিয়) শিক্ষক কোন (সক্রিয়) বিষয় পড়ান। রুটিনের বাছাই-তালিকার উৎস। */
    @Query("""
            select ta from TeacherAssignment ta
            join fetch ta.teacher t
            join fetch ta.subject s
            where ta.session.id = :sessionId
              and ta.section.id = :sectionId
              and t.active = true
              and s.active = true
            order by s.name, t.fullName
            """)
    List<TeacherAssignment> findForSectionAndSession(
            @Param("sessionId") Long sessionId, @Param("sectionId") Long sectionId);
}