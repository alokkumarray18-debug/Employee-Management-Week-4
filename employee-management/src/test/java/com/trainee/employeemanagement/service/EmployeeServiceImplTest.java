package com.trainee.employeemanagement.service;

import com.trainee.employeemanagement.audit.EmployeeAudit;
import com.trainee.employeemanagement.dto.EmployeeRequestDTO;
import com.trainee.employeemanagement.dto.EmployeeResponseDTO;
import com.trainee.employeemanagement.dto.PageResponseDTO;
import com.trainee.employeemanagement.entity.Department;
import com.trainee.employeemanagement.entity.Employee;
import com.trainee.employeemanagement.exception.DuplicateResourceException;
import com.trainee.employeemanagement.exception.ResourceNotFoundException;
import com.trainee.employeemanagement.repository.DepartmentRepository;
import com.trainee.employeemanagement.repository.EmployeeAuditRepository;
import com.trainee.employeemanagement.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceImplTest {

    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private EmployeeAuditRepository employeeAuditRepository;

    @InjectMocks
    private EmployeeServiceImpl employeeService;

    private Department department;
    private Employee employee;
    private EmployeeRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        department = new Department();
        department.setId(1L);
        department.setDepartmentName("Engineering");
        department.setLocation("Bengaluru");

        employee = new Employee();
        employee.setId(10L);
        employee.setName("Asha Rao");
        employee.setEmail("asha.rao@example.com");
        employee.setSalary(75000.0);
        employee.setJoiningDate(LocalDate.of(2024, 1, 15));
        employee.setDepartment(department);

        requestDTO = new EmployeeRequestDTO();
        requestDTO.setName("Asha Rao");
        requestDTO.setEmail("asha.rao@example.com");
        requestDTO.setSalary(75000.0);
        requestDTO.setJoiningDate(LocalDate.of(2024, 1, 15));
        requestDTO.setDepartmentId(1L);
    }

    // ---------- createEmployee: success ----------
    @Test
    void createEmployee_success() {
        when(employeeRepository.existsByEmail(requestDTO.getEmail())).thenReturn(false);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(employeeRepository.save(any(Employee.class))).thenReturn(employee);

        EmployeeResponseDTO result = employeeService.createEmployee(requestDTO);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getEmail()).isEqualTo("asha.rao@example.com");
        assertThat(result.getDepartmentName()).isEqualTo("Engineering");
        verify(employeeRepository).save(any(Employee.class));
    }

    // ---------- createEmployee: duplicate email ----------
    @Test
    void createEmployee_duplicateEmail_throws() {
        when(employeeRepository.existsByEmail(requestDTO.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> employeeService.createEmployee(requestDTO))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining(requestDTO.getEmail());

        verify(employeeRepository, never()).save(any());
    }

    // ---------- createEmployee: department not found ----------
    @Test
    void createEmployee_departmentNotFound_throws() {
        when(employeeRepository.existsByEmail(requestDTO.getEmail())).thenReturn(false);
        when(departmentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.createEmployee(requestDTO))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(employeeRepository, never()).save(any());
    }

    // ---------- createEmployee: invalid input surfaces as repository failure ----------
    @Test
    void createEmployee_repositoryFailure_propagates() {
        when(employeeRepository.existsByEmail(requestDTO.getEmail())).thenReturn(false);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(employeeRepository.save(any(Employee.class)))
                .thenThrow(new DataIntegrityViolationException("constraint violation"));

        assertThatThrownBy(() -> employeeService.createEmployee(requestDTO))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ---------- createEmployeeWithAudit: success writes both employee and audit ----------
    @Test
    void createEmployeeWithAudit_success() {
        when(employeeRepository.existsByEmail(requestDTO.getEmail())).thenReturn(false);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(employeeRepository.save(any(Employee.class))).thenReturn(employee);

        employeeService.createEmployeeWithAudit(requestDTO, "admin");

        verify(employeeRepository).save(any(Employee.class));
        verify(employeeAuditRepository).save(any(EmployeeAudit.class));
    }

    // ---------- createEmployeeWithAudit: missing performedBy rejects before saving audit ----------
    @Test
    void createEmployeeWithAudit_missingPerformedBy_throwsAndSkipsAudit() {
        when(employeeRepository.existsByEmail(requestDTO.getEmail())).thenReturn(false);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(employeeRepository.save(any(Employee.class))).thenReturn(employee);

        assertThatThrownBy(() -> employeeService.createEmployeeWithAudit(requestDTO, "  "))
                .isInstanceOf(IllegalArgumentException.class);

        verify(employeeAuditRepository, never()).save(any());
    }

    // ---------- getEmployeeById: success ----------
    @Test
    void getEmployeeById_success() {
        when(employeeRepository.findById(10L)).thenReturn(Optional.of(employee));

        EmployeeResponseDTO result = employeeService.getEmployeeById(10L);

        assertThat(result.getName()).isEqualTo("Asha Rao");
    }

    // ---------- getEmployeeById: not found ----------
    @Test
    void getEmployeeById_notFound_throws() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.getEmployeeById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- getAllEmployees: paginated ----------
    @Test
    void getAllEmployees_returnsPagedResult() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Employee> page = new PageImpl<>(List.of(employee), pageable, 1);
        when(employeeRepository.findAll(pageable)).thenReturn(page);

        PageResponseDTO<EmployeeResponseDTO> result = employeeService.getAllEmployees(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    // ---------- updateEmployee: duplicate email on update ----------
    @Test
    void updateEmployee_duplicateEmail_throws() {
        when(employeeRepository.findById(10L)).thenReturn(Optional.of(employee));
        when(employeeRepository.existsByEmailAndIdNot(requestDTO.getEmail(), 10L)).thenReturn(true);

        assertThatThrownBy(() -> employeeService.updateEmployee(10L, requestDTO))
                .isInstanceOf(DuplicateResourceException.class);
    }

    // ---------- deleteEmployee: not found ----------
    @Test
    void deleteEmployee_notFound_throws() {
        when(employeeRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.deleteEmployee(5L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(employeeRepository, never()).delete(any());
    }

    // ---------- getHighestPaidEmployee: empty repository ----------
    @Test
    void getHighestPaidEmployee_noEmployees_throws() {
        when(employeeRepository.findAllOrderedBySalaryDesc(any(Pageable.class))).thenReturn(List.of());

        assertThatThrownBy(() -> employeeService.getHighestPaidEmployee())
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
