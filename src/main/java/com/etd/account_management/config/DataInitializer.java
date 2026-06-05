package com.etd.account_management.config;

import com.etd.account_management.dao.EmployeeRepo;
import com.etd.account_management.dao.GradeRepo;
import com.etd.account_management.entity.Employee;
import com.etd.account_management.entity.Grade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final GradeRepo gradeRepo;
    private final EmployeeRepo employeeRepo;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(GradeRepo gradeRepo, EmployeeRepo employeeRepo, PasswordEncoder passwordEncoder) {
        this.gradeRepo = gradeRepo;
        this.employeeRepo = employeeRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedGrades();
        seedDefaultEmployees();
    }

    private void seedGrades() {
        if (gradeRepo.count() == 0) {
            logger.info("DataInitializer :: Seeding grades");
            gradeRepo.saveAll(List.of(
                    Grade.builder().name("Grade-1").build(),
                    Grade.builder().name("Grade-2").build(),
                    Grade.builder().name("Grade-3").build()
            ));
        }
    }

    private void seedDefaultEmployees() {
        Grade gradeOne   = gradeRepo.findById(1L).orElseThrow();
        Grade gradeThree = gradeRepo.findById(3L).orElseThrow();

        if (employeeRepo.findByEmailAddress("admin.hr@cognizant.com").isEmpty()) {
            logger.info("DataInitializer :: Creating default HR employee");
            employeeRepo.save(Employee.builder()
                    .firstName("Admin")
                    .lastName("HR")
                    .phoneNumber("9000000001")
                    .emailAddress("admin.hr@cognizant.com")
                    .password(passwordEncoder.encode("Admin@123"))
                    .role("HR")
                    .currentGrade(gradeOne)
                    .accessGranted(true)
                    .build());
        }

        if (employeeRepo.findByEmailAddress("desk.exec@cognizant.com").isEmpty()) {
            logger.info("DataInitializer :: Creating default TravelDeskExec employee");
            employeeRepo.save(Employee.builder()
                    .firstName("Desk")
                    .lastName("Exec")
                    .phoneNumber("9000000002")
                    .emailAddress("desk.exec@cognizant.com")
                    .password(passwordEncoder.encode("Exec@123"))
                    .role("TravelDeskExe")
                    .currentGrade(gradeOne)
                    .accessGranted(true)
                    .build());
        }

        if (employeeRepo.findByEmailAddress("john.employee@cognizant.com").isEmpty()) {
            logger.info("DataInitializer :: Creating default Employee");
            employeeRepo.save(Employee.builder()
                    .firstName("John")
                    .lastName("Employee")
                    .phoneNumber("9000000003")
                    .emailAddress("john.employee@cognizant.com")
                    .password(passwordEncoder.encode("Employee@123"))
                    .role("Employee")
                    .currentGrade(gradeThree)
                    .accessGranted(true)
                    .build());
        }
    }

}
