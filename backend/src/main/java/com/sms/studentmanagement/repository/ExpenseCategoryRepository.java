package com.sms.studentmanagement.repository;

import com.sms.studentmanagement.entity.ExpenseCategory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    List<ExpenseCategory> findByActiveTrueOrderByNameAsc();

    List<ExpenseCategory> findAllByOrderByNameAsc();
}
