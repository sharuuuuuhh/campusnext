FROM maven:3.9.16-eclipse-temurin-17-noble AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM tomcat:10.1.61-jdk17-temurin-noble
# Railway terminates HTTPS at its edge. Mark the Tomcat connector secure so
# servlet sessions receive Secure cookies when the browser uses HTTPS.
RUN sed -i 's/<Connector port="8080" protocol="HTTP\/1.1"/<Connector port="8080" protocol="HTTP\/1.1" scheme="https" secure="true"/' /usr/local/tomcat/conf/server.xml
COPY --from=build /workspace/target/campusnest.war /usr/local/tomcat/webapps/ROOT.war
EXPOSE 8080
