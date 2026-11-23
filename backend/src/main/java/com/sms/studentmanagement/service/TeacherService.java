package com.sms.studentmanagement.service;

import com.sms.studentmanagement.dto.PeopleDtos.AssignmentRequest;
import com.sms.studentmanagement.dto.PeopleDtos.AssignmentResponse;
import com.sms.studentmanagement.dto.PeopleDtos.TeacherRequest;
import com.sms.studentmanagement.dto.PeopleDtos.TeacherResponse;
import com.sms.studentmanagement.entity.AcademicSession;
import com.sms.studentmanagement.entity.ClassSubject;
import com.sms.studentmanagement.entity.Role;
import com.sms.studentmanagement.entity.Section;
import com.sms.studentmanagement.entity.Subject;
import com.sms.studentmanagement.entity.Teacher;
import com.sms.studentmanagement.entity.TeacherAssignment;
import com.sms.studentmanagement.entity.User;
import com.sms.studentmanagement.exception.ApiException;
import com.sms.studentmanagement.exception.NotFoundException;
import com.sms.studentmanagement.repository.AcademicSessionRepository;
import com.sms.studentmanagement.repository.ClassSubjectRepository;
import com.sms.studentmanagement.repository.SectionRepository;
import com.sms.studentmanagement.repository.SubjectRepository;
import com.sms.studentmanagement.repository.TeacherAssignmentRepository;
import com.sms.studentmanagement.repository.TeacherRepository;
import com.sms.studentmanagement.repository.UserRepository;
import com.sms.studentmanagement.util.Changes;
import com.sms.studentmanagement.util.PhoneNumbers;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final TeacherAssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    private final AcademicSessionRepository sessionRepository;
    private final SectionRepository sectionRepository;
    private final SubjectRepository subjectRepository;
    private final ClassSubjectRepository classSubjectRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<TeacherResponse> list() {
        List<Teacher> list = teacherRepository.findAllWithUser();
        List<TeacherResponse> responses = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            responses.add(TeacherResponse.from(list.get(i)));
        }
        return responses;
    }

    @Transactional(readOnly = true)
    public TeacherResponse me(Long userId) {
        return TeacherResponse.from(findByUser(userId));
    }

    @Transactional
    public TeacherResponse create(TeacherRequest request) {
        String empId = blankToNull(request.employeeId());
        if (empId != null && teacherRepository.existsByEmployeeId(empId)) {
            throw new ApiException(HttpStatus.CONFLICT, "এই কর্মচারী আইডি আগে থেকেই আছে");
        }

        Teacher teacher = new Teacher();
        teacher.setEmployeeId(empId);
        teacher.setFullName(request.fullName().trim());
        teacher.setPhone(PhoneNumbers.normalize(request.phone()));
        teacher.setEmail(blankToNull(request.email()));
        teacher.setDesignation(blankToNull(request.designation()));
        teacher.setQualification(blankToNull(request.qualification()));
        teacher.setJoiningDate(request.joiningDate());
        teacher.setAddress(blankToNull(request.address()));

        if (request.userId() != null) {
            teacher.setUser(loadLinkableUser(request.userId()));
        }
        teacherRepository.saveAndFlush(teacher);

        auditService.log("TEACHER_CREATED", "TEACHER", teacher.getId(), teacher.getFullName());
        return TeacherResponse.from(teacher);
    }

    @Transactional
    public TeacherResponse update(Long id, TeacherRequest request) {
        Teacher teacher = find(id);
        String empId = blankToNull(request.employeeId());
        String fullName = request.fullName().trim();
        String phone = PhoneNumbers.normalize(request.phone());
        String email = blankToNull(request.email());
        String designation = blankToNull(request.designation());
        String qualification = blankToNull(request.qualification());
        String address = blankToNull(request.address());

        if (empId != null && !empId.equals(teacher.getEmployeeId()) && teacherRepository.existsByEmployeeId(empId)) {
            throw new ApiException(HttpStatus.CONFLICT, "এই কর্মচারী আইডি আগে থেকেই আছে");
        }

        Changes changes = new Changes()
                .track("আইডি", teacher.getEmployeeId(), empId)
                .track("নাম", teacher.getFullName(), fullName)
                .track("ফোন", teacher.getPhone(), phone)
                .track("ইমেইল", teacher.getEmail(), email)
                .track("পদবি", teacher.getDesignation(), designation)
                .track("যোগ্যতা", teacher.getQualification(), qualification);

        teacher.setEmployeeId(empId);
        teacher.setFullName(fullName);
        teacher.setPhone(phone);
        teacher.setEmail(email);
        teacher.setDesignation(designation);
        teacher.setQualification(qualification);
        teacher.setJoiningDate(request.joiningDate());
        teacher.setAddress(address);

        boolean linkChanged = request.userId() != null
                && (teacher.getUser() == null || !teacher.getUser().getId().equals(request.userId()));
        if (linkChanged) {
            teacher.setUser(loadLinkableUser(request.userId()));
            changes.track("লগইন অ্যাকাউন্ট", null, request.userId());
        }
        teacherRepository.flush();

        if (!changes.isEmpty()) {
            auditService.log("TEACHER_UPDATED", "TEACHER", id, changes.describe());
        }
        return TeacherResponse.from(teacher);
    }

    @Transactional
    public TeacherResponse setActive(Long id, boolean active) {
        Teacher teacher = find(id);
        teacher.setActive(active);
        teacherRepository.flush();
        auditService.log(active ? "TEACHER_ACTIVATED" : "TEACHER_DEACTIVATED", "TEACHER", id, teacher.getFullName());
        return TeacherResponse.from(teacher);
    }

    // ---------- বরাদ্দ ----------
    @Transactional(readOnly = true)
    public List<AssignmentResponse> assignments(Long teacherId, Long sessionId) {
        find(teacherId);
        List<TeacherAssignment> list = assignmentRepository.findAllForTeacher(teacherId, sessionId);
        List<AssignmentResponse> responses = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            responses.add(AssignmentResponse.from(list.get(i)));
        }
        return responses;
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> myAssignments(Long userId, Long sessionId) {
        Teacher teacher = findByUser(userId);
        List<TeacherAssignment> list = assignmentRepository.findAllForTeacher(teacher.getId(), sessionId);
        List<AssignmentResponse> responses = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            responses.add(AssignmentResponse.from(list.get(i)));
        }
        return responses;
    }

    @Transactional
    public AssignmentResponse addAssignment(Long teacherId, AssignmentRequest request) {
        Teacher teacher = find(teacherId);
        if (!teacher.isActive()) {
            throw bad("নিষ্ক্রিয় শিক্ষককে বরাদ্দ দেওয়া যায় না");
        }
        AcademicSession session = sessionRepository.findById(request.sessionId())
                .orElseThrow(() -> new NotFoundException("সেশন পাওয়া যায়নি"));
        Section section = sectionRepository.findById(request.sectionId())
                .orElseThrow(() -> new NotFoundException("সেকশন পাওয়া যায়নি"));
        Subject subject = subjectRepository.findById(request.subjectId())
                .orElseThrow(() -> new NotFoundException("বিষয় পাওয়া যায়নি"));

        if (!section.isActive() || !section.getSchoolClass().isActive() || !subject.isActive()) {
            throw bad("নিষ্ক্রিয় সেকশন, ক্লাস বা বিষয়ে বরাদ্দ দেওয়া যায় না");
        }
        Long classId = section.getSchoolClass().getId();
        List<ClassSubject> classSubjects = classSubjectRepository.findAllWithSubject(classId);
        boolean subjectInClass = false;
        for (int i = 0; i < classSubjects.size(); i++) {
            if (classSubjects.get(i).getSubject().getId().equals(subject.getId())) {
                subjectInClass = true;
                break;
            }
        }
        if (!subjectInClass) {
            throw bad("এই বিষয়টি এই ক্লাসে যুক্ত নেই। আগে ক্লাসে বিষয় যোগ করুন।");
        }
        if (assignmentRepository.existsBySessionIdAndSectionIdAndSubjectId(
                session.getId(), section.getId(), subject.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, "এই সেকশনের এই বিষয়ে আগে থেকেই একজন শিক্ষক আছেন");
        }

        TeacherAssignment assignment = new TeacherAssignment();
        assignment.setTeacher(teacher);
        assignment.setSession(session);
        assignment.setSection(section);
        assignment.setSubject(subject);
        assignmentRepository.saveAndFlush(assignment);

        auditService.log("ASSIGNMENT_ADDED", "TEACHER", teacherId,
                session.getName() + " / " + section.getSchoolClass().getName() + " " + section.getName()
                        + " / " + subject.getName());
        return AssignmentResponse.from(assignment);
    }

    @Transactional
    public void removeAssignment(Long assignmentId) {
        TeacherAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new NotFoundException("বরাদ্দ পাওয়া যায়নি"));
        Long teacherId = assignment.getTeacher().getId();
        assignmentRepository.delete(assignment);
        auditService.log("ASSIGNMENT_REMOVED", "TEACHER", teacherId, "বরাদ্দ নম্বর " + assignmentId);
    }

    // ---------- ভেতরের সাহায্যকারী ----------
    private User loadLinkableUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("ব্যবহারকারী পাওয়া যায়নি"));
        if (user.getRole() != Role.TEACHER) {
            throw bad("শুধু TEACHER ভূমিকার অ্যাকাউন্ট যুক্ত করা যায়");
        }
        if (teacherRepository.existsByUserId(userId)) {
            throw new ApiException(HttpStatus.CONFLICT, "এই অ্যাকাউন্ট আগে থেকেই অন্য শিক্ষকের সাথে যুক্ত");
        }
        return user;
    }

    private Teacher find(Long id) {
        return teacherRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("শিক্ষক পাওয়া যায়নি"));
    }

    private Teacher findByUser(Long userId) {
        return teacherRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("আপনার শিক্ষক-প্রোফাইল এখনো তৈরি হয়নি"));
    }

    private static ApiException bad(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}