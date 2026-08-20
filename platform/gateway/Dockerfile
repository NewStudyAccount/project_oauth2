# ---- Build Stage ----
FROM maven:3.9-eclipse-temurin-21 AS build

# 复制整个项目（Maven 需要父 pom）
WORKDIR /project
COPY . .

# 在模块目录下构建
WORKDIR /project/platform/gateway
RUN mvn clean package -DskipTests -B

# ---- Runtime Stage ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/platform/gateway/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
