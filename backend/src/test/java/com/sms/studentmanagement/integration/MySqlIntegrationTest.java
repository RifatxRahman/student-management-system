package com.sms.studentmanagement.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sms.studentmanagement.service.StudentCodeGenerator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

/**
 * আসল MySQL (Docker-এ) চালু করে পুরো অ্যাপ লোড করে।
 * এই টেস্টে DB_USER / DB_PASSWORD লাগে না, ডেটাবেসের ঠিকানা টেস্ট নিজে বসায়।
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = "JWT_SECRET=TestSecretTestSecretTestSecretTestSecret12345678")
class MySqlIntegrationTest {

    @Container
    static MySQLContainer mysql = new MySQLContainer("mysql:8.4");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    StudentCodeGenerator codeGenerator;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Test
    void allMigrationsRanOnRealMySql() {
        Integer failed = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 0", Integer.class);
        assertEquals(0, failed);

        Integer tables = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() "
                        + "AND table_name IN ('students', 'guardians', 'student_guardians', 'enrollments', "
                        + "'audit_logs', 'holidays', 'attendance', 'notification_logs')",
                Integer.class);
        assertEquals(8, tables);
    }

    @Test
    void twentyParallelAdmissionsGetTwentyUniqueConsecutiveCodes() throws Exception {
        int threads = 20;
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);

        List<Future<String>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                go.await(); // সবাই তৈরি হলে একসাথে ছুটবে
                return transaction.execute(status -> codeGenerator.next(2099));
            }));
        }
        ready.await();
        go.countDown();

        Set<String> codes = new HashSet<>();
        for (Future<String> future : futures) {
            codes.add(future.get(30, TimeUnit.SECONDS));
        }
        pool.shutdown();

        assertEquals(threads, codes.size(), "একই ID দুজনকে দেওয়া হয়েছে!");
        for (int i = 1; i <= threads; i++) {
            assertTrue(codes.contains(String.format("2099-%05d", i)), "২০৯৯-" + i + " নম্বর ID নেই");
        }
    }

    @Test
    void gradeScaleIsSeededAndExamTablesExist() {
        Integer bands = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM grade_scales", Integer.class);
        assertEquals(7, bands);

        Integer tables = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() "
                        + "AND table_name IN ('grade_scales', 'exams', 'exam_subjects')",
                Integer.class);
        assertEquals(3, tables);
    }

    @Test
    void marksAndResultTablesExist() {
        Integer tables = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() "
                        + "AND table_name IN ('marks', 'results', 'result_subjects')",
                Integer.class);
        assertEquals(3, tables);
    }


    @Test
    void routineTablesExistAndTimetableRejectsDoubleBooking() {
        Integer tables = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() "
                        + "AND table_name IN ('rooms', 'time_slots', 'timetable')",
                Integer.class);
        assertEquals(3, tables);

        // সংঘাত-তালা সত্যিই আছে কি না: তিনটা UNIQUE
        Integer locks = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() "
                        + "AND table_name = 'timetable' AND constraint_type = 'UNIQUE'",
                Integer.class);
        assertEquals(3, locks);
    }
}