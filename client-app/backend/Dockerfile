# ---- Build Stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# 复制父 pom 和当前模块 pom，利用 Docker 缓存依赖
COPY pom.xml .
COPY client-app/backend/pom.xml ./client-app/backend/pom.xml
RUN mvn dependency:go-offline -B -pl client-app/backend

# 复制源码并构建
COPY client-app/backend/src ./client-app/backend/src
RUN mvn clean package -DskipTests -B -pl client-app/backend

# ---- Runtime Stage ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/client-app/backend/target/*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]
