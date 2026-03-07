#  Ocean View Resort — Hotel Reservation System

A full-stack hotel reservation management system for **Ocean View Resort** (Sri Lanka's Premier Resort). Built with a zero-framework Java backend, a plain HTML/CSS/JS frontend, and Microsoft SQL Server as the database.

---

## ✨ Features

- **Staff Authentication** — Login with role-based access (admin / staff)
- **Reservation Management** — Create, view, update status, and cancel reservations with duplicate/conflict checking
- **Room Rates** — Dynamic rates for Standard, Deluxe, Suite, and Family room types
- **Billing** — Auto-calculated bills based on room rate × nights stayed
- **E-Bill (Guest Portal)** — Public-facing bill lookup by reservation number (no login required)
- **Staff CRUD** *(Admin only)* — Add, edit, deactivate, and delete staff accounts
- **Dark Mode** — Toggle between light and dark themes on the frontend

---

## 🗂️ Project Structure

```
OceanViewResort/
├── frontend/
│   ├── index.html          # Login page
│   └── dashboard.html      # Main admin/staff dashboard
├── sql/
│   └── schema.sql          # Database schema + seed data (MSSQL)
├── src/
│   └── main/java/com/hotel/
│       ├── Main.java                    # HTTP server entry point (port 8080)
│       ├── db/DBConnection.java         # SQL Server JDBC connection
│       ├── handlers/
│       │   ├── AuthHandler.java         # POST /api/auth/login
│       │   ├── ReservationHandler.java  # CRUD /api/reservations, GET /api/rates
│       │   ├── BillingHandler.java      # GET /api/billing/{resNo}, /api/ebill/{resNo}
│       │   ├── StaffHandler.java        # CRUD /api/staff
│       │   └── BaseHandler.java         # Shared CORS, JSON helpers
│       ├── models/
│       │   ├── Reservation.java
│       │   └── User.java
│       └── util/JsonUtil.java
└── pom.xml
```

---

## 🛠️ Tech Stack

| Layer     | Technology                              |
|-----------|-----------------------------------------|
| Backend   | Java 21, `com.sun.net.httpserver`       |
| Database  | Microsoft SQL Server (MSSQL)            |
| JDBC      | `mssql-jdbc 12.8.1`                     |
| Frontend  | Vanilla HTML, CSS, JavaScript           |
| Build     | Maven 3                                 |
| Testing   | JUnit 5, REST-Assured                   |
| CI        | GitHub Actions                          |

---

## ⚙️ Prerequisites

- **Java 21+**
- **Maven 3.6+**
- **Microsoft SQL Server** (Express or full edition)
- **MSSQL JDBC Driver** (included via Maven dependency)

---

## 🚀 Setup & Installation

### 1. Clone the Repository

```bash
https://github.com/m-shamil/OceanViewResort.git
```

### 2. Configure the Database

Run the schema script against your SQL Server instance:

```sql
-- In SSMS or sqlcmd:
sqlcmd -S YOUR_SERVER_NAME -E -i sql/schema.sql
```

This will:
- Create the `HotelDB` database
- Create `Users`, `RoomRates`, and `Reservations` tables
- Seed default admin/staff accounts and room rates

### 3. Update the Database Connection

Edit `src/main/java/com/hotel/db/DBConnection.java` and update the connection URL to match your environment:

```java
private static final String URL =
    "jdbc:sqlserver://YOUR_SERVER:1433;" +
    "databaseName=HotelDB;" +
    "encrypt=true;" +
    "trustServerCertificate=true;" +
    "user=your_user;" +
    "password=your_password;";
```

> **Windows Authentication:** Replace `user`/`password` with `integratedSecurity=true` and ensure `sqljdbc_auth.dll` is on your system `PATH`.

### 4. Build the Project

```bash
mvn clean package
```

### 5. Run the Server

```bash
java -jar target/OceanViewResort-1.0-SNAPSHOT.jar
```

The backend will start on **http://localhost:8080**.

### 6. Open the Frontend

Open `frontend/index.html` in your browser (or serve it via any static file server).

---

## 🔑 Default Credentials

| Role  | Username | Password  |
|-------|----------|-----------|
| Admin | `admin`  | `admin123` |
| Staff | `staff1` | `staff123` |

> ⚠️ **Change these credentials immediately in production.**

---

## 📡 API Endpoints

| Method | Endpoint                        | Description                        |
|--------|---------------------------------|------------------------------------|
| POST   | `/api/auth/login`               | Authenticate a user                |
| GET    | `/api/reservations`             | List all reservations              |
| GET    | `/api/reservations/{resNo}`     | Get a reservation by number        |
| POST   | `/api/reservations`             | Create a new reservation           |
| PUT    | `/api/reservations/{resNo}`     | Update reservation status          |
| DELETE | `/api/reservations/{resNo}`     | Cancel a reservation               |
| GET    | `/api/rates`                    | List room rates                    |
| GET    | `/api/billing/{resNo}`          | Calculate bill (staff)             |
| GET    | `/api/ebill/{resNo}`            | View e-bill (guest, public)        |
| POST   | `/api/billing/print/{resNo}`    | Print/export a bill                |
| GET    | `/api/staff`                    | List all staff (admin only)        |
| GET    | `/api/staff/{id}`               | Get staff member by ID             |
| POST   | `/api/staff`                    | Create a staff account             |
| PUT    | `/api/staff/{id}`               | Update a staff account             |
| DELETE | `/api/staff/{id}`               | Delete a staff account             |

---

## 🧪 Running Tests

```bash
mvn test
```

Tests cover `AuthHandler`, `ReservationHandler`, `BillingHandler`, and `StaffHandler` using JUnit 5.

---

## 🔄 CI/CD

This project includes a GitHub Actions workflow (`.github/workflows/java-ci.yml`) that automatically builds and tests the project on every push and pull request.

---

## 🏨 Room Types & Rates

| Room Type | Rate per Night |
|-----------|---------------|
| Standard  | ₱1,500.00     |
| Deluxe    | ₱2,500.00     |
| Suite     | ₱5,000.00     |
| Family    | ₱3,500.00     |

---

## 📝 License

This project is intended for educational and internal use. See [LICENSE](LICENSE) for details.
