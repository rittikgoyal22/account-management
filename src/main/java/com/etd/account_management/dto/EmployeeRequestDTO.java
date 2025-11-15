package com.etd.account_management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeRequestDTO {

    private String firstName;

    private String lastName;

    private String phoneNumber;

    private String emailAddress;

    private String role;

    private Long currentGradeId;

    private Boolean accessGranted;

}
