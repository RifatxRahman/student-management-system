package com.sms.studentmanagement.dto;

import com.sms.studentmanagement.entity.AuditLog;
import com.sms.studentmanagement.entity.Enrollment;
import com.sms.studentmanagement.entity.EnrollmentStatus;
import com.sms.studentmanagement.entity.Gender;
import com.sms.studentmanagement.entity.Guardian;
import com.sms.studentmanagement.entity.GuardianRelation;
import com.sms.studentmanagement.entity.Student;
import com.sms.studentmanagement.entity.StudentStatus;
import com.sms.studentmanagement.entity.Teacher;
import com.sms.studentmanagement.entity.TeacherAssignment;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class PeopleDtos {

    private PeopleDtos() {
    }

    // ---------- শিক্ষক ----------
    public record TeacherRequest(
            @NotBlank @Size(max = 100) String fullName,
            @Size(max = 30) String phone,
            @Size(max = 100) String designation,
            Long userId) {
    }

    public record TeacherResponse(
            Long id, Long userId, String username, String fullName, String phone, String designation,
            boolean active) {

        public static TeacherResponse from(Teacher t) {
            return new TeacherResponse(
                    t.getId(),
                    t.getUser() == null ? null : t.getUser().getId(),
                    t.getUser() == null ? null : t.getUser().getUsername(),
                    t.getFullName(), t.getPhone(), t.getDesignation(), t.isActive());
        }
    }

    public record AssignmentRequest(
            @NotNull Long sessionId, @NotNull Long sectionId, @NotNull Long subjectId) {
    }

    public record AssignmentResponse(
            Long id, Long sessionId, String sessionName, Long classId, String className,
            Long sectionId, String sectionName, Long subjectId, String subjectName, String subjectCode) {

        public static AssignmentResponse from(TeacherAssignment a) {
            return new AssignmentResponse(
                    a.getId(),
                    a.getSession().getId(), a.getSession().getName(),
                    a.getSection().getSchoolClass().getId(), a.getSection().getSchoolClass().getName(),
                    a.getSection().getId(), a.getSection().getName(),
                    a.getSubject().getId(), a.getSubject().getName(), a.getSubject().getCode());
        }
    }

    // ---------- অভিভাবক ----------
    public record GuardianRequest(
            @NotBlank @Size(max = 100) String fullName,
            @NotBlank @Size(max = 30) String phone,
            @Size(max = 30) String altPhone,
            @Email @Size(max = 100) String email,
            @Size(max = 255) String address) {
    }

    public record GuardianResponse(
            Long id, String fullName, String phone, String altPhone, String email, String address) {

        public static GuardianResponse from(Guardian g) {
            return new GuardianResponse(
                    g.getId(), g.getFullName(), g.getPhone(), g.getAltPhone(), g.getEmail(), g.getAddress());
        }
    }

    public record ChildLink(
            Long studentId, String studentCode, String fullName, GuardianRelation relation, boolean primary) {
    }

    public record GuardianDetail(GuardianResponse guardian, List<ChildLink> children) {
    }

    // ---------- শিক্ষার্থী ----------
    public record StudentCreateRequest(
            @NotBlank @Size(max = 100) String fullName,
            @NotNull @Past LocalDate dateOfBirth,
            @NotNull Gender gender,
            @Size(max = 30) String phone,
            @Size(max = 255) String address,
            LocalDate admissionDate,
            @NotNull Long guardianId,
            @NotNull GuardianRelation guardianRelation) {
    }

    public record StudentUpdateRequest(
            @NotBlank @Size(max = 100) String fullName,
            @NotNull @Past LocalDate dateOfBirth,
            @NotNull Gender gender,
            @Size(max = 30) String phone,
            @Size(max = 255) String address) {
    }

    public record StatusRequest(@NotNull StudentStatus status) {
    }

    public record LinkGuardianRequest(
            @NotNull Long guardianId, @NotNull GuardianRelation relation, boolean primary) {
    }

    public record StudentResponse(
            Long id, String studentCode, String fullName, LocalDate dateOfBirth, Gender gender,
            String phone, String address, LocalDate admissionDate, StudentStatus status) {

        public static StudentResponse from(Student s) {
            return new StudentResponse(
                    s.getId(), s.getStudentCode(), s.getFullName(), s.getDateOfBirth(), s.getGender(),
                    s.getPhone(), s.getAddress(), s.getAdmissionDate(), s.getStatus());
        }
    }

    public record StudentSummary(
            Long id, String studentCode, String fullName, Gender gender, StudentStatus status,
            String className, String sectionName, Integer rollNumber) {
    }

    public record GuardianLink(
            Long guardianId, String fullName, String phone, GuardianRelation relation, boolean primary) {
    }

    public record StudentDetail(
            StudentResponse student, List<GuardianLink> guardians, List<EnrollmentResponse> enrollments) {
    }

    // ---------- ভর্তি (Enrollment) ----------
    public record EnrollRequest(
            @NotNull Long sessionId,
            @NotNull Long classId,
            @NotNull Long sectionId,
            @Min(1) Integer rollNumber,
            LocalDate startDate) {
    }

    public record TransferRequest(
            @NotNull Long classId,
            @NotNull Long sectionId,
            @Min(1) Integer rollNumber,
            LocalDate effectiveDate) {
    }

    public record WithdrawRequest(LocalDate endDate) {
    }

    public record EnrollmentResponse(
            Long id, Long studentId, Long sessionId, String sessionName, Long classId, String className,
            Long sectionId, String sectionName, int rollNumber, EnrollmentStatus status,
            LocalDate startDate, LocalDate endDate) {

        public static EnrollmentResponse from(Enrollment e) {
            return new EnrollmentResponse(
                    e.getId(), e.getStudent().getId(),
                    e.getSession().getId(), e.getSession().getName(),
                    e.getSchoolClass().getId(), e.getSchoolClass().getName(),
                    e.getSection().getId(), e.getSection().getName(),
                    e.getRollNumber(), e.getStatus(), e.getStartDate(), e.getEndDate());
        }
    }

    // ---------- Audit Log ----------
    public record AuditLogResponse(
            Long id, Instant createdAt, Long actorUserId, String actorUsername, String action,
            String entityType, Long entityId, String details) {

        public static AuditLogResponse from(AuditLog a, String actorUsername) {
            return new AuditLogResponse(
                    a.getId(), a.getCreatedAt(), a.getActorUserId(), actorUsername, a.getAction(),
                    a.getEntityType(), a.getEntityId(), a.getDetails());
        }
    }
}