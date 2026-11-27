# Build Spring Boot application with Maven, then run it on Java 21.
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN for attempt in 1 2 3 4 5; do \
    mvn -U -B clean package -DskipTests && exit 0; \
    echo "Maven build failed. Retrying after ${attempt}0 seconds..."; \
    sleep $((attempt * 10)); \
done; \
exit 1

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/backend-0.1.0-SNAPSHOT.jar app.jar
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
