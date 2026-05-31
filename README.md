Here is your complete `README.md` for GitHub:

```markdown
# 🎬 Movie Ticket Booking System — Backend

A production-ready RESTful backend for an online movie ticket booking platform built with Java Spring Boot, similar to BookMyShow.

---

## 🚀 Tech Stack

| Technology | Purpose |
|-----------|---------|
| Java 21 | Core language |
| Spring Boot 3.x | Backend framework |
| Spring Security | Authentication & authorization |
| JWT (jjwt 0.11.5) | Stateless token-based auth |
| Spring Data JPA | Database ORM |
| Hibernate | Object-relational mapping |
| MySQL 8.0 | Relational database |
| JavaMailSender | OTP & booking email notifications |
| Nvidia AI API | AI-generated booking confirmation messages |
| Maven | Build tool |
| Lombok | Boilerplate reduction |

---

## ✨ Features

- 🔐 **JWT Authentication** — Stateless token-based login with OTP verification via email
- 🔒 **BCrypt Password Encryption** — Secure password storage using Spring Security
- 🎬 **Movie Management** — Browse, search by title, filter by genre and language
- 🏟️ **Theater & Screen Management** — Multi-city theater support with screen-level seat management
- 💺 **Real-time Seat Management** — Seat status tracking (AVAILABLE → LOCKED → BOOKED) per show
- 🎟️ **Booking System** — Complete booking lifecycle with payment recording and cancellation
- 📧 **Email Notifications** — OTP login emails and AI-generated booking confirmation emails
- 🤖 **Nvidia AI Integration** — Personalized confirmation messages using Llama 3.1 model
- ⭐ **Review System** — Movie ratings and comments per user
- 🛡️ **Global Exception Handling** — ResourceNotFoundException and SeatUnavailableException

---

## 🗄️ Database Design

10 interrelated MySQL tables:

```
users ──────────────────► bookings ◄──── payments
  │                           │
  └──► reviews            show_seats ◄──── seats
         │                    │               │
       movies ◄── shows ──────┘           screens
                    │                         │
                  screens ◄──────────────── theaters
```

| Table | Description |
|-------|-------------|
| users | User details with BCrypt password |
| movies | Movie info with poster URL |
| theaters | Theater details with city |
| screens | Screens linked to theaters |
| seats | Physical seats with type (REGULAR/PREMIUM/VIP) |
| shows | Movie-screen-time linking |
| show_seats | Real-time seat availability per show |
| bookings | Booking records with status |
| payments | Payment details with transaction ID |
| reviews | Movie ratings and comments |

---

## 🔌 API Endpoints

### User APIs
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | /api/users/register | Register new user | Public |
| POST | /api/users/send-otp | Send OTP to email | Public |
| POST | /api/users/verify-otp | Verify OTP → returns JWT | Public |
| GET | /api/users/{id} | Get user by ID | Protected |
| PUT | /api/users/{id} | Update user profile | Protected |

### Movie APIs
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | /api/movies | Get all movies | Public |
| GET | /api/movies/{id} | Get movie by ID | Public |
| GET | /api/movies/search?title= | Search by title | Public |
| GET | /api/movies/genre/{genre} | Filter by genre | Public |
| GET | /api/movies/language/{lang} | Filter by language | Public |

### Show APIs
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | /api/shows/movie/{movieId} | Get shows by movie | Public |
| GET | /api/shows/movie/{id}/city/{city} | Filter by city | Public |
| GET | /api/shows/{id}/seats | Get all seats for show | Public |

### Theater APIs
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | /api/theaters | Get all theaters | Public |
| GET | /api/theaters/city/{city} | Filter by city | Public |

### Booking APIs
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | /api/bookings/create-order | Create simulated order + AI message | Protected |
| POST | /api/bookings/confirm | Confirm booking + send email | Protected |
| GET | /api/bookings/user/{userId} | Get user bookings | Protected |
| PUT | /api/bookings/cancel/{id} | Cancel booking | Protected |

### Review APIs
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | /api/reviews | Add review | Protected |
| GET | /api/reviews/movie/{movieId} | Get reviews by movie | Public |

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────┐
│           React Frontend                │
│           localhost:3000                │
└──────────────┬──────────────────────────┘
               │ REST API (Axios + JWT)
┌──────────────▼──────────────────────────┐
│         Spring Boot Backend             │
│           localhost:8081                │
│                                         │
│  Controllers → Services → Repositories  │
│  JWT Filter → Spring Security           │
│  GlobalExceptionHandler                 │
└──────────────┬──────────────────────────┘
               │ JPA / Hibernate
┌──────────────▼──────────────────────────┐
│           MySQL Database                │
│              bms_db                     │
│          10 Related Tables              │
└─────────────────────────────────────────┘
```

---

## ⚙️ Setup & Run

### Prerequisites
- Java 21
- MySQL 8.0
- Maven

### Steps

**1. Clone the repository**
```bash
git clone https://github.com/prasanjitpattanaik551/MovieTicketBookingApp.git
cd BookMyShow
```

**2. Configure `application.properties`**
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/bms_db
spring.datasource.username=root
spring.datasource.password=YOUR_PASSWORD

spring.mail.host=smtp.office365.com
spring.mail.port=587
spring.mail.username=YOUR_EMAIL
spring.mail.password=YOUR_PASSWORD

nvidia.api.key=YOUR_NVIDIA_API_KEY

jwt.secret=your_secret_key_minimum_32_characters
jwt.expiration=86400000
```

**3. Run the application**
```bash
mvn spring-boot:run
```

Backend starts on `http://localhost:8081`

---

## 🔐 Authentication Flow

```
1. POST /api/users/send-otp?email=user@email.com
   → OTP generated → saved in DB → sent via email

2. POST /api/users/verify-otp?email=user@email.com&otp=123456
   → OTP verified → JWT token returned

3. All protected APIs → pass JWT in header:
   Authorization: Bearer <token>
```

---

## 🎟️ Booking Flow

```
1. POST /api/bookings/create-order
   → Calculates total amount
   → Generates Order ID (ORD-XXXXXXXX)
   → Nvidia AI generates confirmation message
   → Returns orderId + aiMessage to frontend

2. POST /api/bookings/confirm?orderId=ORD-XXXXXXXX
   → Validates seat availability
   → Creates Booking + Payment records
   → Updates show_seats status to BOOKED
   → Sends confirmation email with AI message
```

---

## 🧪 Testing

Tested using **Postman** for all API endpoints.

---

## 👨‍💻 Developer

**Prasanjit Pattanaik**
MCA — KIIT Deemed to be University
2025-26
```

Copy this into your `README.md` file in the root of your Spring Boot project. It covers everything a recruiter or interviewer would want to see.
