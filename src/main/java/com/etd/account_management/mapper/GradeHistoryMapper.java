package com.etd.account_management.mapper;

import com.etd.account_management.dto.GradeHistoryResponseDTO;
import com.etd.account_management.entity.Employee;
import com.etd.account_management.entity.Grade;
import com.etd.account_management.entity.GradeHistory;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class GradeHistoryMapper {

    Logger logger = org.slf4j.LoggerFactory.getLogger(GradeHistoryMapper.class);

    public GradeHistory createGradeHistoryByEmployeeAndGrade(Employee employee, Grade grade) {
        logger.info("Inside GradeHistoryMapper :: Creating GradeHistory for employee id: {} and grade id: {}", employee.getEmployeeId(), grade.getId());
        return GradeHistory.builder()
                .assignedOn(LocalDateTime.now())
                .employee(employee)
                .grade(grade)
                .build();
    }

    public List<GradeHistoryResponseDTO> mapListOfGradeHistoryToGradeHistoryResponseDTO(List<GradeHistory> gradeHistories) {
        logger.info("Inside GradeHistoryMapper :: Mapping List<GradeHistory> to List<GradeHistoryResponseDTO>");
        return gradeHistories.stream()
                .map(gradeHistory -> GradeHistoryResponseDTO.builder()
                            .id(gradeHistory.getId())
                            .assignedOn(gradeHistory.getAssignedOn())
                            .employeeId(gradeHistory.getEmployee().getEmployeeId())
                            .grade(gradeHistory.getGrade().getId())
                            .build()
                ).toList();
    }

}
