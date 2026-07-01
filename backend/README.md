# Social Messaging Backend

Spring Boot 3.x backend for Android social messaging application with WebSocket support.

## Stack
- **Framework**: Spring Boot 3.3.5
- **Java**: 17+
- **Build Tool**: Maven
- **Database**: MySQL 8.x
- **Authentication**: JWT (jjwt 0.12.3)
- **Real-time**: WebSocket + STOMP over SockJS
- **ORM**: Spring Data JPA + Hibernate

## Project Structure
```
backend/
├── src/main/java/com/yourapp/
│   ├── config/           # Spring configuration beans
│   ├── controller/       # REST API endpoints
│   ├── service/          # Business logic
│   ├── repository/       # JPA repositories
│   ├── entity/           # JPA entities
│   ├── dto/              # Request/Response DTOs
│   ├── security/         # JWT and authentication
│   ├── websocket/        # WebSocket handlers
│   ├── exception/        # Global exception handling
│   └── Application.java  # Main application class
├── src/main/resources/
│   └── application.yml   # Application configuration
├── pom.xml               # Maven dependencies
├── .env.example          # Environment variable template
└── .gitignore
```

## Setup Instructions

### 1. Prerequisites
- Java 17 or higher
- Maven 3.6+
- MySQL 8.x running locally or remote

### 2. Environment Setup
```bash
# Copy environment template
cp .env.example .env

# Edit .env with your local values
# DB_URL=jdbc:mysql://localhost:3306/social_app
# DB_USERNAME=root
# DB_PASSWORD=yourpassword
# JWT_SECRET=your-secret-key-minimum-32-chars
```

### 3. Database Setup
```bash
# Create database (MySQL)
mysql -u root -p
CREATE DATABASE social_app CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
EXIT;
```

### 4. Build and Run
```bash
# Build
mvn clean install

# Run locally
mvn spring-boot:run
```

Server runs on `http://localhost:8080/api`

## Dependencies

| Dependency | Version | Purpose |
|---|---|---|
| spring-boot-starter-web | 3.3.5 | REST API |
| spring-boot-starter-data-jpa | 3.3.5 | Database ORM |
| spring-boot-starter-security | 3.3.5 | Authentication |
| spring-boot-starter-websocket | 3.3.5 | Real-time WebSocket |
| spring-boot-starter-validation | 3.3.5 | Input validation |
| mysql-connector-j | Latest | MySQL driver |
| jjwt | 0.12.3 | JWT authentication |
| lombok | Latest | Boilerplate reduction |

## Configuration

### application.yml
- Server port: 8080
- Context path: /api
- Datasource: Configured via environment variables
- JPA DDL: validate (requires schema pre-creation)
- JWT expiration: 15 min (access) / 7 days (refresh)

### Security
- Passwords must be hashed with BCrypt
- JWT secret loaded from environment (minimum 32 characters)
- Never hardcode sensitive data in code

## Next Steps
1. Implement entity classes (User, Message, Conversation)
2. Create database migration/schema
3. Implement authentication (register, login, JWT refresh)
4. Implement REST controllers
5. Setup WebSocket handlers for real-time messaging

## Notes
- This is a skeleton project - no business logic implemented yet
- All sensitive configuration comes from environment variables
- Ready for incremental feature development
