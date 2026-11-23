package com.sms.studentmanagement.repository;

import com.sms.studentmanagement.entity.FeeHead;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeeHeadRepository extends JpaRepository<FeeHead, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    List<FeeHead> findByActiveTrueOrderByNameAsc();

    List<FeeHead> findAllByOrderByNameAsc();
}
