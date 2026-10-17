# COET CampusNest

A Java-first MVP for finding and managing Paying Guest accommodation near College of Engineering Thalassery. The Servlet/JDBC/MySQL application owns search, accounts, owner verification, inquiries, listing moderation and vacancy data. The existing visual interface stays in HTML, CSS and vanilla JavaScript because it runs in the browser and calls the Java API with `fetch()`.

## Stack

- Java 17
- Jakarta Servlet 6.0 on Tomcat 10.1+
- JDBC with MySQL 8
- Maven WAR package
- HTML5, CSS3 and vanilla JavaScript, with no frontend build step

## Architecture and data model

The application follows the project PDF's three tiers: the browser UI is the presentation tier, Jakarta Servlets on Tomcat implement the application tier, and MySQL is the database tier accessed through JDBC. The core relational entities follow the PDF EER: `users`, `pg_owners`, and `pg_details`. One owner can have many PG details; student users can submit many inquiries, each for one PG. The EER's login, owner identity, contact, ID proof, PG category, rent, food facility, vacancy, and distance fields are represented directly. The MVP also stores total beds, room type, full PG address, amenities, listing moderation and visibility, inquiries, and feedback.

`database/schema.sql` creates a fresh database, while `database/seed.sql` adds clearly marked simulated listings. Existing v1 databases should be backed up before migration. From the repository root, open the MySQL client with `mysql -u root -p campusnest`, then run `source database/migrate-v1-to-eer.sql;`. The migration retains copies of the original data in tables ending with `_v1_backup` and carries users, listings, inquiries, feedback, and owner proof metadata into the new model. The local workspace database was migrated and backed up under the ignored `work/` directory.

## Run locally

### Ready-to-run Windows setup in this workspace

The project workspace contains a portable MySQL 8.4.11 and Tomcat 10.1.61 setup under `work/`. Run `./start-local.ps1` from PowerShell to rebuild the WAR, start the local services, and open the app at `http://127.0.0.1:8081/campusnest/`. MySQL stores its data under `work/mysql-data`; the local admin password is in `work/admin-credentials.txt`. These files are for this development workspace only.

### Manual setup

1. Install JDK 17, Maven and MySQL 8. Configure MySQL with a user that can create/use the `campusnest` database.
2. In MySQL, run `database/schema.sql`, then `database/seed.sql`. The seed creates 12 clearly simulated PG listings with fictional owner contact details.
3. Set the connection environment variables before starting Tomcat:

   - `CAMPUSNEST_DB_URL` (default: `jdbc:mysql://localhost:3306/campusnest?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC`)
   - `CAMPUSNEST_DB_USER` (default: `root`)
   - `CAMPUSNEST_DB_PASSWORD` (default: blank)
   - `CAMPUSNEST_UPLOAD_DIR` (optional; owner ID proof directory, default is `campusnest-private/owner-proofs` under Tomcat's working directory)
   - `CAMPUSNEST_ADMIN_EMAIL` and `CAMPUSNEST_ADMIN_PASSWORD` (optional admin bootstrap; password must be at least 12 characters)

   The admin account is created on application startup only when both admin variables are set. Store secrets outside source control.
4. Build the app with `mvn package`.
5. Copy `target/campusnest.war` to Tomcat's `webapps` directory and start Tomcat. Open `http://localhost:8080/campusnest/`.

The pages call `/api/*` relative to the application context. `GET /api/health` reports whether the Java service and database are reachable. Run the app through Tomcat rather than opening `index.html` directly.

## Included flows

- Public search and sorting by text, gender, maximum rent, room type, food and distance; PG details, phone contact and inquiry form.
- Student registration and login; owner registration with PDF/JPG/PNG ID proof upload.
- Owner dashboard with listings, simple vacancy controls, listing create/edit and inquiries.
- Admin dashboard with counts, owner approval/rejection, listing visibility/deletion and feedback.
- Terms and Privacy draft copy in the interface, marked for business and legal review.

Owners remain pending until an admin approves them. New owner listings are submitted for moderation. Demo records are fictional and visibly marked as simulated. Placeholder illustrations stand in for owner photos.

## API outline

- `GET /api/listings`, `GET /api/listings/{id}`
- `GET /api/auth/csrf`, `GET /api/auth/me`, `POST /api/auth/login`, `POST /api/auth/register`, `POST /api/auth/logout`
- `POST /api/owners/register`, `POST /api/inquiries`, `POST /api/feedback`
- `GET /api/owner/listings`, `POST /api/owner/listings`, `PUT /api/owner/listings/{id}`, `PUT /api/owner/listings/{id}/vacancy`, `GET /api/owner/inquiries`
- `GET /api/admin/dashboard`, `GET /api/admin/owners`, `PUT /api/admin/owners/{id}/status`, `GET /api/admin/owners/{id}/proof`, `GET /api/admin/listings`, `PUT /api/admin/listings/{id}/status`, `PUT /api/admin/listings/{id}`, `DELETE /api/admin/listings/{id}`, `GET /api/admin/feedback`

JSON writes use a session CSRF token. Passwords are stored as salted PBKDF2 hashes. Database queries use prepared statements. Owner documents are stored outside the public web directory and can be viewed only by an authenticated admin through the protected proof endpoint.

## Before a real launch

This is an MVP, not a production deployment. Configure HTTPS and secure session cookies at the deployment layer, set a least-privilege database account, and decide ID-proof access, retention and deletion rules. The legal pages are drafts, demo data is fictional, and account/document workflows need real-device and deployment validation. There is no payment, booking, map or recommendation feature.


