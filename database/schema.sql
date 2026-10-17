CREATE DATABASE IF NOT EXISTS campusnest CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE campusnest;

-- Identity/login rows. OWNER profiles are stored in PG_OWNERS below.
CREATE TABLE IF NOT EXISTS users (
  user_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(140) NOT NULL,
  email VARCHAR(190) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  phone_no VARCHAR(32),
  role ENUM('STUDENT','OWNER','ADMIN') NOT NULL,
  account_status ENUM('ACTIVE','DISABLED') NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Matches the PG_OWNERS entity in the PDF. Demo owners have no login user_id.
CREATE TABLE IF NOT EXISTS pg_owners (
  owner_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT UNIQUE,
  demo_key VARCHAR(100) UNIQUE,
  owner_name VARCHAR(140) NOT NULL,
  contact_no VARCHAR(32) NOT NULL,
  address TEXT NOT NULL,
  id_proof VARCHAR(180),
  proof_original_name VARCHAR(180),
  proof_media_type VARCHAR(80),
  proof_reviewed_at TIMESTAMP NULL,
  owner_status ENUM('PENDING','APPROVED','REJECTED') NOT NULL DEFAULT 'PENDING',
  is_demo BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_pg_owner_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- Matches the PG_DETAILS entity/attributes in the PDF, with MVP fields added for
-- room type, total beds, full property address, amenities, and moderation.
CREATE TABLE IF NOT EXISTS pg_details (
  pg_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  owner_id BIGINT NOT NULL,
  demo_key VARCHAR(100) UNIQUE,
  pg_name VARCHAR(100) NOT NULL,
  gender_type ENUM('BOYS','GIRLS') NOT NULL,
  monthly_rent DECIMAL(8,2) NOT NULL,
  room_type ENUM('Shared','Single') NOT NULL DEFAULT 'Shared',
  food_facility BOOLEAN NOT NULL DEFAULT FALSE,
  total_beds SMALLINT UNSIGNED NOT NULL DEFAULT 1,
  vacant_beds INT UNSIGNED NOT NULL DEFAULT 0,
  dist_from_coet DECIMAL(3,1) NOT NULL,
  pg_address VARCHAR(255) NOT NULL,
  amenities_json JSON NOT NULL,
  description TEXT,
  moderation_status ENUM('PENDING','APPROVED','REJECTED') NOT NULL DEFAULT 'PENDING',
  is_visible BOOLEAN NOT NULL DEFAULT TRUE,
  is_demo BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_pg_details_owner FOREIGN KEY (owner_id) REFERENCES pg_owners(owner_id) ON DELETE CASCADE,
  CONSTRAINT chk_pg_vacant_beds CHECK (vacant_beds <= total_beds),
  INDEX idx_pg_details_search (is_visible, moderation_status, gender_type, monthly_rent, dist_from_coet)
) ENGINE=InnoDB;

-- USERS can send many inquiries to PG_DETAILS; anonymous MVP inquiries are allowed.
CREATE TABLE IF NOT EXISTS inquiries (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  pg_id BIGINT NOT NULL,
  student_user_id BIGINT NULL,
  sender_name VARCHAR(140) NOT NULL,
  sender_phone VARCHAR(32) NOT NULL,
  message VARCHAR(2000) NOT NULL,
  status ENUM('NEW','READ','CLOSED') NOT NULL DEFAULT 'NEW',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_inquiry_pg FOREIGN KEY (pg_id) REFERENCES pg_details(pg_id) ON DELETE CASCADE,
  CONSTRAINT fk_inquiry_student FOREIGN KEY (student_user_id) REFERENCES users(user_id) ON DELETE SET NULL,
  INDEX idx_inquiry_pg (pg_id, created_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS feedback (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NULL,
  name VARCHAR(140) NOT NULL,
  email VARCHAR(190),
  message VARCHAR(2000) NOT NULL,
  status ENUM('NEW','REVIEWED','CLOSED') NOT NULL DEFAULT 'NEW',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_feedback_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL
) ENGINE=InnoDB;
