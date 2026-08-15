package com.trainee.employeemanagement.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

/**
 * Response DTO mapped from the external mock REST API
 * (https://jsonplaceholder.typicode.com/users/{id}) used to demonstrate
 * outbound REST integration in EmployeeExternalIntegrationService.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExternalProfileDTO {
    private Long id;
    private String name;
    private String username;
    private String email;
    private String phone;
    private String website;
}
