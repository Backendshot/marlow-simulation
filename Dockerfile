# Use JDK 18 for building and running
FROM eclipse-temurin:18-jdk as builder

WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts ./
COPY src src

RUN chmod +x gradlew

# Build the shadow jar (fat jar)
RUN ./gradlew clean shadowJar --no-build-cache

# Final image
FROM eclipse-temurin:18-jdk
WORKDIR /app

# Copy the fat jar from builder stage
COPY --from=builder /app/build/libs/*-all.jar app.jar

CMD ["java", "-jar", "app.jar"]

#Copy the env file
COPY .env .env
