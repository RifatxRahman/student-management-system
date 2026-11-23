package com.sms.studentmanagement.service;

import com.sms.studentmanagement.dto.TeacherAttendanceDtos.LeaveApplicationRequest;
import com.sms.studentmanagement.dto.TeacherAttendanceDtos.LeaveApplicationResponse;
import com.sms.studentmanagement.entity.Holiday;
import com.sms.studentmanagement.entity.LeaveApplication;
import com.sms.studentmanagement.entity.LeaveStatus;
import com.sms.studentmanagement.entity.Teacher;
import com.sms.studentmanagement.entity.TeacherAttendance;
import com.sms.studentmanagement.entity.TeacherAttendanceStatus;
import com.sms.studentmanagement.entity.User;
import com.sms.studentmanagement.exception.ApiException;
import com.sms.studentmanagement.exception.NotFoundException;
import com.sms.studentmanagement.repository.HolidayRepository;
import com.sms.studentmanagement.repository.LeaveApplicationRepository;
import com.sms.studentmanagement.repository.TeacherAttendanceRepository;
import com.sms.studentmanagement.repository.TeacherRepository;
import com.sms.studentmanagement.repository.UserRepository;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LeaveApplicationService {

    private final LeaveApplicationRepository leaveRepository;
    private final TeacherRepository teacherRepository;
    private final TeacherAttendanceRepository attendanceRepository;
    private final HolidayRepository holidayRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<LeaveApplicationResponse> listAll(LeaveStatus status) {
        List<LeaveApplication> list;
        if (status != null) {
            list = leaveRepository.findByStatusWithTeacher(status);
        } else {
            list = leaveRepository.findAllWithTeacher();
        }

        Map<Long, String> userNames = new HashMap<>();
        List<LeaveApplicationResponse> responses = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            LeaveApplication l = list.get(i);
            String actionByName = resolveUserName(l.getActionBy(), userNames);
            responses.add(LeaveApplicationResponse.from(l, actionByName));
        }
        return responses;
    }

    @Transactional(readOnly = true)
    public List<LeaveApplicationResponse> listMyLeaves(Long userId) {
        Teacher teacher = teacherRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("আপনার শিক্ষক প্রোফাইল পাওয়া যায়নি"));

        List<LeaveApplication> list = leaveRepository.findByTeacherIdWithTeacher(teacher.getId());
        Map<Long, String> userNames = new HashMap<>();
        List<LeaveApplicationResponse> responses = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            LeaveApplication l = list.get(i);
            String actionByName = resolveUserName(l.getActionBy(), userNames);
            responses.add(LeaveApplicationResponse.from(l, actionByName));
        }
        return responses;
    }

    @Transactional
    public LeaveApplicationResponse apply(LeaveApplicationRequest request, Long currentUserId, boolean isAdmin) {
        Teacher teacher;
        if (isAdmin && request.teacherId() != null) {
            teacher = teacherRepository.findById(request.teacherId())
                    .orElseThrow(() -> new NotFoundException("শিক্ষক পাওয়া যায়নি"));
        } else {
            teacher = teacherRepository.findByUserId(currentUserId)
                    .orElseThrow(() -> new NotFoundException("আপনার শিক্ষক প্রোফাইল পাওয়া যায়নি"));
        }

        if (request.startDate().isAfter(request.endDate())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "শুরুর তারিখ শেষের তারিখের চেয়ে পরে হতে পারে না");
        }

        if (leaveRepository.hasOverlappingLeave(teacher.getId(), request.startDate(), request.endDate())) {
            throw new ApiException(HttpStatus.CONFLICT, "এই নির্ধারিত তারিখে শিক্ষকের ছুটির আবেদন আগে থেকেই বিদ্যমান আছে");
        }

        BigDecimal days = request.totalDays();
        if (days == null || days.compareTo(BigDecimal.ZERO) <= 0) {
            long count = ChronoUnit.DAYS.between(request.startDate(), request.endDate()) + 1;
            days = BigDecimal.valueOf(count);
        }

        LeaveApplication application = new LeaveApplication();
        application.setTeacher(teacher);
        application.setLeaveType(request.leaveType());
        application.setStartDate(request.startDate());
        application.setEndDate(request.endDate());
        application.setTotalDays(days);
        application.setReason(request.reason().trim());
        application.setStatus(LeaveStatus.PENDING);
        application.setAppliedAt(Instant.now());

        leaveRepository.save(application);

        auditService.log("LEAVE_APPLIED", "LEAVE", application.getId(),
                teacher.getFullName() + " - " + request.leaveType() + " (" + days + " দিন)");

        return LeaveApplicationResponse.from(application, null);
    }

    @Transactional
    public LeaveApplicationResponse approve(Long leaveId, Long adminUserId, String note) {
        LeaveApplication leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new NotFoundException("ছুটির আবেদন পাওয়া যায়নি"));

        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "শুধুমাত্র অপেক্ষমান (PENDING) আবেদন অনুমোদন করা যায়");
        }

        leave.setStatus(LeaveStatus.APPROVED);
        leave.setActionBy(adminUserId);
        leave.setActionAt(Instant.now());
        leave.setActionReason(note != null ? note.trim() : "অনুমোদিত");
        leaveRepository.save(leave);

        // হাজিরার রেকর্ডে স্বয়ংক্রিয়ভাবে LEAVE স্ট্যাটাস সিঙ্ক
        syncApprovedLeaveToAttendance(leave, adminUserId);

        User adminUser = userRepository.findById(adminUserId).orElse(null);
        String adminName = adminUser != null ? adminUser.getFullName() : "Admin";

        auditService.log("LEAVE_APPROVED", "LEAVE", leave.getId(),
                leave.getTeacher().getFullName() + " - " + leave.getLeaveType() + " (" + leave.getTotalDays() + " দিন)");

        return LeaveApplicationResponse.from(leave, adminName);
    }

    @Transactional
    public LeaveApplicationResponse reject(Long leaveId, Long adminUserId, String reason) {
        LeaveApplication leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new NotFoundException("ছুটির আবেদন পাওয়া যায়নি"));

        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "শুধুমাত্র অপেক্ষমান (PENDING) আবেদন প্রত্যাখ্যান করা যায়");
        }

        if (reason == null || reason.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "প্রত্যাখ্যানের কারণ উল্লেখ করতে হবে");
        }

        leave.setStatus(LeaveStatus.REJECTED);
        leave.setActionBy(adminUserId);
        leave.setActionAt(Instant.now());
        leave.setActionReason(reason.trim());
        leaveRepository.save(leave);

        User adminUser = userRepository.findById(adminUserId).orElse(null);
        String adminName = adminUser != null ? adminUser.getFullName() : "Admin";

        auditService.log("LEAVE_REJECTED", "LEAVE", leave.getId(),
                leave.getTeacher().getFullName() + " - কারণ: " + reason.trim());

        return LeaveApplicationResponse.from(leave, adminName);
    }

    @Transactional
    public LeaveApplicationResponse cancel(Long leaveId, Long currentUserId, boolean isAdmin) {
        LeaveApplication leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new NotFoundException("ছুটির আবেদন পাওয়া যায়নি"));

        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "শুধুমাত্র অপেক্ষমান (PENDING) আবেদন বাতিল করা যায়");
        }

        if (!isAdmin) {
            Teacher teacher = leave.getTeacher();
            if (teacher.getUser() == null || !teacher.getUser().getId().equals(currentUserId)) {
                throw new ApiException(HttpStatus.FORBIDDEN, "আপনি শুধু নিজের ছুটির আবেদন বাতিল করতে পারেন");
            }
        }

        leave.setStatus(LeaveStatus.CANCELLED);
        leave.setActionBy(currentUserId);
        leave.setActionAt(Instant.now());
        leave.setActionReason("আবেদনকারী কর্তৃক বাতিলকৃত");
        leaveRepository.save(leave);

        auditService.log("LEAVE_CANCELLED", "LEAVE", leave.getId(), leave.getTeacher().getFullName());

        return LeaveApplicationResponse.from(leave, null);
    }

    private void syncApprovedLeaveToAttendance(LeaveApplication leave, Long adminUserId) {
        LocalDate start = leave.getStartDate();
        LocalDate end = leave.getEndDate();
        Long teacherId = leave.getTeacher().getId();

        List<Holiday> holidays = holidayRepository.findByHolidayDateBetweenOrderByHolidayDate(start, end);
        Set<LocalDate> holidayDates = new HashSet<>();
        for (int i = 0; i < holidays.size(); i++) {
            holidayDates.add(holidays.get(i).getHolidayDate());
        }

        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            // ছুটির দিন ও শুক্রবার এড়িয়ে হাজিরার দিনে LEAVE হিসেবে সেভ করা
            if (d.getDayOfWeek() == DayOfWeek.FRIDAY || holidayDates.contains(d)) {
                continue;
            }

            Optional<TeacherAttendance> existingOpt = attendanceRepository.findByTeacherIdAndAttendanceDate(teacherId, d);
            TeacherAttendance att;
            if (existingOpt.isPresent()) {
                att = existingOpt.get();
            } else {
                att = new TeacherAttendance();
                att.setTeacher(leave.getTeacher());
                att.setAttendanceDate(d);
            }

            att.setStatus(TeacherAttendanceStatus.LEAVE);
            att.setRemark("অনুমোদিত ছুটি: " + leave.getLeaveType().name());
            att.setMarkedBy(adminUserId);
            attendanceRepository.save(att);
        }
    }

    private String resolveUserName(Long userId, Map<Long, String> cache) {
        if (userId == null) {
            return null;
        }
        if (cache.containsKey(userId)) {
            return cache.get(userId);
        }
        String name = userRepository.findById(userId).map(User::getFullName).orElse("User #" + userId);
        cache.put(userId, name);
        return name;
    }
}
