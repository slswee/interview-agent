# Use an official JDK runtime as a parent image
FROM eclipse-temurin:21-jdk-jammy

# Set the working directory to /app
WORKDIR /app

# Copy the build artifacts from the host to the container
COPY build/libs/intervew-agent-0.0.1.jar app.jar

# Expose the port the app runs on
EXPOSE 8080

# Run the jar file
ENTRYPOINT ["java", "-jar", "app.jar"]
