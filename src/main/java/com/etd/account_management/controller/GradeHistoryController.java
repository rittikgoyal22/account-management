package com.etd.account_management.controller;
import com.etd.account_management.dto.GradeHistoryResponseDTO;
import com.etd.account_management.service.interfaces.GradeHistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/gradeHistory")
public class GradeHistoryController {

    private static final Logger logger = LoggerFactory.getLogger(GradeHistoryController.class);
    private final GradeHistoryService gradeHistoryService;

    public GradeHistoryController(GradeHistoryService gradeHistoryService) {
        this.gradeHistoryService = gradeHistoryService;
    }

    @GetMapping()
    public ResponseEntity<List<GradeHistoryResponseDTO>> getAllGradeHistory()
    {
        logger.info("Inside Grade History Controller :: Fetching grade history");
        List<GradeHistoryResponseDTO> gradeHistory = gradeHistoryService.getAllGradeHistories();
        return new ResponseEntity<>(gradeHistory, HttpStatus.OK);
    }

    @GetMapping("/{employeeId}")
    public ResponseEntity<List<GradeHistoryResponseDTO>> getGradeHistoryByEmployeeId(@PathVariable("employeeId") Long employeeId)
    {
        logger.info("Inside Grade History Controller :: Fetching grade history for employee id: {}", employeeId);
        List<GradeHistoryResponseDTO> gradeHistoryByEmployee = gradeHistoryService.getGradeHistoryByEmployeeId(employeeId);
        return new ResponseEntity<>(gradeHistoryByEmployee, HttpStatus.OK);
    }

}
