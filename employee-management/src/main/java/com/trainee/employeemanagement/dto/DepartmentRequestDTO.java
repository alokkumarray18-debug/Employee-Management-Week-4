package com.trainee.employeemanagement.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DepartmentRequestDTO {

    @NotBlank(message = "Department name is required")
    private String departmentName;

    @NotBlank(message = "Location is required")
    private String location;
}
