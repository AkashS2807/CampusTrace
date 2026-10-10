-- MANUAL REVIEW ONLY. Back up the existing database first.
-- Select your existing database in MySQL Workbench; do NOT run against the demo DB.
-- Preflight: each result must be empty. Correct problems only with the data owner's approval.
SELECT LOWER(TRIM(email)), COUNT(*) FROM `user` GROUP BY LOWER(TRIM(email)) HAVING COUNT(*) > 1;
SELECT id FROM `user` WHERE full_name IS NULL OR email IS NULL OR password IS NULL
 OR contact_number IS NULL OR CHAR_LENGTH(full_name)>100 OR CHAR_LENGTH(email)>254 OR CHAR_LENGTH(contact_number)>25;
SELECT id FROM item WHERE item_name IS NULL OR description IS NULL OR posted_by IS NULL
 OR posted_by_name IS NULL OR contact_number IS NULL OR status IS NULL
 OR type IS NULL OR type NOT IN ('LOST','FOUND') OR status NOT IN ('OPEN','RESOLVED')
 OR CHAR_LENGTH(description)>255 OR CHAR_LENGTH(item_name)>100 OR CHAR_LENGTH(posted_by)>254 OR CHAR_LENGTH(posted_by_name)>100
 OR CHAR_LENGTH(contact_number)>25 OR CHAR_LENGTH(location)>150;
-- Stop if any preflight fails. MySQL DDL commits implicitly: these statements cannot be rolled back.
-- Check SHOW CREATE TABLE `user` and item before running. Add role only if it is absent.
ALTER TABLE `user`
 ADD COLUMN role VARCHAR(10) NOT NULL DEFAULT 'USER',
 MODIFY full_name VARCHAR(100) NOT NULL,
 MODIFY email VARCHAR(254) NOT NULL,
 MODIFY password VARCHAR(255) NOT NULL,
 MODIFY contact_number VARCHAR(25) NOT NULL;
ALTER TABLE item
 MODIFY item_name VARCHAR(100) NOT NULL,
 MODIFY description VARCHAR(255) NOT NULL,
 MODIFY type VARCHAR(10) NOT NULL,
 MODIFY posted_by VARCHAR(254) NOT NULL,
 MODIFY posted_by_name VARCHAR(100) NOT NULL,
 MODIFY contact_number VARCHAR(25) NOT NULL,
 MODIFY location VARCHAR(150),
 MODIFY status VARCHAR(10) NOT NULL DEFAULT 'OPEN';
-- Existing email uniqueness was already declared in the old entity; verify an actual unique index exists.
-- If absent, after duplicate preflight succeeds: ALTER TABLE `user` ADD UNIQUE KEY user_email_unique (email);
-- Passwords are deliberately NOT converted or rewritten here. See README for explicit test-account reset.
