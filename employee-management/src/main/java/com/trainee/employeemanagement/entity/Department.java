package com.trainee.employeemanagement.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Department is the "one" side of the Department <-> Employee relationship.
 *
 * mappedBy = "department" tells Hibernate that the Employee.department field
 * owns the foreign key (department_id lives on the employees table), so this
 * side is the inverse/non-owning side - no extra join column or join table is
 * created for Department itself.
 *
 * FetchType.LAZY is used here too (and is in fact the JPA default for
 * @OneToMany) because a Department's employee list can grow arbitrarily large;
 * eagerly pulling every employee whenever a department is loaded (e.g. for a
 * simple GET /departments/{id}) would be wasteful and unpredictable in cost.
 *
 * CascadeType.PERSIST and CascadeType.MERGE are used instead of CascadeType.ALL
 * on purpose: saving/updating a Department should be able to cascade to
 * employees that are being added programmatically through the parent, but we
 * deliberately do NOT cascade REMOVE - deleting a Department must never
 * silently delete its Employees. That is treated as a business rule violation
 * (see DepartmentServiceImpl#delete) rather than an automatic cascade.
 */
@Entity
@Table(name = "departments")
@Data
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "department_name", nullable = false, unique = true)
    private String departmentName;

    @Column(nullable = false)
    private String location;

    @OneToMany(
            mappedBy = "department",
            fetch = FetchType.LAZY,
            cascade = {CascadeType.PERSIST, CascadeType.MERGE}
    )
    private List<Employee> employees = new ArrayList<>();
}
