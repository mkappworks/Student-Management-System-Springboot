FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copy parent POM and all module POMs for dependency resolution
COPY pom.xml ./pom.xml
COPY service-registry/pom.xml ./service-registry/pom.xml
COPY api-gateway/pom.xml ./api-gateway/pom.xml
COPY auth-service/pom.xml ./auth-service/pom.xml
COPY student-service/pom.xml ./student-service/pom.xml
COPY teacher-service/pom.xml ./teacher-service/pom.xml
COPY module-service/pom.xml ./module-service/pom.xml
COPY grade-service/pom.xml ./grade-service/pom.xml
COPY enrollment-service/pom.xml ./enrollment-service/pom.xml
COPY notification-service/pom.xml ./notification-service/pom.xml

# Install maven and cache dependencies
RUN apk add --no-cache maven && mvn dependency:go-offline -pl enrollment-service -am -q

# Copy source and build
COPY enrollment-service/src ./enrollment-service/src
RUN mvn clean package -pl enrollment-service -am -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S sms && adduser -S sms -G sms
WORKDIR /app
COPY --from=builder /app/enrollment-service/target/*.jar app.jar
RUN chown sms:sms app.jar
USER sms
EXPOSE 8084
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3   CMD wget -q --spider http://localhost:8084/actuator/health || exit 1
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
