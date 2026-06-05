use account_management;

CREATE TABLE grades (
  id INT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(25)
);

INSERT INTO grades (Id, Name) VALUES
(1, 'Grade-1'),
(2, 'Grade-2'),
(3, 'Grade-3');

CREATE TABLE employees (
  employee_id INT PRIMARY KEY AUTO_INCREMENT(100000, 1),
  first_name VARCHAR(15),
  last_name VARCHAR(10),
  phone_number VARCHAR(10) UNIQUE,
  -- Check constraint for exactly 10 digit phone numbers
  CONSTRAINT CHK_PhoneNumber CHECK (phone_number REGEXP '^[0-9]{10}$'),
  email_address VARCHAR(50) UNIQUE,
  -- BCrypt-hashed password; plain text is never stored
  password VARCHAR(255) NOT NULL,
  role VARCHAR(15) CHECK (role IN ('Employee', 'HR', 'TravelDeskExe')),
  current_grade_id INT,
  access_granted BOOLEAN DEFAULT TRUE,
  FOREIGN KEY (current_grade_id) REFERENCES grades(id)
);

-- Pre-populate one HR and one TravelDeskExec on application startup.
-- Passwords below are BCrypt (strength 12) hashes; replace with your own generated values.
-- HR password: Admin@123  |  TravelDeskExec password: Exec@123
INSERT INTO employees (first_name, last_name, phone_number, email_address, password, role, current_grade_id, access_granted) VALUES
('Admin', 'HR',   '9000000001', 'admin.hr@cognizant.com',   '$2a$12$PLACEHOLDER_HASH_FOR_HR_PASSWORD',   'HR',           1, TRUE),
('Desk',  'Exec', '9000000002', 'desk.exec@cognizant.com',  '$2a$12$PLACEHOLDER_HASH_FOR_EXEC_PASSWORD', 'TravelDeskExe', 1, TRUE);

CREATE TABLE grades_history (
  id INT PRIMARY KEY AUTO_INCREMENT,
  assigned_on DATETIME,
  employee_id INT,
  grade_id INT,
  FOREIGN KEY (employee_id) REFERENCES Employees(employee_id),
  FOREIGN KEY (grade_id) REFERENCES Grades(id)
);

DESC grades;
DESC employees;
DESC grades_history;

SELECT * FROM grades;
SELECT * FROM employees;
SELECT * FROM grades_history;