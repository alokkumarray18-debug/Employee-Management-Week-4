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
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeServiceImpl.class);

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final EmployeeAuditRepository employeeAuditRepository;

    // =========================
    // CREATE EMPLOYEE
    // =========================
    @Override
    @Transactional
    public EmployeeResponseDTO createEmployee(EmployeeRequestDTO dto) {
        log.info("Creating employee with email={}", dto.getEmail());
        Employee saved = persistNewEmployee(dto);
        log.info("Employee created successfully id={}", saved.getId());
        return mapToResponse(saved);
    }

    // =========================
    // CREATE EMPLOYEE + AUDIT (single transaction, section 9)
    // =========================
    @Override
    @Transactional
    public EmployeeResponseDTO createEmployeeWithAudit(EmployeeRequestDTO dto, String performedBy) {
        log.info("Creating employee with audit trail, email={}, performedBy={}", dto.getEmail(), performedBy);

        Employee saved = persistNewEmployee(dto);

        // Written in the SAME transaction as the employee insert above. If this
        // throws (e.g. a DB constraint violation, or the RuntimeException below
        // when performedBy is blank), Spring's @Transactional rolls back the
        // entire method - the employee insert included - because both the
        // employee save and the audit save execute against the same
        // EntityManager/transaction and RuntimeExceptions trigger rollback by
        // default. No employee row and no audit row are left behind.
        if (performedBy == null || performedBy.isBlank()) {
            throw new IllegalArgumentException("performedBy is required to record an audit entry");
        }
        EmployeeAudit audit = new EmployeeAudit(saved.getId(), "CREATE", performedBy,
                "Employee created with email " + saved.getEmail());
        employeeAuditRepository.save(audit);

        log.info("Employee {} created and audited by {}", saved.getId(), performedBy);
        return mapToResponse(saved);
    }

    private Employee persistNewEmployee(EmployeeRequestDTO dto) {
        if (employeeRepository.existsByEmail(dto.getEmail())) {
            log.warn("Attempted to create employee with duplicate email={}", dto.getEmail());
            throw new DuplicateResourceException("Employee with email already exists: " + dto.getEmail());
        }

        Department department = departmentRepository.findById(dto.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Department not found with id: " + dto.getDepartmentId()));

        Employee employee = new Employee();
        employee.setName(dto.getName());
        employee.setEmail(dto.getEmail());
        employee.setDepartment(department);
        employee.setSalary(dto.getSalary());
        employee.setJoiningDate(dto.getJoiningDate());

        return employeeRepository.save(employee);
    }

    // =========================
    // GET EMPLOYEE BY ID
    // =========================
    @Override
    @Transactional(readOnly = true)
    public EmployeeResponseDTO getEmployeeById(Long id) {
        log.debug("Fetching employee id={}", id);
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Employee not found id={}", id);
                    return new ResourceNotFoundException("Employee not found with id: " + id);
                });
        return mapToResponse(employee);
    }

    // =========================
    // GET ALL (paginated + sortable)
    // =========================
    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<EmployeeResponseDTO> getAllEmployees(Pageable pageable) {
        log.debug("Fetching employees page={} size={} sort={}",
                pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());
        Page<EmployeeResponseDTO> page = employeeRepository.findAll(pageable).map(this::mapToResponse);
        return PageResponseDTO.from(page);
    }

    // =========================
    // SEARCH BY NAME (paginated)
    // =========================
    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<EmployeeResponseDTO> searchByName(String name, Pageable pageable) {
        log.debug("Searching employees by name~='{}'", name);
        Page<EmployeeResponseDTO> page = employeeRepository
                .findByNameContainingIgnoreCase(name, pageable)
                .map(this::mapToResponse);
        return PageResponseDTO.from(page);
    }

    // =========================
    // FILTER BY DEPARTMENT (paginated)
    // =========================
    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<EmployeeResponseDTO> getByDepartment(Long departmentId, Pageable pageable) {
        log.debug("Fetching employees for departmentId={}", departmentId);
        if (!departmentRepository.existsById(departmentId)) {
            throw new ResourceNotFoundException("Department not found with id: " + departmentId);
        }
        Page<EmployeeResponseDTO> page = employeeRepository
                .findByDepartmentId(departmentId, pageable)
                .map(this::mapToResponse);
        return PageResponseDTO.from(page);
    }

    // =========================
    // UPDATE EMPLOYEE
    // =========================
    @Override
    @Transactional
    public EmployeeResponseDTO updateEmployee(Long id, EmployeeRequestDTO dto) {
        log.info("Updating employee id={}", id);
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));

        if (employeeRepository.existsByEmailAndIdNot(dto.getEmail(), id)) {
            log.warn("Update rejected: email {} already used by another employee", dto.getEmail());
            throw new DuplicateResourceException("Employee with email already exists: " + dto.getEmail());
        }

        Department department = departmentRepository.findById(dto.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Department not found with id: " + dto.getDepartmentId()));

        employee.setName(dto.getName());
        employee.setEmail(dto.getEmail());
        employee.setDepartment(department);
        employee.setSalary(dto.getSalary());
        employee.setJoiningDate(dto.getJoiningDate());

        Employee updated = employeeRepository.save(employee);
        log.info("Employee updated successfully id={}", updated.getId());
        return mapToResponse(updated);
    }

    // =========================
    // DELETE EMPLOYEE
    // =========================
    @Override
    @Transactional
    public void deleteEmployee(Long id) {
        log.info("Deleting employee id={}", id);
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
        employeeRepository.delete(employee);
        log.info("Employee deleted id={}", id);
    }

    // =========================
    // ADVANCED QUERIES
    // =========================
    @Override
    @Transactional(readOnly = true)
    public List<EmployeeResponseDTO> getEmployeesAboveSalary(Double salary) {
        return employeeRepository.findBySalaryGreaterThan(salary)
                .stream().map(this::mapToResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeResponseDTO getHighestPaidEmployee() {
        Employee employee = employeeRepository.findHighestPaidEmployee()
                .orElseThrow(() -> new ResourceNotFoundException("No employees found"));
        return mapToResponse(employee);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeResponseDTO> getEmployeesJoinedAfter(LocalDate date) {
        return employeeRepository.findEmployeesJoinedAfter(date)
                .stream().map(this::mapToResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> getAverageSalaryByDepartment() {
        return employeeRepository.averageSalaryByDepartmentJpql();
    }

    // =========================
    // MAPPING
    // =========================
    private EmployeeResponseDTO mapToResponse(Employee employee) {
        Department department = employee.getDepartment();
        return EmployeeResponseDTO.builder()
                .id(employee.getId())
                .name(employee.getName())
                .email(employee.getEmail())
                .salary(employee.getSalary())
                .joiningDate(employee.getJoiningDate())
                .departmentId(department != null ? department.getId() : null)
                .departmentName(department != null ? department.getDepartmentName() : null)
                .build();
    }
}
