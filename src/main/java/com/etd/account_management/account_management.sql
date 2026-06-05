-- ============================================================
-- Employee Travel Desk — Account Management Service
-- MySQL Reference Schema  (v2)
-- This file is documentation only — NOT executed by the app.
-- Hibernate (ddl-auto=update) auto-creates all tables on startup.
-- ============================================================

-- NOTE: refresh_tokens and token_blacklist tables are owned by
-- auth-service (port 8080). Do not add them here.

USE account_management;

-- ──────────────────────────────────────────────────────────────
-- grades
-- Seeded on startup: Grade-1 (most senior), Grade-2, Grade-3 (most junior)
-- ──────────────────────────────────────────────────────────────
CREATE TABLE grades (
    id   INT          NOT NULL AUTO_INCREMENT,
    name VARCHAR(25)  NOT NULL,
    PRIMARY KEY (id)
);

INSERT INTO grades (name) VALUES
    ('Grade-1'),
    ('Grade-2'),
    ('Grade-3');

-- ──────────────────────────────────────────────────────────────
-- employees
-- IDs auto-generated from a sequence starting at 100000 (always 6 digits).
-- Passwords stored as BCrypt hashes (strength 12) — never plain text.
-- ──────────────────────────────────────────────────────────────
CREATE TABLE employees (
    employee_id   BIGINT       NOT NULL AUTO_INCREMENT,
    first_name    VARCHAR(15),
    last_name     VARCHAR(10),
    phone_number  VARCHAR(10)  UNIQUE,
    email_address VARCHAR(50)  NOT NULL UNIQUE,
    password      VARCHAR(255) NOT NULL,
    role          VARCHAR(15)  CHECK (role IN ('Employee', 'HR', 'TravelDeskExe')),
    current_grade_id INT,
    access_granted   BOOLEAN  NOT NULL DEFAULT TRUE,
    PRIMARY KEY (employee_id),
    CONSTRAINT fk_employee_grade FOREIGN KEY (current_grade_id) REFERENCES grades (id)
) AUTO_INCREMENT = 100000;

-- Seeded on startup by DataInitializer.
-- Passwords below are BCrypt (strength 12) hashes — replace with values
-- generated at runtime; these are placeholders for the reference schema.
--   HR          plain: Admin@123
--   TravelDeskExe plain: Exec@123
--   Employee    plain: Employee@123
INSERT INTO employees (first_name, last_name, phone_number, email_address, password, role, current_grade_id, access_granted) VALUES
    ('Admin', 'HR',       '9000000001', 'admin.hr@cognizant.com',       '$2a$12$PLACEHOLDER_HR_HASH',       'HR',           1, TRUE),
    ('Desk',  'Exec',     '9000000002', 'desk.exec@cognizant.com',      '$2a$12$PLACEHOLDER_EXEC_HASH',     'TravelDeskExe', 1, TRUE),
    ('John',  'Employee', '9000000003', 'john.employee@cognizant.com',   '$2a$12$PLACEHOLDER_EMPLOYEE_HASH', 'Employee',     3, TRUE);

-- ──────────────────────────────────────────────────────────────
-- grades_history
-- Append-only audit log: one row per grade assignment.
-- A row is inserted on employee create AND on every grade change.
-- ──────────────────────────────────────────────────────────────
CREATE TABLE grades_history (
    id          BIGINT   NOT NULL AUTO_INCREMENT,
    assigned_on DATETIME,
    employee_id BIGINT   NOT NULL,
    grade_id    INT      NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_history_employee FOREIGN KEY (employee_id) REFERENCES employees (employee_id),
    CONSTRAINT fk_history_grade    FOREIGN KEY (grade_id)    REFERENCES grades (id)
);

-- ──────────────────────────────────────────────────────────────
-- Verification queries
-- ──────────────────────────────────────────────────────────────
SELECT * FROM grades;
SELECT * FROM employees;
SELECT * FROM grades_history;
