USE campusnest;

INSERT INTO pg_owners (demo_key,owner_name,contact_no,address,owner_status,is_demo) VALUES
('demo-nest-nook','Anjali K.','919876543210','Kundoormala, Thalassery','APPROVED',TRUE),
('demo-greenview','Ramesh P.','919876543211','Kuttoor Road, Thalassery','APPROVED',TRUE),
('demo-courtyard','Fathima M.','919876543212','Kundoormala Junction, Thalassery','APPROVED',TRUE),
('demo-sree-lakshmi','Deepa S.','919876543213','Kuttoor, Thalassery','APPROVED',TRUE),
('demo-hillview','Niyas K.','919876543214','Kundoormala, Thalassery','APPROVED',TRUE),
('demo-olive','Sajitha R.','919876543215','Near COET Road, Kuttoor','APPROVED',TRUE),
('demo-campus-corner','Manoj V.','919876543216','COET Road, Kundoormala','APPROVED',TRUE),
('demo-malabar','Rasiya A.','919876543217','Kuttoor, Thalassery','APPROVED',TRUE),
('demo-comfort','Shihab T.','919876543218','Thalassery Bypass Road','APPROVED',TRUE),
('demo-anugraha','Bindu C.','919876543219','Near Kuttoor Market, Thalassery','APPROVED',TRUE),
('demo-bluebell','Arun K.','919876543220','Thalassery, Kannur District','APPROVED',TRUE),
('demo-haven','Shabna P.','919876543221','Kundoormala Extension, Thalassery','APPROVED',TRUE)
ON DUPLICATE KEY UPDATE owner_name=VALUES(owner_name),contact_no=VALUES(contact_no),address=VALUES(address),owner_status='APPROVED',is_demo=TRUE;

INSERT INTO pg_details (demo_key,owner_id,pg_name,gender_type,monthly_rent,room_type,food_facility,total_beds,vacant_beds,dist_from_coet,pg_address,amenities_json,description,moderation_status,is_visible,is_demo)
SELECT d.demo_key,o.owner_id,d.pg_name,d.gender_type,d.monthly_rent,d.room_type,d.food_facility,d.total_beds,d.vacant_beds,d.dist_from_coet,d.pg_address,d.amenities_json,d.description,'APPROVED',TRUE,TRUE
FROM (
 SELECT 'demo-nest-nook' demo_key,'Nest & Nook' pg_name,'GIRLS' gender_type,5500 monthly_rent,'Shared' room_type,TRUE food_facility,18 total_beds,3 vacant_beds,0.7 dist_from_coet,'Kundoormala, Thalassery' pg_address,JSON_ARRAY('Wi-Fi','Meals','Laundry','Study area') amenities_json,'A bright, comfortable home in a quiet lane near COET. Shared rooms include meals and a dedicated study corner.' description UNION ALL
 SELECT 'demo-greenview','Greenview Residency','BOYS',4200,'Shared',TRUE,24,5,1.1,'Kuttoor Road, Thalassery',JSON_ARRAY('Wi-Fi','Meals','Parking','Laundry'),'Well-kept shared accommodation with home-style food and easy access to local shops.' UNION ALL
 SELECT 'demo-courtyard','The Courtyard PG','GIRLS',7000,'Single',FALSE,10,2,1.4,'Kundoormala Junction, Thalassery',JSON_ARRAY('Wi-Fi','Attached bath','Study desk'),'A private room in a calm family-run property. Common kitchen access and a study desk are included.' UNION ALL
 SELECT 'demo-sree-lakshmi','Sree Lakshmi Ladies Home','GIRLS',3800,'Shared',TRUE,20,0,1.8,'Kuttoor, Thalassery',JSON_ARRAY('Meals','Wi-Fi','Laundry'),'Friendly shared accommodation with meals and laundry access, a short commute to campus.' UNION ALL
 SELECT 'demo-hillview','Hillview Student Stay','BOYS',5000,'Shared',FALSE,16,4,2.2,'Kundoormala, Thalassery',JSON_ARRAY('Wi-Fi','Parking','Study area'),'Simple, clean rooms with a shared study space and secure two-wheeler parking.' UNION ALL
 SELECT 'demo-olive','Olive Nest','GIRLS',6500,'Single',TRUE,8,1,0.9,'Near COET Road, Kuttoor',JSON_ARRAY('Meals','Wi-Fi','Attached bath'),'Private rooms with meals available in a small, well-maintained property.' UNION ALL
 SELECT 'demo-campus-corner','Campus Corner','BOYS',3000,'Shared',FALSE,30,8,0.3,'COET Road, Kundoormala',JSON_ARRAY('Wi-Fi','Parking'),'Affordable shared rooms close to campus, with Wi-Fi and open parking.' UNION ALL
 SELECT 'demo-malabar','Malabar House','GIRLS',6000,'Shared',TRUE,14,3,2.7,'Kuttoor, Thalassery',JSON_ARRAY('Meals','Wi-Fi','Laundry','Security'),'A welcoming stay offering meals, laundry and secure entry.' UNION ALL
 SELECT 'demo-comfort','Comfort Stay','BOYS',4500,'Single',FALSE,12,2,3.1,'Thalassery Bypass Road',JSON_ARRAY('Wi-Fi','Attached bath','Parking'),'Furnished single rooms with attached bathrooms and parking.' UNION ALL
 SELECT 'demo-anugraha','Anugraha Ladies PG','GIRLS',4800,'Shared',TRUE,16,4,3.5,'Near Kuttoor Market, Thalassery',JSON_ARRAY('Meals','Wi-Fi','Study area'),'Shared rooms, home-style meals and a comfortable common study area.' UNION ALL
 SELECT 'demo-bluebell','Bluebell Residency','BOYS',7500,'Single',TRUE,9,1,4.2,'Thalassery, Kannur District',JSON_ARRAY('Meals','Wi-Fi','Attached bath','Laundry'),'A fully furnished private room with meals available, further into Thalassery.' UNION ALL
 SELECT 'demo-haven','Haven House','GIRLS',3500,'Shared',FALSE,22,6,4.5,'Kundoormala Extension, Thalassery',JSON_ARRAY('Wi-Fi','Parking','Laundry'),'Budget-friendly shared rooms with Wi-Fi and laundry access.'
) d JOIN pg_owners o ON o.demo_key=d.demo_key
ON DUPLICATE KEY UPDATE owner_id=VALUES(owner_id),pg_name=VALUES(pg_name),monthly_rent=VALUES(monthly_rent),vacant_beds=VALUES(vacant_beds),is_visible=TRUE,moderation_status='APPROVED',is_demo=TRUE;
