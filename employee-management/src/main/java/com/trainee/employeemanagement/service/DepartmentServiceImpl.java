package com.trainee.employeemanagement.service;

import com.trainee.employeemanagement.dto.DepartmentRequestDTO;
import com.trainee.employeemanagement.dto.DepartmentResponseDTO;
import com.trainee.employeemanagement.entity.Department;
import com.trainee.employeemanagement.exception.BadRequestException;
import com.trainee.employeemanagement.exception.DuplicateResourceException;
import com.trainee.employeemanagement.exception.ResourceNotFoundException;
import com.trainee.employeemanagement.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private static final Logger log = LoggerFactory.getLogger(DepartmentServiceImpl.class);

    private final DepartmentRepository repository;

    private DepartmentResponseDTO toDTO(Department d) {
        return DepartmentResponseDTO.builder()
                .id(d.getId())
                .departmentName(d.getDepartmentName())
                .location(d.getLocation())
                .employeeCount(d.getEmployees() != null ? d.getEmployees().size() : 0)
                .build();
    }

    @Override
    @Transactional
    public DepartmentResponseDTO create(DepartmentRequestDTO dto) {
        log.info("Creating department name={}", dto.getDepartmentName());
        if (repository.existsByDepartmentNameIgnoreCase(dto.getDepartmentName())) {
            throw new DuplicateResourceException("Department already exists: " + dto.getDepartmentName());
        }
        Department d = new Department();
        d.setDepartmentName(dto.getDepartmentName());
        d.setLocation(dto.getLocation());
        Department saved = repository.save(d);
        log.info("Department created id={}", saved.getId());
        return toDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentResponseDTO> getAll() {
        return repository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentResponseDTO getById(Long id) {
        return toDTO(repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id " + id)));
    }

    @Override
    @Transactional
    public DepartmentResponseDTO update(Long id, DepartmentRequestDTO dto) {
        log.info("Updating department id={}", id);
        Department d = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id " + id));

        if (repository.existsByDepartmentNameIgnoreCaseAndIdNot(dto.getDepartmentName(), id)) {
            throw new DuplicateResourceException("Department already exists: " + dto.getDepartmentName());
        }

        d.setDepartmentName(dto.getDepartmentName());
        d.setLocation(dto.getLocation());
        return toDTO(repository.save(d));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        log.info("Deleting department id={}", id);
        Department d = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id " + id));

        // Deliberately NOT cascading deletes to employees (see Department entity
        // javadoc) - a department with active employees must be reassigned or
        // emptied first. This is enforced here rather than relying on a DB FK
        // constraint failure, so the caller gets a clear 400 instead of a raw
        // SQL exception.
        if (d.getEmployees() != null && !d.getEmployees().isEmpty()) {
            throw new BadRequestException(
                    "Cannot delete department " + id + " because it still has " +
                            d.getEmployees().size() + " employee(s) assigned to it");
        }
        repository.delete(d);
        log.info("Department deleted id={}", id);
    }
}
