# notes-project

This project was created using the [Ktor Project Generator](https://start.ktor.io).

Here are some useful links to get you started:

- [Ktor Documentation](https://ktor.io/docs/home.html)
- [Ktor GitHub page](https://github.com/ktorio/ktor)
- The [Ktor Slack chat](https://app.slack.com/client/T09229ZC6/C0A974TJ9). You'll need
  to [request an invite](https://surveys.jetbrains.com/s3/kotlin-slack-sign-up) to join.

## Environment

- Copy `.env.example` to `.env` and adjust values as needed.
- `.env` is used by docker-compose (use `DB_HOST=postgres`).
- `.env.local` is optional and has priority over `.env` for local launch (use `DB_HOST=localhost`).
- `.env` and `.env.local` are ignored by git; keep secrets local.

## Git hooks (Lefthook)

1) Install Lefthook (needed on each developer machine): https://lefthook.dev/installation/index.html
2) Enable hooks: `lefthook install`
2) Hooks:
   - `pre-commit`: `./gradlew ktlintCheck`, `./gradlew detekt`
   - `pre-push`: `./gradlew test`

## Features

Here's a list of features included in this project:

| Name                                                               | Description                                                                        |
|--------------------------------------------------------------------|------------------------------------------------------------------------------------|
| [Routing](https://start.ktor.io/p/routing)                         | Provides a structured routing DSL                                                  |
| [Content Negotiation](https://start.ktor.io/p/content-negotiation) | Provides automatic content conversion according to Content-Type and Accept headers |
| [Status Pages](https://start.ktor.io/p/status-pages)               | Provides exception handling for routes                                             |
| [Call Logging](https://start.ktor.io/p/call-logging)               | Logs client requests                                                               |
| [OpenAPI](https://start.ktor.io/p/openapi)                         | Serves OpenAPI documentation                                                       |
| [Swagger](https://start.ktor.io/p/swagger)                         | Serves Swagger UI for your project                                                 |

## Building & Running

To build or run the project, use one of the following tasks:

| Task                                    | Description                                                          |
|-----------------------------------------|----------------------------------------------------------------------|
| `./gradlew test`                        | Run the tests                                                        |
| `./gradlew build`                       | Build everything                                                     |
| `./gradlew buildFatJar`                 | Build an executable JAR of the server with all dependencies included |
| `./gradlew buildImage`                  | Build the docker image to use with the fat JAR                       |
| `./gradlew publishImageToLocalRegistry` | Publish the docker image locally                                     |
| `./gradlew run`                         | Run the server                                                       |
| `./gradlew runDocker`                   | Run using the local docker image                                     |

If the server starts successfully, you'll see the following output:

```
2024-12-04 14:32:45.584 [main] INFO  Application - Application started in 0.303 seconds.
2024-12-04 14:32:45.682 [main] INFO  Application - Responding at http://0.0.0.0:8080
```
