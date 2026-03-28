# Stage 1: Builder - Use a JDK image to build the application
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /app

# Copy Gradle wrapper and build configuration files
COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle.kts .
COPY settings.gradle.kts .
# The pom.xml line has been removed.

# Copy source code
COPY src/ src/

# Make gradlew executable and run the build.
# '-x test' skips tests during the build to speed it up for containerization.
# '--no-daemon' is often recommended for CI environments.
RUN ./gradlew build -x test --no-daemon

# Stage 2: Runner - Use a minimal JRE image for the final artifact
FROM eclipse-temurin:21-jre-jammy AS runner

WORKDIR /app

# Copy the built JAR from the builder stage into the runner image
# Ensure the artifact name matches your build output (assuming 'intervew-agent-0.0.1.jar')
COPY --from=builder /app/build/libs/intervew-agent-0.0.1.jar app.jar

# Expose the port the app runs on
EXPOSE 8080

# Define the command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
