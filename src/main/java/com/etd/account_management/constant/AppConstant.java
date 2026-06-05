package com.etd.account_management.constant;

public class AppConstant {

    private AppConstant() {
        // Private constructor to prevent instantiation
    }

    //Message Properties Keys
    public static final String ERROR_GRADE_CHANGE_NEW_JOINER = "error.grade.change.new.joiner";
    public static final String ERROR_GRADE_CHANGE_ONCE_YEAR = "error.grade.change.once.year";
    public static final String ERROR_GRADE_CHANGE_UPWARDS_ONLY = "error.grade.change.upwards.only";
    public static final String ERROR_EMPLOYEE_NOT_FOUND = "error.employee.not.found";
    public static final String ERROR_EMPLOYEE_INVALID_EMAIL = "error.employee.invalid.email";
    public static final String ERROR_EMPLOYEE_INVALID_ID = "error.employee.invalid.id";
    public static final String ERROR_GRADE_NOT_FOUND = "error.grade.not.found";

    //General Constants
    public static final String EMAIL_DOMAIN = "@cognizant.com";
    public static final String EMPLOYEE_ID = "Employee ID";
    public static final String EMAIL_ADDRESS = "Email Address";
    public static final String CURRENT_GRADE_ID = "Current Grade ID";
    public static final String ROLE_TRAVEL_DESK_EXE = "TravelDeskExe";
    public static final String ROLE_HR = "HR";
    public static final String ROLE_EMPLOYEE = "Employee";
    public static final String API_BASE_PATH = "/api/**";
}
