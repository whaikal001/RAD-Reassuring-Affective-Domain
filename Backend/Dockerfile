# ===== Build Stage =====
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /app

# Copy Maven wrapper and pom.xml first for better caching
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

# Make mvnw executable
RUN chmod +x mvnw

# Download dependencies (cached layer)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN ./mvnw clean package -DskipTests -B

# ===== Runtime Stage =====
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Create non-root user
RUN groupadd -r radai && useradd -r -g radai radai

# Copy JAR from builder
COPY --from=builder /app/target/radai-*.jar app.jar

# Set ownership
RUN chown -R radai:radai /app

USER radai

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --spider --quiet http://localhost:8080/api/auth/health || exit 1

# Run the app with optimized JVM settings
ENTRYPOINT ["java", "-Djava.net.preferIPv4Stack=true", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
