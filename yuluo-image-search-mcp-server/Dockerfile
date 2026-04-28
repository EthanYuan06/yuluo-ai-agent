FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY yuluo-image-search-mcp-server-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8127

ENTRYPOINT ["java", "-jar", "app.jar"]
