package com.trainee.employeemanagement.service;

import com.trainee.employeemanagement.dto.DepartmentRequestDTO;
import com.trainee.employeemanagement.dto.DepartmentResponseDTO;

import java.util.List;

public interface DepartmentService {
    DepartmentResponseDTO create(DepartmentRequestDTO dto);
    List<DepartmentResponseDTO> getAll();
    DepartmentResponseDTO getById(Long id);
    DepartmentResponseDTO update(Long id, DepartmentRequestDTO dto);
    void delete(Long id);
}
