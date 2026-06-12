FROM maven:3.6-jdk-8 AS build
WORKDIR /app

# Copy parent POM and all module POMs first for dependency caching
COPY pom.xml .
COPY common/pom.xml common/
COPY userfront-service/pom.xml userfront-service/
COPY admin-service/pom.xml admin-service/

# Download dependencies (cached unless POMs change)
RUN mvn dependency:go-offline -pl common,userfront-service -am -B || true

# Copy source code
COPY common/src common/src
COPY userfront-service/src userfront-service/src

# Build the common module and userfront-service
RUN mvn clean package -pl common,userfront-service -am -DskipTests -B

# --- Runtime stage ---
FROM openjdk:8-jre-slim
WORKDIR /app
COPY --from=build /app/userfront-service/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
