# Host CampusNest on Railway

Railway is the current recommended host for this project because the app needs Java 17, Tomcat 10.1, and MySQL. The repository now has a root `Dockerfile`; Railway builds the WAR and serves it at `/` from Tomcat's `ROOT.war`.

## 1. Connect the GitHub repository

1. Sign in at [Railway](https://railway.com/) and create a project.
2. Add a service from GitHub and select `sharuuuuuhh/campusnext`, branch `main`.
3. Railway should detect the root `Dockerfile`. Confirm the build configuration uses the repository root.
4. In the web service variables, set `PORT=8080`. The Tomcat connector listens on this port.

## 2. Add MySQL

1. In the same Railway project and environment, add a MySQL database service. Keep it private. The Java service will connect using Railway's internal network.
2. Use the Railway CLI from the repository root to initialize the schema without exposing MySQL publicly:

   ```text
   railway link
   railway connect MySQL
   ```

   If your database service has a different name, select it when prompted. In the MySQL prompt, run:

   ```sql
   CREATE DATABASE IF NOT EXISTS campusnest CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
   USE campusnest;
   SOURCE database/schema.sql;
   SOURCE database/seed.sql;
   ```

   The Railway MySQL account must be allowed to create and use `campusnest`. If it is limited to its pre-created database, use that database name instead by making the same name consistent in `schema.sql`, `seed.sql`, and the JDBC URL before deployment.

## 3. Connect the web service to MySQL

Open the web service's Variables tab and add these variables. If the MySQL service is not named `MySQL`, replace `MySQL` in each reference with its exact Railway service name.

```text
CAMPUSNEST_DB_URL=jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/campusnest?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC
CAMPUSNEST_DB_USER=${{MySQL.MYSQLUSER}}
CAMPUSNEST_DB_PASSWORD=${{MySQL.MYSQLPASSWORD}}
CAMPUSNEST_UPLOAD_DIR=/data/owner-proofs
CAMPUSNEST_ADMIN_EMAIL=your-admin-email
CAMPUSNEST_ADMIN_PASSWORD=choose-a-unique-password-of-at-least-12-characters
PORT=8080
```

Keep the admin password in Railway Variables only. Do not commit it or paste it into chat. Railway creates the admin account at application startup when both admin variables are present.

## 4. Persist private ID proof uploads

In the web service, add a Railway Volume mounted at `/data`. The upload directory is outside the public web root; the admin proof endpoint checks the admin session before serving a file. Without this volume, uploaded proofs can disappear on redeploy. Limit use to test documents until the Privacy Policy's retention and deletion details have been reviewed.

## 5. Deploy and check

1. In web service Settings, add a public domain.
2. Configure its Healthcheck Path as `/api/health`.
3. Deploy, then open the Railway domain. The `/api/health` response should report `{"status":"ok","database":true}`.
4. Sign in with the admin email and password from Variables. Test listing search and the owner registration/review flow with non-sensitive test data.
5. Turn on MySQL backups before adding real users or listings.

Railway terminates HTTPS before forwarding requests. This Docker image marks its Tomcat connector as secure; the session cookie stays HttpOnly and Tomcat marks it Secure. Local development remains on HTTP.

## Cost and limits

Railway currently lists a Free plan with $1/month of included resource credit and a Hobby plan at $5/month with $5 of included usage. Extra use may cost more. The free tier also has small per-service memory and volume limits, so check the current [pricing](https://railway.com/pricing) and workspace usage before running continuously or storing real ID proofs. Railway supports a MySQL service and private service networking; see the [MySQL guide](https://docs.railway.com/databases/mysql), [Dockerfile guide](https://docs.railway.com/builds/dockerfiles), and [healthcheck guide](https://docs.railway.com/deployments/healthchecks).
