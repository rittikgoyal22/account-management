package com.etd.account_management.mapper;

import com.etd.account_management.dto.GradeResponseDTO;
import com.etd.account_management.entity.Grade;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GradeMapper {

    public List<GradeResponseDTO> mapListOfGradeToGradeDTO(List<Grade> grades)
    {
        return grades.stream().map(grade->
            GradeResponseDTO.builder().id(grade.getId()).name(grade.getName()).build()
        ).toList();
    }

}
