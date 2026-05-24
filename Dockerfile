FROM amazoncorretto:21
WORKDIR /app
COPY app.jar app.jar
ENTRYPOINT ["java", "-Xmx400m", "-jar", "app.jar", "--spring.config.additional-location=/app/config/application-secret.yml"]
