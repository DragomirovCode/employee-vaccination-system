FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /workspace

COPY gradlew gradlew
COPY gradle gradle
COPY settings.gradle.kts build.gradle.kts ./
COPY app app
COPY auth auth
COPY employee employee
COPY vaccine vaccine
COPY vaccination vaccination
COPY reporting reporting
COPY audit audit

RUN chmod +x gradlew
RUN ./gradlew :app:bootJar --no-daemon

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY --from=builder /workspace/app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
