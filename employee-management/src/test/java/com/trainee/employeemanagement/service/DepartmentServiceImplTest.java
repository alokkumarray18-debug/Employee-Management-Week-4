package com.trainee.employeemanagement.service;

import com.trainee.employeemanagement.dto.DepartmentRequestDTO;
import com.trainee.employeemanagement.dto.DepartmentResponseDTO;
import com.trainee.employeemanagement.entity.Department;
import com.trainee.employeemanagement.entity.Employee;
import com.trainee.employeemanagement.exception.BadRequestException;
import com.trainee.employeemanagement.exception.DuplicateResourceException;
import com.trainee.employeemanagement.exception.ResourceNotFoundException;
import com.trainee.employeemanagement.repository.DepartmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceImplTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @InjectMocks
    private DepartmentServiceImpl departmentService;

    private Department department;
    private DepartmentRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        department = new Department();
        department.setId(1L);
        department.setDepartmentName("Engineering");
        department.setLocation("Bengaluru");

        requestDTO = new DepartmentRequestDTO();
        requestDTO.setDepartmentName("Engineering");
        requestDTO.setLocation("Bengaluru");
    }

    // ---------- create: success ----------
    @Test
    void create_success() {
        when(departmentRepository.existsByDepartmentNameIgnoreCase("Engineering")).thenReturn(false);
        when(departmentRepository.save(any(Department.class))).thenReturn(department);

        DepartmentResponseDTO result = departmentService.create(requestDTO);

        assertThat(result.getDepartmentName()).isEqualTo("Engineering");
        assertThat(result.getEmployeeCount()).isEqualTo(0);
    }

    // ---------- create: duplicate name ----------
    @Test
    void create_duplicateName_throws() {
        when(departmentRepository.existsByDepartmentNameIgnoreCase("Engineering")).thenReturn(true);

        assertThatThrownBy(() -> departmentService.create(requestDTO))
                .isInstanceOf(DuplicateResourceException.class);

        verify(departmentRepository, never()).save(any());
    }

    // ---------- create: invalid input surfaces as repository failure ----------
    @Test
    void create_repositoryFailure_propagates() {
        when(departmentRepository.existsByDepartmentNameIgnoreCase("Engineering")).thenReturn(false);
        when(departmentRepository.save(any(Department.class)))
                .thenThrow(new DataIntegrityViolationException("constraint violation"));

        assertThatThrownBy(() -> departmentService.create(requestDTO))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ---------- getById: success ----------
    @Test
    void getById_success() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));

        DepartmentResponseDTO result = departmentService.getById(1L);

        assertThat(result.getId()).isEqualTo(1L);
    }

    // ---------- getById: not found ----------
    @Test
    void getById_notFound_throws() {
        when(departmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> departmentService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- delete: not found ----------
    @Test
    void delete_notFound_throws() {
        when(departmentRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> departmentService.delete(7L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- delete: rejected when department still has employees ----------
    @Test
    void delete_withEmployees_throwsBadRequest() {
        Employee employee = new Employee();
        employee.setId(5L);
        department.setEmployees(List.of(employee));
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));

        assertThatThrownBy(() -> departmentService.delete(1L))
                .isInstanceOf(BadRequestException.class);

        verify(departmentRepository, never()).delete(any());
    }

    // ---------- delete: success when empty ----------
    @Test
    void delete_success() {
        department.setEmployees(List.of());
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));

        departmentService.delete(1L);

        verify(departmentRepository).delete(department);
    }
}
