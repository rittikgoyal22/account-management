package com.etd.account_management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GradeHistoryResponseDTO {

    private Long id;

    private LocalDateTime assignedOn;

    private Long employeeId;

    private Long grade;

}