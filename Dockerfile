# ── Stage 1: Build ───────────────────────────────────────────────────────────
FROM maven:3.9.12-eclipse-temurin-21 AS build

WORKDIR /app

# Cache dependencies first (only re-downloads when pom.xml changes)
COPY pom.xml .
RUN mvn dependency:go-offline -B --no-transfer-progress

# Copy source and build fat JAR
COPY src ./src
RUN mvn clean package -DskipTests -B --no-transfer-progress

# ── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=build /app/target/task-planner-1.0.0.jar app.jar

EXPOSE 8080

# Africa/Harare = CAT (UTC+2, no DST) — matches application.properties
ENV TZ=Africa/Harare

ENTRYPOINT ["java", "-jar", "app.jar"]
