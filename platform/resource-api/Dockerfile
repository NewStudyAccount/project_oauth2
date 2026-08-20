# ---- Build Stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# 复制父 pom 和当前模块 pom，利用 Docker 缓存依赖
COPY pom.xml .
COPY platform/resource-api/pom.xml ./platform/resource-api/pom.xml
RUN mvn dependency:go-offline -B -pl platform/resource-api

# 复制源码并构建
COPY platform/resource-api/src ./platform/resource-api/src
RUN mvn clean package -DskipTests -B -pl platform/resource-api

# ---- Runtime Stage ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/platform/resource-api/target/*.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]
