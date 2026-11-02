package com.sms.studentmanagement.util;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MeritRanker {

    private MeritRanker() {
    }

    public record Entry(Long id, boolean passed, BigDecimal gpa, BigDecimal totalMarks) {
    }

    /**
     * শুধু পাস করাদের অবস্থান (১ থেকে)। আগে GPA, সমান হলে মোট নম্বর। দুটোই সমান হলে একই অবস্থান,
     * পরেরজন সেই হিসেবে লাফ দেয় (১, ২, ২, ৪)। ফেলদের কোনো অবস্থান নেই, ফলে ম্যাপে তারা থাকে না।
     */
    public static Map<Long, Integer> rank(List<Entry> entries) {
        Comparator<Entry> order = Comparator
                .comparing(Entry::gpa, Comparator.reverseOrder())
                .thenComparing(Entry::totalMarks, Comparator.reverseOrder());
        List<Entry> passed = entries.stream().filter(Entry::passed).sorted(order).toList();

        Map<Long, Integer> positions = new LinkedHashMap<>();
        int previousPosition = 0;
        Entry previous = null;
        for (int i = 0; i < passed.size(); i++) {
            Entry current = passed.get(i);
            boolean tie = previous != null
                    && previous.gpa().compareTo(current.gpa()) == 0
                    && previous.totalMarks().compareTo(current.totalMarks()) == 0;
            int position = tie ? previousPosition : i + 1;
            positions.put(current.id(), position);
            previous = current;
            previousPosition = position;
        }
        return positions;
    }
}