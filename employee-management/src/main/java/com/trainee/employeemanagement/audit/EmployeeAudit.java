package com.trainee.employeemanagement.audit;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * A record of an action performed on an Employee, written in the SAME
 * database transaction as the Employee change itself (see
 * EmployeeServiceImpl#createEmployeeWithAudit). If writing the audit row
 * fails for any reason, the whole transaction - including the Employee
 * insert - is rolled back, so we never end up with an Employee that has no
 * corresponding audit trail.
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "employee_audit")
public class EmployeeAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private String performedBy;

    @Column(nullable = false)
    private LocalDateTime performedAt;

    private String details;

    public EmployeeAudit(Long employeeId, String action, String performedBy, String details) {
        this.employeeId = employeeId;
        this.action = action;
        this.performedBy = performedBy;
        this.details = details;
        this.performedAt = LocalDateTime.now();
    }
}
