# ---- Build Stage ----
FROM maven:3.9-eclipse-temurin-21 AS build

# 配置阿里云 Maven 镜像（解决国内 SSL 证书问题）
RUN mkdir -p /root/.m2 && \
    echo '<settings><mirrors><mirror><id>aliyun</id><mirrorOf>central</mirrorOf><name>Aliyun Maven</name><url>https://maven.aliyun.com/repository/public</url></mirror></mirrors></settings>' > /root/.m2/settings.xml

# 复制整个项目（Maven 需要父 pom）
WORKDIR /project
COPY . .

# 在模块目录下构建
WORKDIR /project/platform/resource-api
RUN mvn clean package -DskipTests -B

# ---- Runtime Stage ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /project/platform/resource-api/target/*.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]
