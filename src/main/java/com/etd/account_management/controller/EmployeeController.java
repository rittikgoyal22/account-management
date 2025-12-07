package com.etd.account_management.controller;

import com.etd.account_management.dto.EmployeeRequestDTO;
import com.etd.account_management.dto.EmployeeResponseDTO;
import com.etd.account_management.service.interfaces.EmployeeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
@CrossOrigin
public class EmployeeController {

    private final EmployeeService employeeService;
    private final Logger logger = LoggerFactory.getLogger(EmployeeController.class);

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping()
    public ResponseEntity<List<EmployeeResponseDTO>> getAllEmployees() {
        logger.info("Inside Employee Controller :: Fetching all employees");
        List<EmployeeResponseDTO> employees = employeeService.getAllEmployees();
        return ResponseEntity.ok(employees);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeResponseDTO> getEmployeeById(@PathVariable("id") Long id) {
        logger.info("Inside Employee Controller :: Fetching employee with id: {}", id);
        EmployeeResponseDTO employees = employeeService.getEmployeeById(id);
        return ResponseEntity.ok(employees);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmployeeById(@PathVariable("id") Long id) {
        logger.info("Inside Employee Controller :: Deleting employee with id: {}", id);
        employeeService.deleteEmployeeById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping()
    public ResponseEntity<EmployeeResponseDTO> createEmployee(@RequestBody EmployeeRequestDTO employeeRequestDTO) {
        logger.info("Inside Employee Controller :: Creating new employee : {}", employeeRequestDTO);
        EmployeeResponseDTO employeeResponseDTO = employeeService.createEmployee(employeeRequestDTO);
        return ResponseEntity.ok(employeeResponseDTO);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmployeeResponseDTO> updateEmployee(@PathVariable("id") Long id,@RequestBody EmployeeRequestDTO employeeRequestDTO) {
        logger.info("Inside Employee Controller :: Updating employee with id: {}", id);
        EmployeeResponseDTO employeeResponseDTO = employeeService.updateEmployee(id, employeeRequestDTO);
        return ResponseEntity.ok(employeeResponseDTO);
    }

}
