package com.etd.account_management.controller;

import com.etd.account_management.dto.GradeResponseDTO;
import com.etd.account_management.service.interfaces.GradeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/grades")
public class GradeController {

    private final GradeService gradeService;
    private static final Logger logger = LoggerFactory.getLogger(GradeController.class);

    public GradeController(GradeService gradeService) {
        this.gradeService = gradeService;
    }

    @GetMapping()
    public ResponseEntity<List<GradeResponseDTO>> getAllGrades()
    {
        logger.info("Inside Grade Controller :: Fetching all grades");
        List<GradeResponseDTO> grades = gradeService.getAllGrade();
        return new ResponseEntity<>(grades, HttpStatus.OK);
    }

}
