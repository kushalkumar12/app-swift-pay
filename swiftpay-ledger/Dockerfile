# ─────────────────────────────────────────────
# Stage 1 — Build
# ─────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper and pom first (layer-cache dependencies)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

RUN chmod +x mvnw && ./mvnw dependency:go-offline -q

# Copy source and build
COPY src/ src/

RUN ./mvnw clean package -DskipTests -q

# ─────────────────────────────────────────────
# Stage 2 — Runtime
# ─────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

# Non-root user for security
RUN addgroup -S swiftpay && adduser -S swiftpay -G swiftpay
USER swiftpay

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8882

ENTRYPOINT ["java", "-jar", "app.jar"]
