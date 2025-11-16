package com.etd.account_management.service.interfaces;

import com.etd.account_management.dto.GradeHistoryResponseDTO;

import java.util.List;

public interface GradeHistoryService {

    List<GradeHistoryResponseDTO> getGradeHistoryByEmployeeId(Long employeeId);

    List<GradeHistoryResponseDTO> getAllGradeHistories();

}
