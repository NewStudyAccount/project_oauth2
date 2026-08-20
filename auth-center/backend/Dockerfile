# ---- Build Stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# 复制父 pom 和当前模块 pom，利用 Docker 缓存依赖
COPY pom.xml .
COPY auth-center/backend/pom.xml ./auth-center/backend/pom.xml
RUN mvn dependency:go-offline -B -pl auth-center/backend

# 复制源码并构建
COPY auth-center/backend/src ./auth-center/backend/src
RUN mvn clean package -DskipTests -B -pl auth-center/backend

# ---- Runtime Stage ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/auth-center/backend/target/*.jar app.jar

EXPOSE 9000

ENTRYPOINT ["java", "-jar", "app.jar"]
