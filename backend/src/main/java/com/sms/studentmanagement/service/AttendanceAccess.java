package com.sms.studentmanagement.service;

import com.sms.studentmanagement.entity.Teacher;
import com.sms.studentmanagement.exception.ApiException;
import com.sms.studentmanagement.repository.TeacherAssignmentRepository;
import com.sms.studentmanagement.repository.TeacherRepository;
import com.sms.studentmanagement.security.Actor;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AttendanceAccess {

    private final TeacherRepository teacherRepository;
    private final TeacherAssignmentRepository assignmentRepository;

    /** শিক্ষকের প্রোফাইল আছে ও সক্রিয়, না হলে 403। */
    public Teacher activeTeacher(Actor actor) {
        Teacher teacher = teacherRepository.findByUserId(actor.userId())
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "আপনার শিক্ষক-প্রোফাইল তৈরি হয়নি"));
        if (!teacher.isActive()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "আপনার শিক্ষক-প্রোফাইল নিষ্ক্রিয়");
        }
        return teacher;
    }

    /** Admin হলে ছাড়। শিক্ষক হলে এই সেকশনে তাঁর বরাদ্দ থাকতেই হবে, না হলে 403। */
    public void requireSection(Actor actor, Long sectionId, LocalDate from, LocalDate to) {
        if (actor.admin()) {
            return;
        }
        Teacher teacher = activeTeacher(actor);
        if (assignmentRepository.countCoverage(teacher.getId(), sectionId, from, to) == 0) {
            throw new ApiException(HttpStatus.FORBIDDEN, "এই সেকশনে আপনার হাজিরা নেওয়ার অনুমতি নেই");
        }
    }
}