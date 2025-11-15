package com.etd.account_management.controller;

import com.etd.account_management.dto.GradeResponseDTO;
import com.etd.account_management.service.interfaces.GradeService;
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

    public GradeController(GradeService gradeService) {
        this.gradeService = gradeService;
    }

    @GetMapping()
    public ResponseEntity<List<GradeResponseDTO>> getAllGrades()
    {
        List<GradeResponseDTO> grades = gradeService.getAllGrade();
        return new ResponseEntity<>(grades, HttpStatus.OK);
    }

}
