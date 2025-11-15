package com.etd.account_management.service.classes;

import com.etd.account_management.dao.GradeHistoryRepo;
import com.etd.account_management.dao.GradeRepo;
import com.etd.account_management.dao.EmployeeRepo;
import com.etd.account_management.dto.EmployeeRequestDTO;
import com.etd.account_management.dto.EmployeeResponseDTO;
import com.etd.account_management.entity.Employee;
import com.etd.account_management.entity.Grade;
import com.etd.account_management.entity.GradeHistory;
import com.etd.account_management.mapper.EmployeeMapper;
import com.etd.account_management.mapper.GradeHistoryMapper;
import com.etd.account_management.service.interfaces.EmployeeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepo employeeRepo;
    private final EmployeeMapper employeeMapper;
    private final GradeHistoryMapper gradeHistoryMapper;
    private final GradeRepo gradeRepo;
    private final GradeHistoryRepo gradeHistoryRepo;
    private static final Logger logger = LoggerFactory.getLogger(EmployeeServiceImpl.class);


    public EmployeeServiceImpl(EmployeeRepo employeeRepo, EmployeeMapper employeeMapper, GradeHistoryMapper gradeHistoryMapper, GradeRepo gradeRepo, GradeHistoryRepo gradeHistoryRepo) {
        this.employeeRepo = employeeRepo;
        this.employeeMapper = employeeMapper;
        this.gradeHistoryMapper = gradeHistoryMapper;
        this.gradeRepo = gradeRepo;
        this.gradeHistoryRepo = gradeHistoryRepo;
    }


    @Override
    public List<EmployeeResponseDTO> getAllEmployees() {
        List<Employee> employees = employeeRepo.findAll();
        return employeeMapper.mapListOfEmployeeToEmployeeDTO(employees);
    }

    @Override
    public EmployeeResponseDTO getEmployeeById(Long id) {
        Employee employee = employeeRepo.findById(id).orElseThrow(() -> new RuntimeException("Employee not found"));
        return employeeMapper.mapEmployeeToEmployeeResponseDTO(employee);
    }

    @Override
    @Transactional
    public void deleteEmployeeById(Long id) {
        if (!employeeRepo.existsById(id)) {
            throw new RuntimeException("Employee not found");
        }
        gradeHistoryRepo.deleteByEmployeeEmployeeId(id);
        employeeRepo.deleteById(id);
    }

    @Override
    @Transactional
    public EmployeeResponseDTO createEmployee(EmployeeRequestDTO employeeRequestDTO) {

        if(employeeRequestDTO.getEmailAddress() == null || !employeeRequestDTO.getEmailAddress().endsWith("@cognizant.com")) {
            throw new RuntimeException("Invalid email address");
        }

        if(employeeRequestDTO.getRole() != null && employeeRequestDTO.getRole().equals("TravelDeskExec")) {
            employeeRequestDTO.setCurrentGradeId(1L);
        }

        Grade grade = gradeRepo.findById(employeeRequestDTO.getCurrentGradeId())
                .orElseThrow(() -> new RuntimeException("Grade not found"));
        Employee employee = employeeMapper.mapEmployeeRequestDTOToEmployee(employeeRequestDTO, grade);
        Employee savedEmployee = employeeRepo.save(employee);
        GradeHistory gradeHistory = gradeHistoryMapper.createGradeHistoryByEmployeeAndGrade(employee, grade);
        gradeHistoryRepo.save(gradeHistory);
        return employeeMapper.mapEmployeeToEmployeeResponseDTO(savedEmployee);
    }

    @Override
    @Transactional
    public EmployeeResponseDTO updateEmployee(Long id, EmployeeRequestDTO employeeRequestDTO) {
        Employee existingEmployee = employeeRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        if(employeeRequestDTO.getEmailAddress() == null || !employeeRequestDTO.getEmailAddress().endsWith("@cognizant.com")) {
            throw new RuntimeException("Invalid email address");
        }

        if(existingEmployee.getCurrentGrade().getId()!=null && employeeRequestDTO.getCurrentGradeId()!=null &&
                existingEmployee.getCurrentGrade().getId() < employeeRequestDTO.getCurrentGradeId()) {
            throw new RuntimeException("GradeUpdateRuleViolationException: Grade can only be upgraded");
        }
        Grade newGrade = null;
        if(!Objects.equals(existingEmployee.getCurrentGrade().getId(), employeeRequestDTO.getCurrentGradeId()))
        {
            List<GradeHistory> gradeHistories = existingEmployee.getGradeHistories().stream().sorted((a,b)->b.getAssignedOn().compareTo(a.getAssignedOn())).toList();
            LocalDateTime joiningDate = gradeHistories.getLast().getAssignedOn();
            LocalDateTime lastGradeChangeDate = gradeHistories.getFirst().getAssignedOn();
            LocalDateTime today = LocalDateTime.now();
            if(joiningDate.plusYears(2).isAfter(today))
            {
                throw new RuntimeException("GradeUpdateRuleViolationException: Grade can be changed only after 2 years of joining");
            }
            if(lastGradeChangeDate.plusYears(1).isAfter(today) )
            {
                throw new RuntimeException("GradeUpdateRuleViolationException: Grade can be changed only after 1 years of past change");
            }
            newGrade = gradeRepo.findById(employeeRequestDTO.getCurrentGradeId())
                    .orElseThrow(() -> new RuntimeException("Grade not found"));
        }

        employeeMapper.mapNewDataToExistingEmployee(employeeRequestDTO, existingEmployee,newGrade);

        Employee updatedEmployee = employeeRepo.save(existingEmployee);

        if (!Objects.equals(existingEmployee.getCurrentGrade().getId(), employeeRequestDTO.getCurrentGradeId())) {
            GradeHistory gradeHistory = gradeHistoryMapper.createGradeHistoryByEmployeeAndGrade(existingEmployee, newGrade);
            gradeHistoryRepo.save(gradeHistory);
        }
        return employeeMapper.mapEmployeeToEmployeeResponseDTO(updatedEmployee);
    }
}