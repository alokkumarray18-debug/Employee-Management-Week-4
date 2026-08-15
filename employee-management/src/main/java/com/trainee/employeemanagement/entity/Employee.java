package com.trainee.employeemanagement.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

/**
 * Employee is the "many" side of the Department <-> Employee relationship.
 *
 * FetchType.LAZY is used for the department association: when we load an
 * Employee we usually only need its own fields (name, salary, etc.) and not
 * the full Department graph. Loading the department eagerly on every single
 * employee query (e.g. a paginated list of 10,000 employees) would trigger
 * either an N+1 query storm or an unnecessarily large join, so LAZY keeps the
 * default employee read path cheap. When the department IS needed (e.g. the
 * response DTO wants departmentName) it is fetched explicitly and mapped
 * inside the transactional service method, so the lazy proxy is always
 * resolved while the Hibernate session is still open.
 */
@Data
@Entity
@Table(name = "employees")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private Double salary;

    private LocalDate joiningDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;
}
