package com.etd.account_management.util;

import com.etd.account_management.dto.EmployeeRequestDTO;
import com.etd.account_management.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.Locale;

import static com.etd.account_management.constant.AppConstant.EMAIL_ADDRESS;
import static com.etd.account_management.constant.AppConstant.EMAIL_DOMAIN;
import static com.etd.account_management.constant.AppConstant.ERROR_EMPLOYEE_INVALID_EMAIL;

@Component
public record CommonUtil(MessageSource messageSource) {

    private static final Logger logger = LoggerFactory.getLogger(CommonUtil.class);

    public void validateEmailAddress(EmployeeRequestDTO employeeRequestDTO) {
        logger.info("Inside Common Util :: validateEmailAddress : {}", employeeRequestDTO.getEmailAddress());
        if (employeeRequestDTO.getEmailAddress() == null || !employeeRequestDTO.getEmailAddress().endsWith(EMAIL_DOMAIN)) {
            logger.warn("Invalid email address: {}", employeeRequestDTO.getEmailAddress());
            throw new BadRequestException(messageSource.getMessage(ERROR_EMPLOYEE_INVALID_EMAIL, null, Locale.ENGLISH), EMAIL_ADDRESS);
        }
    }

}
