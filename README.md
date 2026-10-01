# Ordinator Backend

Ordinator is a project and task management application. This repository provides the REST API for user accounts, session authentication, projects, project membership, tasks, and task assignments.

The Angular client lives in the separate `ordinator-ui` project. See its [README](../ordinator-ui/README.md) for frontend setup.

## Stack

- Java 25 and Spring Boot 4.1.1
- Spring Web MVC, Spring Security, and Jakarta Validation
- Spring Data JPA with PostgreSQL
- Flyway for database migrations
- springdoc OpenAPI for API documentation
- JUnit, Mockito, MockMvc, and Testcontainers for tests
- Maven Wrapper, pinned to Maven 3.9.16

## Requirements

- JDK 25, with `JAVA_HOME` pointing to the JDK installation
- Docker with Docker Compose for the local database and integration tests
- Network access to download Maven dependencies and PostgreSQL container images on first use

Run the commands below from this repository's root. On Windows PowerShell, use `./mvnw.cmd` in place of `./mvnw`. A separate Maven installation is unnecessary.

## Local setup

1. Start PostgreSQL:

   ```sh
   docker compose up -d db
   ```

2. Start the API with the development profile:

   ```sh
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   ```

   Windows PowerShell:

   ```powershell
   .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
   ```

3. Start the frontend in the `ordinator-ui` project and open [http://localhost:4200](http://localhost:4200).

The API listens at [http://localhost:8080](http://localhost:8080). Flyway applies the migrations in `src/main/resources/db/migration` at startup, and Hibernate validates the schema.

The development database settings in `application-dev.yaml` match `compose.yaml`:

| Setting          | Development value |
| ---------------- | ----------------- |
| PostgreSQL image | `postgres:18.6`   |
| Host and port    | `localhost:5432`  |
| Database         | `ordinator`       |
| Username         | `user`            |
| Password         | `password`        |

The database persists in the `postgres_data` Docker volume. Stop the database with `docker compose down`; the volume is retained.

## Configuration

`src/main/resources/application.yaml` defines the base application settings. The `dev` profile adds local datasource settings, SQL logging, and API documentation settings in `application-dev.yaml`.

For another environment, supply datasource settings through a profile or environment variables:

| Variable                     | Purpose                                         |
| ---------------------------- | ----------------------------------------------- |
| `SPRING_PROFILES_ACTIVE`     | Select the active Spring profile, such as `dev` |
| `SPRING_DATASOURCE_URL`      | PostgreSQL JDBC URL                             |
| `SPRING_DATASOURCE_USERNAME` | Database username                               |
| `SPRING_DATASOURCE_PASSWORD` | Database password                               |
| `SERVER_PORT`                | Override the API port                           |

## Authentication

Authentication uses an HTTP session and the `JSESSIONID` cookie. Passwords are hashed with Argon2.

- Fetch a CSRF token with `GET /api/auth/csrf` before sending a state-changing request. Use the `XSRF-TOKEN` cookie value in the `X-XSRF-TOKEN` request header.
- Register with `POST /api/users`, sending a JSON body with `email`, `rawPassword`, `firstName`, and `lastName`.
- Log in with `POST /api/auth/login`, sending form fields named `email` and `password`. Successful login returns `204`.
- Fetch the current user with `GET /api/auth/me`.
- Log out with `POST /api/auth/logout`. Successful logout returns `204` and invalidates the session.

Registration, login, and CSRF token retrieval are available without an authenticated session. State-changing requests still require CSRF protection. Refresh the CSRF token after login or logout when making requests through a custom client.

## API overview

| Method | Path                                                    | Purpose                                               |
| ------ | ------------------------------------------------------- | ----------------------------------------------------- |
| `POST` | `/api/users`                                            | Create a user account                                 |
| `GET`  | `/api/auth/csrf`                                        | Obtain a CSRF token                                   |
| `POST` | `/api/auth/login`                                       | Log in                                                |
| `POST` | `/api/auth/logout`                                      | Log out                                               |
| `GET`  | `/api/auth/me`                                          | Get the current user                                  |
| `POST` | `/api/projects`                                         | Create a project                                      |
| `GET`  | `/api/projects/info`                                    | List summaries of the current user's projects         |
| `GET`  | `/api/projects/{projectId}`                             | Get a project and its members                         |
| `POST` | `/api/projects/{projectId}/users/{inviteeEmail}`        | Add an existing user by email                         |
| `GET`  | `/api/projects/{projectId}/project-members`             | List members of a project the current user belongs to |
| `POST` | `/api/tasks/projects/{projectId}`                       | Create a task in a project                            |
| `GET`  | `/api/tasks/projects/{projectId}/info`                  | List a project's task summaries                       |
| `GET`  | `/api/tasks/{taskId}/projects/{projectId}`              | Get task details and members                          |
| `POST` | `/api/tasks/{taskId}/project-members/{projectMemberId}` | Assign a project member to a task                     |

Project creators become `ADMIN` members. Admins can add managers and members; managers can add members. Members cannot add project members or create tasks. Admins and managers can create tasks, and the task creator becomes its owner and initial task member. Only the task owner can assign additional members from the same project.

Adding a project member resolves an existing account by email, case-insensitively. It adds membership immediately.

API documentation is available at [Swagger UI](http://localhost:8080/swagger-ui/index.html) and [OpenAPI JSON](http://localhost:8080/v3/api-docs). The current security configuration requires an authenticated session to access these endpoints.

## Tests and builds

Run all tests:

```sh
./mvnw test
```

Integration tests start their own PostgreSQL container using Testcontainers and require a running Docker daemon. They use `postgres:latest`, independently of the Compose database.

Run service unit tests without starting integration-test containers:

```sh
./mvnw "-Dtest=*ServiceTest" test
```

Build the executable application, including tests:

```sh
./mvnw package
```

Run the packaged application with the local development database:

```sh
java -jar target/ordinator-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

## Project layout

```text
src/main/java/com/enelrith/ordinator/
  common/       Shared entities, exception handling, and OpenAPI configuration
  security/     Authentication and security configuration
  user/         User accounts, persistence, and DTOs
  project/      Projects, membership, roles, persistence, and DTOs
  task/         Tasks, assignments, persistence, and DTOs
src/main/resources/
  application*.yaml     Application configuration
  db/migration/        Versioned Flyway SQL migrations
  messages.properties  Validation messages
src/test/java/         Unit and integration tests
compose.yaml           Local PostgreSQL service
```
