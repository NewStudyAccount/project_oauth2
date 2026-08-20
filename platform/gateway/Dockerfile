# ---- Build Stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# 复制父 pom 和当前模块 pom，利用 Docker 缓存依赖
COPY pom.xml .
COPY platform/gateway/pom.xml ./platform/gateway/pom.xml
RUN mvn dependency:go-offline -B -pl platform/gateway

# 复制源码并构建
COPY platform/gateway/src ./platform/gateway/src
RUN mvn clean package -DskipTests -B -pl platform/gateway

# ---- Runtime Stage ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/platform/gateway/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
