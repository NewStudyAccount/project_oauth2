# ---- Build Stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# 复制父 pom 和当前模块 pom，利用 Docker 缓存依赖
COPY pom.xml .
COPY platform/user-service/pom.xml ./platform/user-service/pom.xml
RUN mvn dependency:go-offline -B -pl user-service

# 复制源码并构建
COPY platform/user-service/src ./platform/user-service/src
RUN mvn clean package -DskipTests -B -pl user-service

# ---- Runtime Stage ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/platform/user-service/target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
