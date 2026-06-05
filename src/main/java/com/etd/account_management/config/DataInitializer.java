package com.etd.account_management.config;

import com.etd.account_management.dao.EmployeeRepo;
import com.etd.account_management.dao.GradeRepo;
import com.etd.account_management.dao.TokenBlacklistRepo;
import com.etd.account_management.entity.Employee;
import com.etd.account_management.entity.Grade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final GradeRepo gradeRepo;
    private final EmployeeRepo employeeRepo;
    private final PasswordEncoder passwordEncoder;
    private final TokenBlacklistRepo tokenBlacklistRepo;

    public DataInitializer(GradeRepo gradeRepo, EmployeeRepo employeeRepo,
                           PasswordEncoder passwordEncoder, TokenBlacklistRepo tokenBlacklistRepo) {
        this.gradeRepo = gradeRepo;
        this.employeeRepo = employeeRepo;
        this.passwordEncoder = passwordEncoder;
        this.tokenBlacklistRepo = tokenBlacklistRepo;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedGrades();
        seedDefaultEmployees();
        cleanupExpiredBlacklistTokens();
    }

    private void cleanupExpiredBlacklistTokens() {
        logger.info("DataInitializer :: Cleaning up expired blacklisted tokens");
        tokenBlacklistRepo.deleteExpiredTokens(LocalDateTime.now());
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
        Grade gradeOne = gradeRepo.findById(1L).orElse(gradeRepo.findAll().get(0));

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
    }

}
