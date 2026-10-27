CREATE DATABASE IF NOT EXISTS service_request_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE service_request_db;

CREATE TABLE IF NOT EXISTS users (
    user_id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS employees (
    employee_id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    department VARCHAR(100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS requests (
    request_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    category VARCHAR(100),
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    assigned_to INT,
    status VARCHAR(30) NOT NULL DEFAULT 'NEW',
    resolution TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP NULL DEFAULT NULL,
    INDEX idx_requests_user_created (user_id, created_at),
    INDEX idx_requests_assigned_to (assigned_to),
    INDEX idx_requests_status (status),
    CONSTRAINT fk_requests_user
        FOREIGN KEY (user_id) REFERENCES users (user_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_requests_employee
        FOREIGN KEY (assigned_to) REFERENCES employees (employee_id)
        ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Demo password for both accounts is: password
-- These BCrypt hashes must be checked with a BCrypt-compatible password verifier.
INSERT INTO users (name, email, password, role)
VALUES
    ('Morgan Admin', 'admin@company.com', '$2a$10$oiGsx2B.lR3hGiHHXS2FGOQqRSLqqs0xmABWkjZJQW0MU9K5pxLgy', 'ADMIN'),
    ('Taylor User', 'user@company.com', '$2a$10$oiGsx2B.lR3hGiHHXS2FGOQqRSLqqs0xmABWkjZJQW0MU9K5pxLgy', 'USER')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    password = VALUES(password),
    role = VALUES(role);

INSERT INTO employees (name, email, department)
VALUES
    ('Jordan Lee', 'jordan.lee@company.com', 'IT Support'),
    ('Casey Morgan', 'casey.morgan@company.com', 'Facilities')
ON DUPLICATE KEY UPDATE email = VALUES(email);

INSERT INTO requests (user_id, title, description, category, priority, assigned_to, status)
SELECT u.user_id,
       'Laptop cannot connect to Wi-Fi',
       'The office laptop disconnects from the wireless network several times a day.',
       'IT Support',
       'HIGH',
       e.employee_id,
       'IN_PROGRESS'
FROM users AS u
JOIN employees AS e ON e.email = 'jordan.lee@company.com'
WHERE u.email = 'user@company.com'
  AND NOT EXISTS (
      SELECT 1
      FROM requests AS existing_request
      WHERE existing_request.user_id = u.user_id
        AND existing_request.title = 'Laptop cannot connect to Wi-Fi'
  );

INSERT INTO requests (user_id, title, description, category, priority, status)
SELECT u.user_id,
       'Replace flickering meeting room light',
       'The ceiling light in meeting room 2 flickers during use.',
       'Facilities',
       'LOW',
       'NEW'
FROM users AS u
WHERE u.email = 'user@company.com'
  AND NOT EXISTS (
      SELECT 1
      FROM requests AS existing_request
      WHERE existing_request.user_id = u.user_id
        AND existing_request.title = 'Replace flickering meeting room light'
  );