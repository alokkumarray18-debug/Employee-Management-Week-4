package com.trainee.employeemanagement.repository;

import com.trainee.employeemanagement.audit.EmployeeAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmployeeAuditRepository extends JpaRepository<EmployeeAudit, Long> {
    List<EmployeeAudit> findByEmployeeId(Long employeeId);
}
