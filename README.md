Hello, World! My name is Ricardo Ji.

# Quiz Management System

A comprehensive quiz management system built with Spring Boot and JavaScript, allowing users to create, manage, and take quizzes with support for mathematical expressions.

## Features

- User authentication with JWT
- Quiz creation and management
- Multiple question types (standard, multiple choice, with subsections)
- Mathematical expression rendering using MathJax
- Quiz grading and result tracking
- Topic-based organization of questions

## Tech Stack

### Backend
- Java 17
- Spring Boot 3.2.3
- Spring Security
- Spring Data JPA
- PostgreSQL
- JWT Authentication

### Frontend
- HTML5, CSS3, JavaScript
- MathJax for math rendering
- Responsive design

## Setup and Installation

1. Clone the repository
2. Set up PostgreSQL database
3. Update database credentials in `application.properties`
4. Run `mvn spring-boot:run`
5. Open `http://localhost:8080`

## Project Structure

```
src/main/java/com/example/app/
├── controller/     # REST API endpoints
├── service/        # Business logic
├── repository/     # Data access
├── model/          # Entity classes
├── security/       # Authentication
└── dto/            # Data transfer objects
```

## API Endpoints

- `POST /api/auth/login` - User login
- `POST /api/auth/register` - User registration
- `GET /api/quizzes` - List all quizzes
- `POST /api/quizzes` - Create quiz
- `GET /api/questions` - List questions
- `POST /api/questions` - Create question

## Acknowledgments

Anthropic Claude. (2025). AI assistance for programming and documentation. Available at: https://www.anthropic.com/claude.
[Accessed 14 Mar. 2025].

Note: While Claude provided assistance with programming concepts, documentation, and best practices, all code implementation and system architecture decisions were made by the author. This quiz application was developed independently with some guidance from AI.

