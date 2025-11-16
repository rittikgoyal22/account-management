package com.etd.account_management.exception;

import lombok.Getter;

@Getter
public class GradeUpdateRuleViolationException extends RuntimeException {
    private final String fieldName;

    public GradeUpdateRuleViolationException(String message, String fieldName) {
        super(message);
        this.fieldName = fieldName;
    }

}
