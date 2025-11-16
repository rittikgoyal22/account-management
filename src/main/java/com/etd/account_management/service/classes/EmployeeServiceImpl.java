package com.etd.account_management.service.classes;

import com.etd.account_management.dao.GradeHistoryRepo;
import com.etd.account_management.dao.GradeRepo;
import com.etd.account_management.dao.EmployeeRepo;
import com.etd.account_management.dto.EmployeeRequestDTO;
import com.etd.account_management.dto.EmployeeResponseDTO;
import com.etd.account_management.entity.Employee;
import com.etd.account_management.entity.Grade;
import com.etd.account_management.entity.GradeHistory;
import com.etd.account_management.exception.BadRequestException;
import com.etd.account_management.exception.GradeUpdateRuleViolationException;
import com.etd.account_management.exception.NotFoundException;
import com.etd.account_management.mapper.EmployeeMapper;
import com.etd.account_management.mapper.GradeHistoryMapper;
import com.etd.account_management.service.interfaces.EmployeeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepo employeeRepo;
    private final EmployeeMapper employeeMapper;
    private final GradeHistoryMapper gradeHistoryMapper;
    private final GradeRepo gradeRepo;
    private final GradeHistoryRepo gradeHistoryRepo;
    private final MessageSource messageSource;
    private static final Logger logger = LoggerFactory.getLogger(EmployeeServiceImpl.class);

    public EmployeeServiceImpl(EmployeeRepo employeeRepo, EmployeeMapper employeeMapper, GradeHistoryMapper gradeHistoryMapper, GradeRepo gradeRepo, GradeHistoryRepo gradeHistoryRepo, MessageSource messageSource) {
        this.employeeRepo = employeeRepo;
        this.employeeMapper = employeeMapper;
        this.gradeHistoryMapper = gradeHistoryMapper;
        this.gradeRepo = gradeRepo;
        this.gradeHistoryRepo = gradeHistoryRepo;
        this.messageSource = messageSource;
    }

    @Override
    public List<EmployeeResponseDTO> getAllEmployees() {
        logger.info("Inside EmployeeServiceImpl :: Fetching all employees");
        List<Employee> employees = employeeRepo.findAll();
        return employeeMapper.mapListOfEmployeeToEmployeeDTO(employees);
    }

    @Override
    public EmployeeResponseDTO getEmployeeById(Long id) {
        logger.info("Inside EmployeeServiceImpl :: Fetching employee with id: {}", id);
        Employee employee = employeeRepo.findById(id).orElseThrow(() -> new NotFoundException(messageSource.getMessage("error.employee.not.found",null, Locale.ENGLISH), "Employee ID"));
        return employeeMapper.mapEmployeeToEmployeeResponseDTO(employee);
    }

    @Override
    @Transactional
    public void deleteEmployeeById(Long id) {
        logger.info("Inside EmployeeServiceImpl :: Deleting employee with id: {}", id);
        if (!employeeRepo.existsById(id)) {
            logger.warn("Employee with id: {} not found", id);
            throw new NotFoundException(messageSource.getMessage("error.employee.not.found",null, Locale.ENGLISH), "Employee ID");
        }
        gradeHistoryRepo.deleteByEmployeeEmployeeId(id);
        employeeRepo.deleteById(id);
    }

    @Override
    @Transactional
    public EmployeeResponseDTO createEmployee(EmployeeRequestDTO employeeRequestDTO) {
        logger.info("Inside EmployeeServiceImpl :: Creating new employee");
        if(employeeRequestDTO.getEmailAddress() == null || !employeeRequestDTO.getEmailAddress().endsWith("@cognizant.com")) {
            logger.warn("Invalid email address: {}", employeeRequestDTO.getEmailAddress());
            throw new BadRequestException(messageSource.getMessage("error.employee.invalid.email",null, Locale.ENGLISH), "Email Address");
        }
        if(employeeRequestDTO.getRole() != null && employeeRequestDTO.getRole().equals("TravelDeskExec")) {
            logger.warn("Assigning default grade id 1 for TravelDeskExec role");
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
        logger.info("Inside EmployeeServiceImpl :: Updating employee with id: {}", id);
        Employee existingEmployee = employeeRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        if(employeeRequestDTO.getEmailAddress() == null || !employeeRequestDTO.getEmailAddress().endsWith("@cognizant.com")) {
            logger.warn("Invalid email address: {}", employeeRequestDTO.getEmailAddress());
            throw new BadRequestException(messageSource.getMessage("error.employee.invalid.email",null, Locale.ENGLISH), "Email Address");
        }
        Long employeeCurrentGradeId = existingEmployee.getCurrentGrade().getId();

        if(employeeCurrentGradeId!=null && employeeRequestDTO.getCurrentGradeId()!=null &&
                employeeCurrentGradeId < employeeRequestDTO.getCurrentGradeId()) {
            logger.warn("Attempt to downgrade grade from {} to {}", employeeCurrentGradeId, employeeRequestDTO.getCurrentGradeId());
            throw new GradeUpdateRuleViolationException(messageSource.getMessage("error.grade.change.upwards.only", null, Locale.ENGLISH), null);
        }
        Grade newGrade = null;
        if(!Objects.equals(employeeCurrentGradeId, employeeRequestDTO.getCurrentGradeId()))
        {
            List<GradeHistory> gradeHistories = existingEmployee.getGradeHistories().stream().sorted((a,b)->b.getAssignedOn().compareTo(a.getAssignedOn())).toList();
            LocalDateTime joiningDate = gradeHistories.getLast().getAssignedOn();
            LocalDateTime lastGradeChangeDate = gradeHistories.getFirst().getAssignedOn();
            LocalDateTime today = LocalDateTime.now();
            if(joiningDate.plusYears(2).isAfter(today))
            {
                logger.warn("New joiner grade change attempt within 2 years of joining date: {}", joiningDate);
                throw new GradeUpdateRuleViolationException(messageSource.getMessage("error.grade.change.new.joiner", null, Locale.ENGLISH), null);
            }
            if(lastGradeChangeDate.plusYears(1).isAfter(today))
            {
                logger.warn("Grade change attempt within 1 year of last grade change date: {}", lastGradeChangeDate);
                throw new GradeUpdateRuleViolationException(messageSource.getMessage("error.grade.change.once.year", null, Locale.ENGLISH), null);
            }
            newGrade = gradeRepo.findById(employeeRequestDTO.getCurrentGradeId())
                    .orElseThrow(() -> new RuntimeException("Grade not found"));
        }

        employeeMapper.mapNewDataToExistingEmployee(employeeRequestDTO, existingEmployee,newGrade);

        Employee updatedEmployee = employeeRepo.save(existingEmployee);

        if (!Objects.equals(employeeCurrentGradeId, employeeRequestDTO.getCurrentGradeId())) {
            GradeHistory gradeHistory = gradeHistoryMapper.createGradeHistoryByEmployeeAndGrade(existingEmployee, newGrade);
            gradeHistoryRepo.save(gradeHistory);
        }
        return employeeMapper.mapEmployeeToEmployeeResponseDTO(updatedEmployee);
    }

}