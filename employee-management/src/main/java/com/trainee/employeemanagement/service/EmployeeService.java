package com.trainee.employeemanagement.service;

import com.trainee.employeemanagement.dto.EmployeeRequestDTO;
import com.trainee.employeemanagement.dto.EmployeeResponseDTO;
import com.trainee.employeemanagement.dto.PageResponseDTO;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface EmployeeService {

    EmployeeResponseDTO createEmployee(EmployeeRequestDTO dto);

    EmployeeResponseDTO createEmployeeWithAudit(EmployeeRequestDTO dto, String performedBy);

    EmployeeResponseDTO getEmployeeById(Long id);

    PageResponseDTO<EmployeeResponseDTO> getAllEmployees(Pageable pageable);

    PageResponseDTO<EmployeeResponseDTO> searchByName(String name, Pageable pageable);

    PageResponseDTO<EmployeeResponseDTO> getByDepartment(Long departmentId, Pageable pageable);

    EmployeeResponseDTO updateEmployee(Long id, EmployeeRequestDTO dto);

    void deleteEmployee(Long id);

    // Advanced queries (assignment section 5)
    List<EmployeeResponseDTO> getEmployeesAboveSalary(Double salary);

    EmployeeResponseDTO getHighestPaidEmployee();

    List<EmployeeResponseDTO> getEmployeesJoinedAfter(LocalDate date);

    List<Object[]> getAverageSalaryByDepartment();
}
