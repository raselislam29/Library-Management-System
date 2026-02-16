# Library Management System

A full-featured Library Management System built with Java, JavaFX, and Firebase. Supports role-based access for admins/librarians and members, with features for book catalog management, borrowing/returning, user management, notifications, and profile settings.

## Features

### Authentication
- Email/password sign-in and registration via Firebase Auth REST API
- Password reset via email
- Role-based routing (Admin/Librarian vs. Member dashboards)
- Dark/light theme toggle on login screen

### Admin / Librarian Dashboard
- **Home** - Overview and quick stats
- **Books** - Add, edit, delete, and search books by title, author, or ISBN
- **Borrowers** - View active and returned borrow records
- **Users** - List all users, assign roles (ADMIN, LIBRARIAN, MEMBER)
- **Notifications** - Low-stock book alerts and overdue borrow warnings
- **Profile & Settings** - View/edit profile, change password

### Member Dashboard
- **Home** - Personalized overview
- **Browse** - Search and browse the book catalog
- **Borrows** - View active borrows with due dates
- **Notifications** - Personal borrow-related alerts
- **Profile & Settings** - View/edit profile, change password

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 17 |
| UI Framework | JavaFX 24 + FXML |
| Backend / Database | Firebase Admin SDK (Firestore) |
| Authentication | Firebase Auth (REST API) |
| Build Tool | Maven |
| JSON Parsing | Gson |
| UI Libraries | ControlsFX, BootstrapFX, ValidatorFX |

## Project Structure

```
Library-Management-System/
├── pom.xml
├── src/main/
│   ├── java/com/lms/lmsfinal/
│   │   ├── LibraryApp.java              # Application entry point
│   │   ├── FirebaseService.java         # All Firebase/Firestore operations
│   │   ├── Session.java                 # Current user session state
│   │   ├── Book.java                    # Book model
│   │   ├── User.java                    # User model
│   │   ├── BorrowRecord.java            # Borrow record model
│   │   ├── UserBorrow.java              # User borrow DTO
│   │   ├── UserSummary.java             # User summary DTO
│   │   ├── LoginController.java         # Login screen controller
│   │   ├── RegisterController.java      # Registration screen controller
│   │   ├── AdminShellController.java    # Admin dashboard shell
│   │   ├── UserShellController.java     # User dashboard shell
│   │   ├── Admin*Controller.java        # Admin sub-page controllers
│   │   ├── User*Controller.java         # User sub-page controllers
│   │   └── BorrowFormController.java    # Book borrow form
│   └── resources/com/lms/lmsfinal/
│       ├── *.fxml                       # UI layout files
│       ├── styles/                      # CSS themes
│       ├── images/                      # UI images
│       └── books.json                   # Sample book data for import
```

## Prerequisites

- **Java 17** or later
- **Maven 3.8+**
- A **Firebase project** with:
  - Firestore database enabled
  - Email/password authentication enabled
  - A service account key (`serviceAccountKey.json`)

## Getting Started

1. **Clone the repository:**
   ```bash
   git clone https://github.com/raselislam29/Library-Management-System.git
   cd Library-Management-System
   ```

2. **Add your Firebase service account key:**
   Place your `serviceAccountKey.json` file at:
   ```
   src/main/resources/com/lms/lmsfinal/serviceAccountKey.json
   ```
   This file is git-ignored for security.

3. **Build and run:**
   ```bash
   ./mvnw javafx:run
   ```
   Or if you have Maven installed globally:
   ```bash
   mvn javafx:run
   ```

4. **Login or register** with an email and password. The first user can be promoted to ADMIN via Firestore console or by another admin.

## Firestore Collections

| Collection | Purpose |
|------------|---------|
| `users` | Stores user roles, profile info, and join dates (keyed by Firebase Auth UID) |
| `books` | Book catalog with ISBN, title, author, category, publisher, and copy counts |
| `borrows` | Borrow records linking users to books with timestamps and return status |

## License

This project is for educational purposes.
