package com.trainee.employeemanagement.repository;

import com.trainee.employeemanagement.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    // ---- Derived queries ----
    List<Employee> findBySalaryGreaterThan(Double salary);

    Page<Employee> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<Employee> findByDepartmentId(Long departmentId, Pageable pageable);

    List<Employee> findByDepartmentId(Long departmentId);

    List<Employee> findByJoiningDateAfter(LocalDate date);

    // ---- JPQL ----
    @Query("SELECT e FROM Employee e ORDER BY e.salary DESC")
    List<Employee> findAllOrderedBySalaryDesc(Pageable pageable);

    default Optional<Employee> findHighestPaidEmployee() {
        List<Employee> top = findAllOrderedBySalaryDesc(org.springframework.data.domain.PageRequest.of(0, 1));
        return top.isEmpty() ? Optional.empty() : Optional.of(top.get(0));
    }

    @Query("SELECT e FROM Employee e WHERE e.joiningDate > :date")
    List<Employee> findEmployeesJoinedAfter(@Param("date") LocalDate date);

    @Query("SELECT e.department.id, e.department.departmentName, AVG(e.salary) " +
            "FROM Employee e GROUP BY e.department.id, e.department.departmentName")
    List<Object[]> averageSalaryByDepartmentJpql();

    // ---- Native SQL ----
    @Query(value = "SELECT department_id, AVG(salary) AS avg_salary " +
            "FROM employees GROUP BY department_id", nativeQuery = true)
    List<Object[]> averageSalaryByDepartmentNative();

    @Query(value = "SELECT * FROM employees WHERE salary > :salary", nativeQuery = true)
    List<Employee> findBySalaryAboveNative(@Param("salary") Double salary);
}
