FROM gradle:8.7-jdk17 AS build
WORKDIR /app
COPY . .
RUN ./gradlew --no-daemon buildFatJar

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/libs/*-all.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
