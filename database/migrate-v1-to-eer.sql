-- Run from the repository root with the MySQL command-line client.
-- Keeps a copy of each v1 table as *_v1_backup before transforming data.
USE campusnest;

CREATE TABLE users_v1_backup LIKE users;
INSERT INTO users_v1_backup SELECT * FROM users;
CREATE TABLE pg_listings_v1_backup LIKE pg_listings;
INSERT INTO pg_listings_v1_backup SELECT * FROM pg_listings;
CREATE TABLE owner_documents_v1_backup LIKE owner_documents;
INSERT INTO owner_documents_v1_backup SELECT * FROM owner_documents;
CREATE TABLE inquiries_v1_backup LIKE inquiries;
INSERT INTO inquiries_v1_backup SELECT * FROM inquiries;
CREATE TABLE feedback_v1_backup LIKE feedback;
INSERT INTO feedback_v1_backup SELECT * FROM feedback;

-- Remove old foreign keys before renaming referenced user columns. MySQL blocks
-- that rename while a foreign key still points at the old column names.
ALTER TABLE owner_documents DROP FOREIGN KEY fk_document_owner;
ALTER TABLE pg_listings DROP FOREIGN KEY fk_listing_owner;
ALTER TABLE inquiries DROP FOREIGN KEY fk_inquiry_listing, DROP FOREIGN KEY fk_inquiry_student;
ALTER TABLE feedback DROP FOREIGN KEY fk_feedback_user;

RENAME TABLE inquiries TO inquiries_v1_old, feedback TO feedback_v1_old;
UPDATE users SET status='ACTIVE';
ALTER TABLE users
  CHANGE COLUMN id user_id BIGINT NOT NULL AUTO_INCREMENT,
  CHANGE COLUMN full_name name VARCHAR(140) NOT NULL,
  CHANGE COLUMN phone phone_no VARCHAR(32),
  CHANGE COLUMN status account_status ENUM('ACTIVE','DISABLED') NOT NULL DEFAULT 'ACTIVE';

SOURCE database/schema.sql;

INSERT INTO pg_owners(user_id,owner_name,contact_no,address,id_proof,proof_original_name,proof_media_type,proof_reviewed_at,owner_status,is_demo,created_at)
SELECT u.user_id,b.full_name,COALESCE(NULLIF(b.phone,''),'Not provided'),'Address not provided',d.stored_name,d.original_name,d.media_type,d.reviewed_at,
       CASE b.status WHEN 'APPROVED' THEN 'APPROVED' WHEN 'REJECTED' THEN 'REJECTED' ELSE 'PENDING' END,FALSE,b.created_at
FROM users u
JOIN users_v1_backup b ON b.id=u.user_id AND b.role='OWNER'
LEFT JOIN owner_documents_v1_backup d ON d.user_id=b.id;

INSERT INTO pg_owners(demo_key,owner_name,contact_no,address,owner_status,is_demo)
SELECT CONCAT('legacy-',p.id),p.owner_name,p.contact_phone,p.address,'APPROVED',TRUE
FROM pg_listings_v1_backup p WHERE p.owner_user_id IS NULL;

INSERT INTO pg_details(pg_id,owner_id,pg_name,gender_type,monthly_rent,room_type,food_facility,total_beds,vacant_beds,dist_from_coet,pg_address,amenities_json,description,moderation_status,is_visible,is_demo,created_at,updated_at)
SELECT p.id,o.owner_id,LEFT(p.name,100),UPPER(p.gender),p.monthly_rent,p.room_type,p.food_available,p.beds_total,p.beds_vacant,ROUND(p.distance_km,1),p.address,p.amenities_json,p.description,p.moderation_status,p.is_visible,p.is_demo,p.created_at,p.updated_at
FROM pg_listings_v1_backup p
JOIN pg_owners o ON (p.owner_user_id IS NOT NULL AND o.user_id=p.owner_user_id) OR (p.owner_user_id IS NULL AND o.demo_key=CONCAT('legacy-',p.id));

INSERT INTO inquiries(id,pg_id,student_user_id,sender_name,sender_phone,message,status,created_at)
SELECT i.id,i.listing_id,i.student_user_id,i.sender_name,i.sender_phone,i.message,i.status,i.created_at
FROM inquiries_v1_old i;

INSERT INTO feedback(id,user_id,name,email,message,status,created_at)
SELECT id,user_id,name,email,message,status,created_at FROM feedback_v1_old;

DROP TABLE inquiries_v1_old, feedback_v1_old, owner_documents, pg_listings;
