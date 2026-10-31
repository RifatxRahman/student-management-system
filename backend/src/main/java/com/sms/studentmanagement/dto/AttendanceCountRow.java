package com.sms.studentmanagement.dto;

import com.sms.studentmanagement.entity.AttendanceStatus;

public record AttendanceCountRow(Long enrollmentId, AttendanceStatus status, Long count) {
}