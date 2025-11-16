package com.etd.account_management.exception;

import com.etd.account_management.dto.ErrorDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(value = GradeUpdateRuleViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorDTO> handleGradeUpdateRuleViolationException(GradeUpdateRuleViolationException ex) {
        ErrorDTO errorDTO = ErrorDTO.builder()
                .message(ex.getMessage())
                .fieldName(ex.getFieldName())
                .status(HttpStatus.BAD_REQUEST)
                .build();
        return new ResponseEntity<>(errorDTO, errorDTO.getStatus());
    }

    @ExceptionHandler(value = NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ErrorDTO> handleNotFoundException(NotFoundException ex) {
        ErrorDTO errorDTO = ErrorDTO.builder()
                .message(ex.getMessage())
                .fieldName(ex.getFieldName())
                .status(HttpStatus.NOT_FOUND)
                .build();
        return new ResponseEntity<>(errorDTO, errorDTO.getStatus());
    }

    @ExceptionHandler(value = BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorDTO> handleBadRequestException(BadRequestException ex) {
        ErrorDTO errorDTO = ErrorDTO.builder()
                .message(ex.getMessage())
                .fieldName(ex.getFieldName())
                .status(HttpStatus.BAD_REQUEST)
                .build();
        return new ResponseEntity<>(errorDTO, errorDTO.getStatus());
    }

}
