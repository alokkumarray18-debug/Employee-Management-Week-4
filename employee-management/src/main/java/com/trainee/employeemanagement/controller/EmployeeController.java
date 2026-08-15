package com.trainee.employeemanagement.controller;

import com.trainee.employeemanagement.dto.*;
import com.trainee.employeemanagement.service.EmployeeExternalIntegrationService;
import com.trainee.employeemanagement.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
@Tag(name = "Employees", description = "CRUD, search, pagination and advanced queries for employees")
public class EmployeeController {

    private static final Logger log = LoggerFactory.getLogger(EmployeeController.class);

    private final EmployeeService employeeService;
    private final EmployeeExternalIntegrationService externalIntegrationService;

    @Operation(summary = "Create a new employee")
    @PostMapping
    public ResponseEntity<ApiResponse<EmployeeResponseDTO>> createEmployee(
            @Valid @RequestBody EmployeeRequestDTO dto) {
        log.info("POST /api/v1/employees email={}", dto.getEmail());
        EmployeeResponseDTO created = employeeService.createEmployee(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Employee created successfully", created));
    }

    @Operation(summary = "Create a new employee and write an audit record in the same transaction",
            description = "Demonstrates section 9: if the audit write fails, the employee insert rolls back too.")
    @PostMapping("/with-audit")
    public ResponseEntity<ApiResponse<EmployeeResponseDTO>> createEmployeeWithAudit(
            @Valid @RequestBody EmployeeRequestDTO dto,
            @Parameter(description = "Name/id of whoever is performing the action")
            @RequestParam(defaultValue = "SYSTEM") String performedBy) {
        EmployeeResponseDTO created = employeeService.createEmployeeWithAudit(dto, performedBy);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Employee created and audited successfully", created));
    }

    @Operation(summary = "List employees (paginated, sortable)",
            description = "e.g. /api/v1/employees?page=0&size=10&sortBy=salary&direction=desc")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponseDTO<EmployeeResponseDTO>>> getAllEmployees(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        Pageable pageable = buildPageable(page, size, sortBy, direction);
        PageResponseDTO<EmployeeResponseDTO> result = employeeService.getAllEmployees(pageable);
        return ResponseEntity.ok(ApiResponse.success("Employees retrieved successfully", result));
    }

    @Operation(summary = "Search employees by (partial, case-insensitive) name")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponseDTO<EmployeeResponseDTO>>> searchByName(
            @RequestParam String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = buildPageable(page, size, "id", "asc");
        PageResponseDTO<EmployeeResponseDTO> result = employeeService.searchByName(name, pageable);
        return ResponseEntity.ok(ApiResponse.success("Search results retrieved successfully", result));
    }

    @Operation(summary = "List employees belonging to a given department (paginated)")
    @GetMapping("/department/{departmentId}")
    public ResponseEntity<ApiResponse<PageResponseDTO<EmployeeResponseDTO>>> getByDepartment(
            @PathVariable Long departmentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = buildPageable(page, size, "id", "asc");
        PageResponseDTO<EmployeeResponseDTO> result = employeeService.getByDepartment(departmentId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Employees retrieved successfully", result));
    }

    @Operation(summary = "Get a single employee by id")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeResponseDTO>> getEmployeeById(@PathVariable Long id) {
        EmployeeResponseDTO employee = employeeService.getEmployeeById(id);
        return ResponseEntity.ok(ApiResponse.success("Employee retrieved successfully", employee));
    }

    @Operation(summary = "Update an existing employee")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeResponseDTO>> updateEmployee(
            @PathVariable Long id, @Valid @RequestBody EmployeeRequestDTO dto) {
        EmployeeResponseDTO updated = employeeService.updateEmployee(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Employee updated successfully", updated));
    }

    @Operation(summary = "Delete an employee")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteEmployee(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.ok(ApiResponse.success("Employee deleted successfully", null));
    }

    // ---- Advanced queries (assignment section 5) ----

    @Operation(summary = "Employees earning above a given salary")
    @GetMapping("/salary-above/{amount}")
    public ResponseEntity<ApiResponse<List<EmployeeResponseDTO>>> getAboveSalary(@PathVariable Double amount) {
        return ResponseEntity.ok(ApiResponse.success("Employees retrieved successfully",
                employeeService.getEmployeesAboveSalary(amount)));
    }

    @Operation(summary = "The single highest-paid employee")
    @GetMapping("/highest-paid")
    public ResponseEntity<ApiResponse<EmployeeResponseDTO>> getHighestPaid() {
        return ResponseEntity.ok(ApiResponse.success("Highest paid employee retrieved successfully",
                employeeService.getHighestPaidEmployee()));
    }

    @Operation(summary = "Employees who joined after a given date (yyyy-MM-dd)")
    @GetMapping("/joined-after/{date}")
    public ResponseEntity<ApiResponse<List<EmployeeResponseDTO>>> getJoinedAfter(
            @PathVariable @org.springframework.format.annotation.DateTimeFormat(iso =
                    org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success("Employees retrieved successfully",
                employeeService.getEmployeesJoinedAfter(date)));
    }

    @Operation(summary = "Average salary grouped by department")
    @GetMapping("/average-salary-by-department")
    public ResponseEntity<ApiResponse<List<Object[]>>> averageSalaryByDepartment() {
        return ResponseEntity.ok(ApiResponse.success("Average salaries retrieved successfully",
                employeeService.getAverageSalaryByDepartment()));
    }

    // ---- External REST API integration (assignment section 10) ----

    @Operation(summary = "Fetch a mock external profile for an employee via RestClient",
            description = "Demonstrates outbound REST integration against https://jsonplaceholder.typicode.com")
    @GetMapping("/external-profile/{externalId}")
    public ResponseEntity<ApiResponse<ExternalProfileDTO>> getExternalProfile(@PathVariable Long externalId) {
        ExternalProfileDTO profile = externalIntegrationService.fetchExternalProfile(externalId);
        return ResponseEntity.ok(ApiResponse.success("External profile retrieved successfully", profile));
    }

    private Pageable buildPageable(int page, int size, String sortBy, String direction) {
        Sort.Direction dir = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return org.springframework.data.domain.PageRequest.of(page, size, Sort.by(dir, sortBy));
    }
}
