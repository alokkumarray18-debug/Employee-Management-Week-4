-- Run this against your local MySQL server before starting the app
-- (or let Hibernate auto-create/update it in the 'local'/'docker' profiles
-- with spring.jpa.hibernate.ddl-auto=update).

CREATE DATABASE IF NOT EXISTS employee_db_local;
USE employee_db_local;

CREATE TABLE IF NOT EXISTS departments (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    department_name VARCHAR(100) NOT NULL UNIQUE,
    location        VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS employees (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(100)  NOT NULL,
    email         VARCHAR(100)  NOT NULL UNIQUE,
    salary        DOUBLE        NOT NULL,
    joining_date  DATE,
    department_id BIGINT        NOT NULL,
    CONSTRAINT fk_employees_department
        FOREIGN KEY (department_id) REFERENCES departments(id)
);

CREATE TABLE IF NOT EXISTS employee_audit (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id  BIGINT       NOT NULL,
    action       VARCHAR(50)  NOT NULL,
    performed_by VARCHAR(100) NOT NULL,
    performed_at DATETIME     NOT NULL,
    details      VARCHAR(255)
);

-- Sample seed data (optional)
INSERT INTO departments (department_name, location) VALUES
    ('Engineering', 'Bengaluru'),
    ('Marketing',   'Mumbai'),
    ('Finance',     'Pune');

INSERT INTO employees (name, email, salary, joining_date, department_id) VALUES
    ('Alice Johnson', 'alice.johnson@example.com', 75000.00, '2024-01-15', 1),
    ('Bob Smith',     'bob.smith@example.com',     60000.00, '2024-02-01', 2),
    ('Carol Davis',   'carol.davis@example.com',   68000.00, '2024-03-10', 3);
