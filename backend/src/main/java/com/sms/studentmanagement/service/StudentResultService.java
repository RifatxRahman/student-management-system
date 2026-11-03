package com.sms.studentmanagement.service;

import com.sms.studentmanagement.dto.StudentResultDtos.StudentResultSummary;
import com.sms.studentmanagement.entity.Enrollment;
import com.sms.studentmanagement.entity.Exam;
import com.sms.studentmanagement.exception.NotFoundException;
import com.sms.studentmanagement.repository.ExamResultRepository;
import com.sms.studentmanagement.repository.StudentRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudentResultService {

    private final StudentRepository studentRepository;
    private final ExamResultRepository resultRepository;

    /** শিক্ষার্থীর সব প্রকাশিত পরীক্ষার ফল, সেশন ধরে নতুন থেকে পুরনো। (কে দেখতে পারবে, তা পথের তালায়: শুধু Admin) */
    @Transactional(readOnly = true)
    public List<StudentResultSummary> history(Long studentId) {
        studentRepository.findById(studentId)
                .orElseThrow(() -> new NotFoundException("শিক্ষার্থী পাওয়া যায়নি"));
        return resultRepository.findAllForStudent(studentId).stream()
                .map(r -> {
                    Exam exam = r.getExam();
                    Enrollment enrollment = r.getEnrollment();
                    return new StudentResultSummary(
                            r.getId(), exam.getId(), exam.getName(), exam.getExamType(),
                            exam.getSession().getId(), exam.getSession().getName(), exam.getSchoolClass().getName(),
                            enrollment.getSection().getId(), enrollment.getSection().getName(),
                            enrollment.getId(), enrollment.getRollNumber(),
                            r.getTotalMarks(), r.getTotalFullMarks(), r.getAverageMarks(),
                            r.getGpa(), r.getGrade(), r.isPassed(),
                            r.getPositionInClass(), r.getPositionInSection(), exam.getPublishedAt());
                })
                .toList();
    }
}