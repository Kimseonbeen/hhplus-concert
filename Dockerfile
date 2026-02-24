# 빌드 스테이지
FROM eclipse-temurin:17-jdk-jammy AS builder
RUN apt-get update && apt-get install -y git
WORKDIR /app
COPY . .
RUN ./gradlew clean build -x test

# 실행 스테이지
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app
COPY --from=builder /app/build/libs/*[0-9].jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
