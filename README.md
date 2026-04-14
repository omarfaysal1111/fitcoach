# FitCoach Pro — Backend API

A smart fitness coaching backend system delivering personalized workout and
nutrition plans via a clean, scalable REST API.

Built as the backend engine for Guider — an intelligent guidance platform.

## Architecture
Client (Flutter / Mobile)
│
▼
REST API Layer (Spring Boot)
│
┌────┴────┐
Service Layer  Auth Layer (JWT)
│
Repository Layer (JPA / Hibernate)
│
PostgreSQL Database
│
Docker Container

## Tech Stack

- **Framework:** Java · Spring Boot · Spring Security
- **Architecture:** Clean Architecture · RESTful API · Microservice-ready
- **Database:** PostgreSQL · Hibernate / JPA
- **Auth:** JWT-based authentication & role-based access control
- **DevOps:** Docker · GitHub Actions CI/CD

## Key Features

- Personalized workout plan generation engine
- Nutrition tracking and recommendation system
- JWT authentication with role-based access (User / Coach / Admin)
- RESTful API designed for Flutter mobile client consumption
- Dockerized for portable deployment

## Running Locally

```bash
# Clone the repo
git clone https://github.com/omarfaysal1111/fitcoach-pro-backend.git

# Configure DB in application.properties
spring.datasource.url=jdbc:postgresql://localhost:5432/fitcoach
spring.datasource.username=your_user
spring.datasource.password=your_password

# Run with Docker
docker-compose up --build

# Or run directly
./mvnw spring-boot:run
```

## Related

- [Guider Platform](https://github.com/omarfaysal1111) — the parent intelligent guidance system
