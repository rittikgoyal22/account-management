package com.etd.account_management.service.interfaces;

import com.etd.account_management.dto.EmployeeRequestDTO;
import com.etd.account_management.dto.EmployeeResponseDTO;

import java.util.List;

public interface EmployeeService {

    List<EmployeeResponseDTO> getAllEmployees();

    EmployeeResponseDTO getEmployeeById(Long id);

    void deleteEmployeeById(Long id);

    EmployeeResponseDTO createEmployee(EmployeeRequestDTO employeeRequestDTO);

    EmployeeResponseDTO updateEmployee(Long id, EmployeeRequestDTO employeeRequestDTO);
}
