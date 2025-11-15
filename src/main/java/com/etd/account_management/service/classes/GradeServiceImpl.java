package com.etd.account_management.service.classes;

import com.etd.account_management.dao.GradeRepo;
import com.etd.account_management.dto.GradeResponseDTO;
import com.etd.account_management.entity.Grade;
import com.etd.account_management.mapper.GradeMapper;
import com.etd.account_management.service.interfaces.GradeService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GradeServiceImpl implements GradeService {

    private final GradeRepo gradeRepo;
    private final GradeMapper gradeMapper;

    public GradeServiceImpl(GradeRepo gradeRepo, GradeMapper gradeMapper) {
        this.gradeRepo = gradeRepo;
        this.gradeMapper = gradeMapper;
    }


    @Override
    public List<GradeResponseDTO> getAllGrade() {
        List<Grade> grades = gradeRepo.findAll();
        return gradeMapper.mapListOfGradeToGradeDTO(grades);
    }
}
