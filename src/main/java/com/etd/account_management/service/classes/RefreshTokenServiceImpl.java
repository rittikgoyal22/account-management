package com.etd.account_management.service.classes;

import com.etd.account_management.dao.EmployeeRepo;
import com.etd.account_management.dao.RefreshTokenRepo;
import com.etd.account_management.entity.Employee;
import com.etd.account_management.entity.RefreshToken;
import com.etd.account_management.exception.BadRequestException;
import com.etd.account_management.exception.NotFoundException;
import com.etd.account_management.service.interfaces.RefreshTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

import static com.etd.account_management.constant.AppConstant.EMAIL_ADDRESS;
import static com.etd.account_management.constant.AppConstant.EMPLOYEE_ID;
import static com.etd.account_management.constant.AppConstant.ERROR_EMPLOYEE_NOT_FOUND;
import static com.etd.account_management.constant.AppConstant.ERROR_REFRESH_TOKEN_EXPIRED;
import static com.etd.account_management.constant.AppConstant.ERROR_REFRESH_TOKEN_INVALID;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenServiceImpl.class);
    private static final long REFRESH_TOKEN_VALIDITY_DAYS = 7;

    private final RefreshTokenRepo refreshTokenRepo;
    private final EmployeeRepo employeeRepo;
    private final MessageSource messageSource;

    public RefreshTokenServiceImpl(RefreshTokenRepo refreshTokenRepo,
                                   EmployeeRepo employeeRepo,
                                   MessageSource messageSource) {
        this.refreshTokenRepo = refreshTokenRepo;
        this.employeeRepo = employeeRepo;
        this.messageSource = messageSource;
    }

    @Override
    @Transactional
    public RefreshToken createRefreshToken(String emailAddress) {
        logger.info("Inside RefreshTokenServiceImpl :: createRefreshToken for: {}", emailAddress);

        Employee employee = employeeRepo.findByEmailAddress(emailAddress)
                .orElseThrow(() -> new NotFoundException(
                        messageSource.getMessage(ERROR_EMPLOYEE_NOT_FOUND, null, Locale.ENGLISH), EMPLOYEE_ID));

        // delete any existing refresh token via direct SQL (executes immediately,
        // before the INSERT below — avoids unique constraint violation on employee_id)
        refreshTokenRepo.deleteByEmployee(employee);

        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .employee(employee)
                .expiryDate(LocalDateTime.now().plusDays(REFRESH_TOKEN_VALIDITY_DAYS))
                .build();

        return refreshTokenRepo.save(refreshToken);
    }

    @Override
    @Transactional
    public RefreshToken verifyExpiration(RefreshToken refreshToken) {
        logger.info("Inside RefreshTokenServiceImpl :: verifyExpiration");

        if (refreshToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            logger.warn("Refresh token has expired for employee: {}",
                    refreshToken.getEmployee().getEmailAddress());
            refreshTokenRepo.delete(refreshToken);
            throw new BadRequestException(
                    messageSource.getMessage(ERROR_REFRESH_TOKEN_EXPIRED, null, Locale.ENGLISH),
                    EMAIL_ADDRESS);
        }
        return refreshToken;
    }

    @Override
    @Transactional
    public void deleteByEmailAddress(String emailAddress) {
        logger.info("Inside RefreshTokenServiceImpl :: deleteByEmailAddress for: {}", emailAddress);

        employeeRepo.findByEmailAddress(emailAddress)
                .ifPresent(refreshTokenRepo::deleteByEmployee);
    }

    @Override
    public RefreshToken findByToken(String token) {
        logger.info("Inside RefreshTokenServiceImpl :: findByToken");

        return refreshTokenRepo.findByToken(token)
                .orElseThrow(() -> {
                    logger.warn("Refresh token not found");
                    return new BadRequestException(
                            messageSource.getMessage(ERROR_REFRESH_TOKEN_INVALID, null, Locale.ENGLISH),
                            EMAIL_ADDRESS);
                });
    }

}
