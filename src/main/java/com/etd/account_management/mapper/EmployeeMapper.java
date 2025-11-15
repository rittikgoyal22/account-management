package com.etd.account_management.mapper;

import com.etd.account_management.dto.EmployeeRequestDTO;
import com.etd.account_management.dto.EmployeeResponseDTO;
import com.etd.account_management.entity.Employee;
import com.etd.account_management.entity.Grade;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class EmployeeMapper {

    public List<EmployeeResponseDTO> mapListOfEmployeeToEmployeeDTO(List<Employee> employees) {
        return employees.stream().map(this::mapEmployeeToEmployeeResponseDTO).toList();
    }

    public EmployeeResponseDTO mapEmployeeToEmployeeResponseDTO(Employee employee) {
        return EmployeeResponseDTO.builder()
                .id(employee.getEmployeeId())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .phoneNumber(employee.getPhoneNumber())
                .email(employee.getEmailAddress())
                .role(employee.getRole())
                .currentGradeId(!ObjectUtils.isEmpty(employee.getCurrentGrade())? employee.getCurrentGrade().getId() : null)
                .gradeAssignedOn(ObjectUtils.isEmpty(employee.getGradeHistories())? LocalDateTime.now():employee.getGradeHistories().getLast().getAssignedOn())
                .build();
    }

    public Employee mapEmployeeRequestDTOToEmployee(EmployeeRequestDTO employeeRequestDTO, Grade grade) {
        Employee employee = new Employee();
        employee.setFirstName(employeeRequestDTO.getFirstName());
        employee.setLastName(employeeRequestDTO.getLastName());
        employee.setPhoneNumber(employeeRequestDTO.getPhoneNumber());
        employee.setEmailAddress(employeeRequestDTO.getEmailAddress());
        employee.setRole(employeeRequestDTO.getRole());
        employee.setCurrentGrade(grade);
        if (ObjectUtils.isEmpty(employeeRequestDTO.getAccessGranted())) {
            employee.setAccessGranted(true);
        } else {
            employee.setAccessGranted(employeeRequestDTO.getAccessGranted());
        }
        return employee;
    }

}
