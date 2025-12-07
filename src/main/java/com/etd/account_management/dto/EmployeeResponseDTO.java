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
public class EmployeeResponseDTO {

    private Long id;

    private String firstName;

    private String lastName;

    private String phoneNumber;

    private String emailAddress;

    private String role;

    private String gradeName;

    private LocalDateTime gradeAssignedOn;

}
