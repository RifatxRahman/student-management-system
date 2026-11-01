package com.sms.studentmanagement.service;

public interface SmsSender {

    /** পাঠাতে না পারলে RuntimeException ছুড়বে (তখন বার্তা FAILED হয়)। */
    void send(String phone, String message);

    /** ডেমো হলে true: আসল SMS যায় না। */
    default boolean isDemo() {
        return false;
    }
}