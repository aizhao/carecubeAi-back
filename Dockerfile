# ============================================
# 后端 Dockerfile — 多阶段构建
# 阶段1: Maven 构建
# 阶段2: JRE 运行
# 构建上下文: carecubeai-api/
# ============================================

FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml .
COPY ruoyi-admin/pom.xml ruoyi-admin/
COPY ruoyi-common/pom.xml ruoyi-common/
COPY ruoyi-framework/pom.xml ruoyi-framework/
COPY ruoyi-system/pom.xml ruoyi-system/
COPY ruoyi-quartz/pom.xml ruoyi-quartz/
COPY ruoyi-generator/pom.xml ruoyi-generator/
RUN mvn dependency:go-offline -pl ruoyi-admin -am -q || true
COPY . .
RUN mvn clean package -DskipTests -pl ruoyi-admin -am -q

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /build/ruoyi-admin/target/ruoyi-admin.jar app.jar
COPY ry.sh ry.sh
RUN chmod +x ry.sh
EXPOSE 8081
ENTRYPOINT ["java", "-Xms512m", "-Xmx1024m", "-Duser.timezone=Asia/Shanghai", "-jar", "app.jar"]
