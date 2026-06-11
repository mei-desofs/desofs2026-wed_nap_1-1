# ==========================================================
# BUILD STAGE
# Responsible for compiling the Spring Boot application
# ==========================================================
FROM maven:3.9-eclipse-temurin-21 AS build

# Set the working directory inside the container
WORKDIR /app

# Copy the pom.xml
COPY pom.xml .

# Download project dependencies
RUN mvn dependency:go-offline

# Copy the application source code
COPY src ./src

# Compile the application and generate the JAR file.
RUN mvn clean package -DskipTests

# ==========================================================
# RUNTIME STAGE
# ==========================================================
FROM eclipse-temurin:21-jre

# Create a dedicated system group and user.
RUN addgroup --system emovieshop && \
    adduser --system --ingroup emovieshop emovieshop

# Set the working directory for the application
WORKDIR /app

# Copy the generated JAR file from the build stage.
COPY --from=build /app/target/*.jar app.jar

# Run the application using a non-privileged user
USER emovieshop

# Expose the port used by the REST API
EXPOSE 8080

# Command executed when the container starts
ENTRYPOINT ["java", "-jar", "app.jar"]
