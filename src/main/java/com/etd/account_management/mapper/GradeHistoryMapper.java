package com.etd.account_management.mapper;

import com.etd.account_management.entity.Employee;
import com.etd.account_management.entity.Grade;
import com.etd.account_management.entity.GradeHistory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class GradeHistoryMapper {

    public GradeHistory createGradeHistoryByEmployeeAndGrade(Employee employee, Grade grade) {
        return GradeHistory.builder()
                .assignedOn(LocalDateTime.now())
                .employee(employee)
                .grade(grade)
                .build();
    }

}
