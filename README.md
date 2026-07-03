# SubsTrack
> A modern Subscription Tracker API designed to help users manage recurring expenses, trial periods, and all subscriptions. Currently in active development.

## Tech Stack
* **Core:** Java 25, Spring Boot 4.x
* **Database:** PostgreSQL, Spring Data JPA
* **Migrations:** Liquibase
* **Mapping & Boilerplate:** MapStruct, Lombok
* **Caching:** Spring Cache with Caffeine
* **Testing:** JUnit 5, Testcontainers

## Architecture Note
This project follows a strict **Modular Monolith (Package by Feature)** architecture combined with **CQS (Command-Query Separation)**. 

Each domain (e.g., `subscription`) is split into:
* `api/` - Public contracts, DTOs, and Facades. This is the only part accessible to other modules.
* `internal/` - Package-private implementations (Entities, Repositories, Services and Controllers).

## Running Locally (Development)

### 1. Prerequisites
* JDK 25
* Docker & Docker Compose
* Maven

### 2. Start the Database
The project relies on a Dockerized PostgreSQL instance. Spin it up using:
```bash
docker-compose up -d
```

### 3. Run the Application
Start the Spring Boot application. Liquibase will automatically create the database schema on the first run.

```bash
mvn spring-boot:run
```

### 4. API Documentation
Once the application is running, the interactive OpenAPI (Swagger) documentation is available at:
http://localhost:8080/swagger-ui.html