# Deploy CampusNest to Vercel

Vercel can build the Java/Tomcat application from `Dockerfile.vercel`. CampusNest still needs an external MySQL server. Vercel does not provide a MySQL database for this app.

## Before importing the repository

1. Create a managed MySQL 8 database with a public TLS-enabled connection. Allow connections from Vercel (or use a database provider that supports Vercel integrations/private connectivity).
2. From a MySQL client, run `database/schema.sql` and then `database/seed.sql` against that database.
3. Keep the database hostname, database name, username, password, and TLS requirements. Never commit them to Git.

## Deploy

1. Sign in to Vercel and choose **Add New → Project**.
2. Import `sharuuuuuhh/campusnext` from GitHub and select branch `main`.
3. Vercel detects the root `Dockerfile.vercel`. Leave the project root as `.` and deploy with the Container runtime. If the import screen asks for a framework, choose **Other** or **Container**.
4. Before relying on the deployment, add the variables below in **Project → Settings → Environment Variables** for Production (and Preview if wanted). Redeploy after adding them.

```text
CAMPUSNEST_DB_URL=jdbc:mysql://DB_HOST:3306/campusnest?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC&sslMode=REQUIRED
CAMPUSNEST_DB_USER=DB_USERNAME
CAMPUSNEST_DB_PASSWORD=DB_PASSWORD
CAMPUSNEST_ADMIN_EMAIL=your-admin-email
CAMPUSNEST_ADMIN_PASSWORD=use-a-unique-password-at-least-12-characters
CAMPUSNEST_UPLOAD_DIR=/tmp/campusnest-owner-proofs
```

Vercel supplies `PORT`; the image passes it to Tomcat. Do not add `PORT=8080` in Vercel.

## Verify

Open the deployment URL, then open `/api/health`. A ready app returns `{"status":"ok","database":true}`. Check the deployment logs if the result is an error or the database field is false.

## Important limits before real use

- `/tmp` is temporary. Owner ID proofs saved there can disappear when an instance is replaced. Do not collect real ID proofs until uploads use durable private object storage such as an appropriately configured S3-compatible bucket.
- Servlet login sessions are currently held in Tomcat memory. Vercel may route requests to different or newly started instances, so sign-in/session behavior needs verification and durable/shared session storage before production use.
- Test listing search, registration, login, owner/admin actions, and inquiry submission on the deployed URL using non-sensitive data.

Vercel's current container deployment documentation: https://vercel.com/docs/functions/runtimes/container
