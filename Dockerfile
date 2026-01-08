FROM gradle:8.7-jdk17 AS build
WORKDIR /app
COPY . .
RUN ./gradlew --no-daemon installDist

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/install/notes-project /app/notes-project
EXPOSE 8080
ENTRYPOINT ["/app/notes-project/bin/notes-project"]
