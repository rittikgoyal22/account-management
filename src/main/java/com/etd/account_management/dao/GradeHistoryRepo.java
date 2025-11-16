package com.etd.account_management.dao;

import com.etd.account_management.entity.GradeHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GradeHistoryRepo extends JpaRepository<GradeHistory, Long> {

    void deleteByEmployeeEmployeeId(Long employeeId);

    List<GradeHistory> findByEmployeeEmployeeId(Long employeeId);

}
