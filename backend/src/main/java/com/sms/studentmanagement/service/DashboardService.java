package com.sms.studentmanagement.service;

import com.sms.studentmanagement.dto.AttendanceCountRow;
import com.sms.studentmanagement.dto.DashboardDtos.AttendanceTrendItem;
import com.sms.studentmanagement.dto.DashboardDtos.DashboardSummaryResponse;
import com.sms.studentmanagement.dto.DashboardDtos.RecentActivity;
import com.sms.studentmanagement.dto.DashboardDtos.StatCards;
import com.sms.studentmanagement.dto.DashboardDtos.UpcomingHolidayItem;
import com.sms.studentmanagement.entity.AcademicSession;
import com.sms.studentmanagement.entity.AttendanceStatus;
import com.sms.studentmanagement.entity.AuditLog;
import com.sms.studentmanagement.entity.Holiday;
import com.sms.studentmanagement.repository.AcademicSessionRepository;
import com.sms.studentmanagement.repository.AttendanceRepository;
import com.sms.studentmanagement.repository.AuditLogRepository;
import com.sms.studentmanagement.repository.HolidayRepository;
import com.sms.studentmanagement.repository.SchoolClassRepository;
import com.sms.studentmanagement.repository.StudentRepository;
import com.sms.studentmanagement.repository.TeacherRepository;
import com.sms.studentmanagement.security.Actor;
import com.sms.studentmanagement.util.AttendanceMath;
import com.sms.studentmanagement.util.DhakaTime;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final AcademicSessionRepository academicSessionRepository;
    private final AttendanceRepository attendanceRepository;
    private final HolidayRepository holidayRepository;
    private final AuditLogRepository auditLogRepository;
    private final AttendanceCalendar calendar;

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary(Actor actor) {
        LocalDate today = DhakaTime.today();

        // ১. মূল পরিসংখ্যান
        long totalStudents = studentRepository.count();
        long totalTeachers = teacherRepository.count();
        long totalClasses = schoolClassRepository.count();

        String sessionName = academicSessionRepository.findByActiveTrue()
                .map(AcademicSession::getName)
                .orElse("কোনো সক্রিয় সেশন নেই");

        // আজকের হাজিরা গণনা
        boolean isHoliday = calendar.closedReason(today) != null;
        List<AttendanceCountRow> todayCounts = attendanceRepository.countSchoolWideOnDate(today);
        long todayPresent = 0;
        long todayLate = 0;
        long todayAbsent = 0;

        for (int i = 0; i < todayCounts.size(); i++) {
            AttendanceCountRow row = todayCounts.get(i);
            if (row.status() == AttendanceStatus.PRESENT) {
                todayPresent += row.count();
            } else if (row.status() == AttendanceStatus.LATE) {
                todayLate += row.count();
            } else if (row.status() == AttendanceStatus.ABSENT) {
                todayAbsent += row.count();
            }
        }

        BigDecimal todayPercent = AttendanceMath.percentage(todayPresent, todayLate, todayAbsent);

        StatCards stats = new StatCards(
                totalStudents, totalTeachers, totalClasses, sessionName,
                todayPercent, todayPresent, todayAbsent, isHoliday);

        // ২. গত ৭টি কর্মদিবসের হাজিরার ট্রেন্ড
        List<AttendanceTrendItem> trend = new ArrayList<>();
        LocalDate checkDate = today;
        int collectedDays = 0;

        while (collectedDays < 7 && !checkDate.isBefore(today.minusDays(25))) {
            if (calendar.closedReason(checkDate) == null) {
                List<AttendanceCountRow> dayCounts = attendanceRepository.countSchoolWideOnDate(checkDate);
                long p = 0;
                long lt = 0;
                long ab = 0;
                for (int i = 0; i < dayCounts.size(); i++) {
                    AttendanceCountRow r = dayCounts.get(i);
                    if (r.status() == AttendanceStatus.PRESENT) {
                        p += r.count();
                    } else if (r.status() == AttendanceStatus.LATE) {
                        lt += r.count();
                    } else if (r.status() == AttendanceStatus.ABSENT) {
                        ab += r.count();
                    }
                }
                BigDecimal pct = AttendanceMath.percentage(p, lt, ab);
                String dayName = checkDate.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);

                trend.add(new AttendanceTrendItem(checkDate, dayName, p, ab, pct));
                collectedDays++;
            }
            checkDate = checkDate.minusDays(1);
        }
        Collections.reverse(trend); // তারিখের ক্রমানুসারে সাজানো

        // ৩. সাম্প্রতিক অডিট কার্যক্রম (শেষ ৫টি)
        List<AuditLog> auditList = auditLogRepository.findTop5ByOrderByCreatedAtDesc();
        List<RecentActivity> activities = new ArrayList<>();
        for (int i = 0; i < auditList.size(); i++) {
            AuditLog a = auditList.get(i);
            activities.add(new RecentActivity(
                    a.getId(), a.getAction(), a.getEntityType(),
                    a.getDetails(), a.getActorUserId() != null ? String.valueOf(a.getActorUserId()) : "System",
                    a.getCreatedAt()));
        }

        // ৪. আসন্ন ছুটি (আগামী ৫টি)
        List<Holiday> holidayList = holidayRepository.findTop5ByHolidayDateGreaterThanEqualOrderByHolidayDateAsc(today);
        List<UpcomingHolidayItem> upcoming = new ArrayList<>();
        for (int i = 0; i < holidayList.size(); i++) {
            Holiday h = holidayList.get(i);
            upcoming.add(new UpcomingHolidayItem(h.getId(), h.getHolidayDate(), h.getName()));
        }

        return new DashboardSummaryResponse(stats, trend, activities, upcoming);
    }
}
