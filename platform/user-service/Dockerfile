# ---- Build Stage ----
FROM maven:3.9-eclipse-temurin-21 AS build

# 复制整个项目（Maven 需要父 pom）
WORKDIR /project
COPY . .

# 在模块目录下构建
WORKDIR /project/platform/user-service
RUN mvn clean package -DskipTests -B

# ---- Runtime Stage ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/platform/user-service/target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
