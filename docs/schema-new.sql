-- Run manually on MySQL 8.0.16+; creates a separate demo database.
-- No existing CampusTrace database is dropped or changed.
CREATE DATABASE IF NOT EXISTS campustrace_demo CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE campustrace_demo;
CREATE TABLE IF NOT EXISTS `user` (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 full_name VARCHAR(100) NOT NULL,
 email VARCHAR(254) NOT NULL UNIQUE,
 password VARCHAR(255) NOT NULL,
 contact_number VARCHAR(25) NOT NULL,
 role VARCHAR(10) NOT NULL DEFAULT 'USER',
 CONSTRAINT user_role_valid CHECK (role IN ('USER','ADMIN'))
);
CREATE TABLE IF NOT EXISTS item (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 item_name VARCHAR(100) NOT NULL,
 description VARCHAR(255) NOT NULL,
 type VARCHAR(10) NOT NULL,
 posted_by VARCHAR(254) NOT NULL,
 posted_by_name VARCHAR(100) NOT NULL,
 contact_number VARCHAR(25) NOT NULL,
 location VARCHAR(150),
 status VARCHAR(10) NOT NULL DEFAULT 'OPEN',
 CONSTRAINT item_type_valid CHECK (type IN ('LOST','FOUND')),
 CONSTRAINT item_status_valid CHECK (status IN ('OPEN','RESOLVED'))
);
