package com.sms.studentmanagement.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Changes {

    private final List<String> fields = new ArrayList<>();

    public Changes track(String field, Object oldValue, Object newValue) {
        if (!Objects.equals(oldValue, newValue)) {
            fields.add(field);
        }
        return this;
    }

    public boolean isEmpty() {
        return fields.isEmpty();
    }

    public String describe() {
        return String.join(", ", fields) + " বদলেছে";
    }
}