# Ordinator Backend

Ordinator is a project and task management application. This repository provides the REST API for user accounts, session authentication, projects, project membership, tasks, task assignments, and task comments with optional file attachments.

The Angular client lives in the separate `ordinator-ui` project. See its [README](https://github.com/Enelrith/ordinator-ui) for frontend setup.

## Stack

- Java 25 and Spring Boot 4.1.1
- Spring Web MVC, Spring Security, and Jakarta Validation
- Spring Data JPA with PostgreSQL
- Flyway for database migrations
- AWS SDK for Java v2 for S3 attachment storage, with LocalStack for local development
- springdoc OpenAPI for API documentation
- JUnit, Mockito, MockMvc, and Testcontainers for tests
- Maven Wrapper, pinned to Maven 3.9.16

## Requirements

- JDK 25, with `JAVA_HOME` pointing to the JDK installation
- Docker with Docker Compose for PostgreSQL, LocalStack, and integration tests
- A LocalStack auth token, available from your [LocalStack account](https://app.localstack.cloud/)
- Network access to download Maven dependencies and container images on first use

Run the commands below from this repository's root. On Windows PowerShell, use `./mvnw.cmd` in place of `./mvnw`. A separate Maven installation is unnecessary.

## Local setup

1. Set `LOCALSTACK_AUTH_TOKEN` in the shell used to run Docker Compose and Maven:

   ```sh
   export LOCALSTACK_AUTH_TOKEN="your-localstack-auth-token"
   ```

   Windows PowerShell:

   ```powershell
   $env:LOCALSTACK_AUTH_TOKEN = "your-localstack-auth-token"
   ```

2. Start PostgreSQL and LocalStack:

   ```sh
   docker compose up -d
   ```

3. Once LocalStack is ready, create the attachment bucket:

   ```sh
   docker compose exec localstack awslocal s3 mb s3://ordinator-attachments --region us-east-1
   ```

   Create the bucket once per LocalStack instance. The application uses an existing bucket; it does not create one at startup. See the [LocalStack S3 documentation](https://docs.localstack.cloud/aws/services/s3/) for bucket and object commands.

4. Start the API with the development profile:

   ```sh
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   ```

   Windows PowerShell:

   ```powershell
   .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
   ```

5. Start the frontend in the `ordinator-ui` project and open [http://localhost:4200](http://localhost:4200).

The API listens at [http://localhost:8080](http://localhost:8080). Flyway applies the migrations in `src/main/resources/db/migration` at startup, and Hibernate validates the schema.

The development database settings in `application-dev.yaml` match `compose.yaml`:

| Setting          | Development value |
| ---------------- | ----------------- |
| PostgreSQL image | `postgres:18.6`   |
| Host and port    | `localhost:5432`  |
| Database         | `ordinator`       |
| Username         | `user`            |
| Password         | `password`        |

The database persists in the `postgres_data` Docker volume. Stop the services with `docker compose down`; the volumes are retained.

LocalStack exposes S3 on port `4566`. Its current Compose configuration sets `PERSISTENCE=1`, so buckets and uploaded objects are retained after a restart in `localstack_data`

## Configuration

`src/main/resources/application.yaml` defines the base application settings. The `dev` profile adds local datasource settings, SQL logging, API documentation settings, and S3 connection settings in `application-dev.yaml`.

For another environment, supply datasource settings through a profile or environment variables:

| Variable                     | Purpose                                         |
| ---------------------------- | ----------------------------------------------- |
| `SPRING_PROFILES_ACTIVE`     | Select the active Spring profile, such as `dev` |
| `SPRING_DATASOURCE_URL`      | PostgreSQL JDBC URL                             |
| `SPRING_DATASOURCE_USERNAME` | Database username                               |
| `SPRING_DATASOURCE_PASSWORD` | Database password                               |
| `SERVER_PORT`                | Override the API port                           |

The S3 client uses region `us-east-1`. Configure these properties in every environment that runs the application:

| Property                   | Development value                            | Purpose                         |
| -------------------------- | -------------------------------------------- | ------------------------------- |
| `localstack.s3.endpoint`   | `https://s3.localhost.localstack.cloud:4566` | S3 endpoint used by the backend |
| `localstack.s3.accessKey`  | `test`                                       | S3 access key                   |
| `localstack.s3.secretKey`  | `test`                                       | S3 secret key                   |
| `localstack.s3.bucketName` | `ordinator-attachments`                      | Existing bucket for attachments |

The `test` S3 credentials are separate from `LOCALSTACK_AUTH_TOKEN`, which authenticates the LocalStack container. Multipart upload limits can be configured with `spring.servlet.multipart.max-file-size` and `spring.servlet.multipart.max-request-size`.

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
| `POST` | `/api/comments/tasks/{taskId}`                          | Create a comment with an optional attachment          |
| `GET`  | `/api/comments/projects/{projectId}/tasks/{taskId}`     | List a task's comments with pagination                |
| `GET`  | `/api/comments/{commentId}/attachment`                  | Download a comment's attachment                       |

Project creators become `ADMIN` members. Admins can add managers and members; managers can add members. Members cannot add project members or create tasks. Admins and managers can create tasks, and the task creator becomes its owner and initial task member. Only the task owner can assign additional members from the same project.

Adding a project member resolves an existing account by email, case-insensitively. It adds membership immediately.

API documentation is available at [Swagger UI](http://localhost:8080/swagger-ui/index.html) and [OpenAPI JSON](http://localhost:8080/v3/api-docs). The current security configuration requires an authenticated session to access these endpoints.

## Comments and attachments

Project members can read task comments. Creating comments and downloading attachments require membership in the task. Requests without the required membership return `404`.

Create comments using `multipart/form-data`, including when there is no attachment:

| Part             | Content type       | Required | Value                        |
| ---------------- | ------------------ | -------- | ---------------------------- |
| `commentRequest` | `application/json` | Yes      | `{"content":"Comment text"}` |
| `attachmentFile` | File content type  | No       | One file                     |

Comment content is trimmed and must contain 1–300 characters. A nonempty attachment must have a nonblank filename of at most 255 characters. An empty file is treated as no attachment. A successful request returns `201` with the created comment.

Each comment has at most one attachment. PostgreSQL stores the original filename and a generated object key; S3 stores the file bytes. Original filenames can repeat because each upload receives its own object key. The response includes `attachmentName`, which is `null` for comments without an attachment.

Comment listing accepts `page`, `size`, and `sort` query parameters. By default, it returns 10 comments per page, ordered by `createdAt` descending. Page numbers start at zero, for example `?page=1` for the second page. Responses contain `content` and a `page` object with `number`, `size`, `totalElements`, and `totalPages`.

The download endpoint returns file bytes as `application/octet-stream`, with `Content-Disposition: attachment` and the original filename. It returns `404` if the comment has no attachment.

If a comment transaction rolls back after an upload attempt, a transaction event listener attempts to delete the S3 object. Cleanup failures are logged; there is currently no retry queue or scheduled reconciliation. Comment editing, deletion, and attachment replacement are not exposed by the current API.

## Tests and builds

Run all tests:

```sh
./mvnw test
```

Integration tests start their own PostgreSQL and LocalStack containers using Testcontainers and require a running Docker daemon and `LOCALSTACK_AUTH_TOKEN` in the Maven process environment. They use `postgres:latest` and `localstack/localstack:latest`, independently of the Compose services. Test configuration supplies the dynamically mapped S3 endpoint and credentials; comment tests create their own attachment bucket.

Comment integration tests cover multipart requests, optional uploads, stored file contents, pagination, validation, authentication, and access restrictions.

Run a clean build with all tests and lifecycle checks:

```sh
./mvnw clean verify
```

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
  comment/      Task comments, attachment metadata, persistence, and DTOs
  s3/           S3 client configuration and upload/download operations
src/main/resources/
  application*.yaml     Application configuration
  db/migration/        Versioned Flyway SQL migrations
  messages.properties  Validation messages
src/test/java/         Unit and integration tests
compose.yaml           Local PostgreSQL and LocalStack services
```
