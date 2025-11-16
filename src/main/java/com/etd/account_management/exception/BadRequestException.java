package com.etd.account_management.exception;

import lombok.Getter;

@Getter
public class BadRequestException extends RuntimeException {

    private final String fieldName;

    public BadRequestException(String message, String fieldName) {
        super(message);
        this.fieldName = fieldName;
    }

}
