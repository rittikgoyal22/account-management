package com.etd.account_management.mapper;

import com.etd.account_management.dto.GradeResponseDTO;
import com.etd.account_management.entity.Grade;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GradeMapper {

    Logger logger = org.slf4j.LoggerFactory.getLogger(GradeMapper.class);

    public List<GradeResponseDTO> mapListOfGradeToGradeDTO(List<Grade> grades)
    {
        logger.info("Inside GradeMapper :: Mapping List<Grade> to List<GradeResponseDTO>");
        return grades.stream().map(grade->
            GradeResponseDTO.builder().id(grade.getId()).name(grade.getName()).build()
        ).toList();
    }

}
