# Use Java 21 as base
FROM eclipse-temurin:21-jdk-jammy

WORKDIR /app

# Copy pom.xml and mvnw
COPY Backend/pom.xml .
COPY Backend/mvnw .
COPY Backend/.mvn .mvn

# Build the application
RUN ./mvnw -B -DskipTests clean package

# Copy the JAR from target
RUN cp target/radai-*.jar app.jar

# Expose port (default 8080, override with PORT env var)
EXPOSE 8080

# Run the app
ENTRYPOINT ["java", "-jar", "app.jar"]
