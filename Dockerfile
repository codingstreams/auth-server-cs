# ==========================================
# Stage 1: Build the application with Maven
# ==========================================
FROM eclipse-temurin:25-jdk AS builder

WORKDIR /build

# Copy Maven wrapper and POM first for layer caching
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Ensure maven wrapper is executable and download dependencies
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy project source code
COPY src/ src/

# Build executable fat jar (skipping unit/integration tests during image build)
RUN ./mvnw clean package -DskipTests -B

# ==========================================
# Stage 2: Minimal Distroless/Chiseled Runtime
# ==========================================
FROM eclipse-temurin:25-jdk-chiseled

WORKDIR /app

# Copy the built jar artifact from builder stage
COPY --from=builder /build/target/*.jar app.jar

# Exec format entrypoint for chiseled container
ENTRYPOINT ["java", "-jar", "app.jar"]
