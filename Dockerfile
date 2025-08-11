# Use JDK 18 for building and running
FROM eclipse-temurin:18-jdk as builder

# Set working directory
WORKDIR /app

# Copy Gradle and project files
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts ./

# Copy source code
COPY src src

# Give gradlew permission to run
RUN chmod +x gradlew

# Build the shadow jar
RUN ./gradlew clean jar --no-build-cache

# Final image
FROM eclipse-temurin:18-jdk

WORKDIR /app

# Copy the jar from the builder stage
COPY --from=builder /app/build/libs/.jar app.jar

# Run the application
CMD ["java", "-jar", "app.jar"]
