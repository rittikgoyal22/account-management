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

import java.util.List;

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
//        logger.info("Deleted {} grade history records for employee id {}", recordsDeletedByGradeHistory, id);
        employeeRepo.deleteById(id);
    }

    @Override
    @Transactional
    public EmployeeResponseDTO createEmployee(EmployeeRequestDTO employeeRequestDTO) {
//                c.	Grade of an employee should be allowed to go upwards not downwards, for example a Grade-1 employee cannot be downgraded to Grade-2.
//        d.	An employee’s grade can only be changed once in an year. The grade of new joinees can only be changed after they complete 2 years. If a user tries to break the rule for grading generate a user-defined exception as “GradeUpdateRuleViolationException”.

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
}