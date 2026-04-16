# 使用预装 Maven 和 JDK21 的镜像
FROM maven:3.9-amazoncorretto-21 AS builder

WORKDIR /app

# 复制 pom.xml 并下载依赖（利用 Docker 缓存层）
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 复制源代码
COPY src ./src

# 执行打包，跳过测试
RUN mvn clean package -DskipTests -B

# 运行时阶段：使用更轻量的 JRE 镜像
FROM amazoncorretto:21-alpine

WORKDIR /app

# 从构建阶段复制 jar 包
COPY --from=builder /app/target/yuluo-ai-agent-0.0.1-SNAPSHOT.jar app.jar

# 创建临时目录（应用需要）
RUN mkdir -p /app/tmp/file /app/tmp/download

# 暴露应用端口
EXPOSE 8123

# 设置环境变量（可根据需要覆盖）
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC"

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --spring.profiles.active=${SPRING_PROFILES_ACTIVE}"]
