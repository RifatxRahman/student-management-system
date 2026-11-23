package com.sms.studentmanagement.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentTransactionStatus {
    INITIATED("শুরু হয়েছে"),
    PENDING("প্রসেসিং / ওটিপি অপেক্ষমাণ"),
    COMPLETED("সফল / পরিশোধিত"),
    FAILED("ব্যর্থ হয়েছে"),
    CANCELLED("বাতিল করা হয়েছে");

    private final String label;
}
