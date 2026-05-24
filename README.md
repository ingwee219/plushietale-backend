# Plushie Tale — Backend

REST API server for **Plushie Tale**, an AI-powered web app that generates personalised bedtime stories based on a child's plushie.

**Live demo:** http://56.228.67.183  
**Frontend repo:** https://github.com/ingwee219/plushietale-frontend

---

## Features

- **AI story generation** — Sends plushie images and metadata to the Gemini API; returns a unique, age-appropriate story
- **JWT authentication** — Stateless auth with access tokens; supports both local sign-up and Google OAuth2
- **Image storage** — Plushie photos uploaded to AWS S3
- **Community board** — Posts, comments, and likes with per-user constraints
- **RESTful API** — Documented with Swagger UI (`/swagger-ui.html`)

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5 |
| Database | MySQL 8 |
| ORM | Spring Data JPA (Hibernate) |
| Security | Spring Security + JWT + Google OAuth2 |
| AI | Google Gemini API (gemini-2.5-flash) |
| Storage | AWS S3 |
| Infra | Docker, AWS EC2 (eu-north-1) |

## Project Structure

```
src/main/java/com/storyapp/
├── user/        # Registration, profile, authentication
├── toy/         # Plushie CRUD + S3 image upload
├── story/       # AI story generation and management
├── post/        # Community board with likes
├── comment/     # Comment CRUD
├── auth/        # JWT, OAuth2, Security config
├── ai/          # Gemini API integration
├── storage/     # AWS S3 service
└── global/      # Exception handling, common response wrapper
```

## Local Setup

### Prerequisites
- Java 21
- Docker Desktop
- An `application-secret.yml` with your own API keys (see below)

### Required secrets (`src/main/resources/application-secret.yml`)
```yaml
spring:
  datasource:
    password: your-db-password
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: your-google-client-id
            client-secret: your-google-client-secret

jwt:
  secret: your-jwt-secret

gemini:
  api-key: your-gemini-api-key

aws:
  access-key: your-aws-access-key
  secret-key: your-aws-secret-key
  s3:
    bucket: your-s3-bucket
    region: your-region
```

### Run

```bash
# Start MySQL via Docker
docker compose up -d

# Run the application
./mvnw spring-boot:run
```

API runs on `http://localhost:8080`
