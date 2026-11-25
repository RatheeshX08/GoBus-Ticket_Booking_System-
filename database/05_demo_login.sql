-- Add the demo login account to an existing bus_booking_db.
-- Demo credentials: demo@gobus.com / password123
USE bus_booking_db;

CREATE TABLE IF NOT EXISTS users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(120) NOT NULL UNIQUE,
    password_salt BINARY(16) NOT NULL,
    password_hash BINARY(32) NOT NULL,
    hash_iterations INT NOT NULL DEFAULT 180000,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO users (full_name, email, password_salt, password_hash, hash_iterations) VALUES
('GoBus Demo', 'demo@gobus.com',
 UNHEX('476f4275732d64656d6f2d73616c7421'),
 UNHEX('af318595a0c42a0843d75d3580218e89dd0e784ed443cebd7413eef70cef7778'),
 180000)
ON DUPLICATE KEY UPDATE
    full_name = VALUES(full_name),
    password_salt = VALUES(password_salt),
    password_hash = VALUES(password_hash),
    hash_iterations = VALUES(hash_iterations);
