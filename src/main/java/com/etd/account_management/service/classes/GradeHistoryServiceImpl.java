package com.etd.account_management.service.classes;

import com.etd.account_management.dao.GradeHistoryRepo;
import com.etd.account_management.dto.GradeHistoryResponseDTO;
import com.etd.account_management.entity.GradeHistory;
import com.etd.account_management.exception.NotFoundException;
import com.etd.account_management.mapper.GradeHistoryMapper;
import com.etd.account_management.service.interfaces.GradeHistoryService;
import org.slf4j.Logger;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

import static com.etd.account_management.constant.AppConstant.EMPLOYEE_ID;
import static com.etd.account_management.constant.AppConstant.ERROR_EMPLOYEE_NOT_FOUND;

@Service
public class GradeHistoryServiceImpl implements GradeHistoryService {

    private final Logger logger = org.slf4j.LoggerFactory.getLogger(GradeHistoryServiceImpl.class);
    private final GradeHistoryRepo gradeHistoryRepo;
    private final GradeHistoryMapper gradeHistoryMapper;
    private final MessageSource messageSource;

    public GradeHistoryServiceImpl(GradeHistoryRepo gradeHistoryRepo, GradeHistoryMapper gradeHistoryMapper, MessageSource messageSource) {
        this.gradeHistoryRepo = gradeHistoryRepo;
        this.gradeHistoryMapper = gradeHistoryMapper;
        this.messageSource = messageSource;
    }

    @Override
    public List<GradeHistoryResponseDTO> getGradeHistoryByEmployeeId(Long employeeId) {
        logger.info("Inside GradeHistoryServiceImpl :: Fetching grade history for employee id: {}", employeeId);
        List<GradeHistory> gradeHistories = gradeHistoryRepo.findByEmployeeEmployeeId(employeeId);
        if(gradeHistories.isEmpty()) {
            logger.warn("No grade history found for employee id: {}", employeeId);
            throw new NotFoundException(messageSource.getMessage(ERROR_EMPLOYEE_NOT_FOUND,null, Locale.ENGLISH), EMPLOYEE_ID);
        }
        return gradeHistoryMapper.mapListOfGradeHistoryToGradeHistoryResponseDTO(gradeHistories);
    }

    @Override
    public List<GradeHistoryResponseDTO> getAllGradeHistories() {
        logger.info("Inside GradeHistoryServiceImpl :: Fetching all grade histories");
        List<GradeHistory> gradeHistories = gradeHistoryRepo.findAll();
        return gradeHistoryMapper.mapListOfGradeHistoryToGradeHistoryResponseDTO(gradeHistories);
    }

}
