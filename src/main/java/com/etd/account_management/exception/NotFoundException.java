package com.etd.account_management.exception;

import lombok.Getter;

@Getter
public class NotFoundException extends RuntimeException {

    private final String fieldName;

    public NotFoundException(String message, String fieldName) {
        super(message);
        this.fieldName = fieldName;
    }

}
