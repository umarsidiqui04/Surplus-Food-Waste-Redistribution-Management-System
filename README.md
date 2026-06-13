# 🍽️ SFWRMS — Surplus Food Waste Reduction Management System

A desktop management system built with **Java 17**, **JavaFX 21**, and **Microsoft SQL Server** that connects food donors, NGOs, and delivery drivers to minimize food waste through intelligent donation matching, urgency-based prioritization, and end-to-end delivery tracking.

![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)
![JavaFX](https://img.shields.io/badge/JavaFX-21-007396?logo=java&logoColor=white)
![SQL Server](https://img.shields.io/badge/SQL_Server-2019+-CC2927?logo=microsoftsqlserver&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-Kotlin_DSL-02303A?logo=gradle&logoColor=white)
![License](https://img.shields.io/badge/License-Educational-blue)

---

## ✨ Features

### 🔐 Multi-Role Authentication
- Secure login for **Food Donors**, **NGO Representatives**, and **Drivers**
- Password hashing with role-based session management
- Singleton `SessionManager` for global user state

### 🥗 Donation Management (Donor)
- **Log Surplus Food** — Record food type, quantity, unit, preparation & expiry times, and pickup location
- **Edit Donations** — Modify pending donations before assignment
- **Delete Donations** — Remove pending donations with cascading cleanup
- **Track Status** — Real-time donation status tracking (Pending → Assigned → InTransit → Delivered)

### 📊 Urgency Score Engine
- **Dynamic urgency scoring** calculated in real-time based on expiry proximity
- Formula: 48-hour sliding scale (5.0 → 100.0) computed both in Java and SQL
- Higher urgency donations surface first for NGO review

### 🏢 NGO Operations (NGO Representative)
- **Browse Available Donations** — View all pending donations with filtering by location, food type, and urgency
- **Accept / Reject Donations** — Manual review and decision workflow
- **Assign Drivers** — Select available drivers from the NGO's fleet for pickup
- **Manage Assignments** — Track active and cancelled assignments
- **Sustainability Reports** — Generate weekly/monthly/yearly reports with:
  - Total meals saved (2 kg = 1 meal)
  - Delivery success rate
  - Top donors and top driver analytics

### 🚗 Driver Operations
- **View Assignments** — See assigned pickups with food details, location, and donor info
- **Mark Picked Up** — Update status to InTransit when food is collected
- **Confirm Delivery** — Final delivery confirmation with timestamp

### 🔔 Notification System
- Role-based notifications (Donor / NGO Rep / Driver)
- Auto-generated on assignment, pickup, and delivery events
- Read/unread tracking

### 🏗️ Architecture & Design Patterns
- **3-Tier Layered Architecture**: Presentation (FXML + Controllers) → Business Logic (AppBackend) → Data (SQL Server)
- **GoF Singleton** — Single `AppBackend` instance
- **GoF Facade** — `AppBackend` as the single entry point for all business operations
- **GRASP Information Expert** — Domain operations owned by `AppBackend`
- **GRASP Controller** — Mediates between UI events and database
- **GRASP Low Coupling** — UI depends only on `AppBackend`

---

## 🛠️ Tech Stack

| Layer              | Technology                                    |
|--------------------|-----------------------------------------------|
| Language           | Java 17                                       |
| UI Framework       | JavaFX 21 (FXML + Controllers)                |
| UI Components      | ControlsFX 11.2, BootstrapFX 0.4              |
| Database           | Microsoft SQL Server (JDBC 12.6)              |
| Build Tool         | Gradle with Kotlin DSL                        |
| Packaging          | jlink (modular runtime images)                |
| Architecture       | 3-Tier Layered (MVC variant)                  |
| Design Patterns    | Singleton, Facade, Information Expert          |

---

## 📁 Project Structure

```
SFWRMS/
├── src/main/
│   ├── java/com/sfwrms/sfwrms/
│   │   ├── HelloApplication.java       # JavaFX entry point
│   │   ├── Launcher.java               # Main class launcher
│   │   ├── AppBackend.java             # Business logic facade (Singleton)
│   │   ├── DBConnection.java           # JDBC SQL Server connection
│   │   ├── SessionManager.java         # Global session state
│   │   ├── PasswordUtil.java           # Password hashing utility
│   │   ├── SceneHelper.java            # FXML scene navigation helper
│   │   ├── DataSetup.java              # Initial data seeding
│   │   │
│   │   ├── User.java                   # Base user abstract class
│   │   ├── FoodDonor.java              # Donor user model
│   │   ├── NGORep.java                 # NGO representative model
│   │   ├── DriverUser.java             # Driver user model
│   │   ├── DriverInfo.java             # Driver info DTO
│   │   │
│   │   ├── DonationModel.java          # Donation entity
│   │   ├── AssignmentModel.java         # Pickup assignment entity
│   │   ├── NotificationModel.java       # Notification entity
│   │   ├── ReportData.java             # Sustainability report DTO
│   │   │
│   │   ├── LoginController.java         # Login screen
│   │   ├── DonorDashboardController.java     # Donor home
│   │   ├── DonorDonationsController.java     # Donor donation list
│   │   ├── AddEditDonationController.java    # Add/Edit donation form
│   │   ├── NGODashboardController.java       # NGO home
│   │   ├── NGODonationsController.java       # NGO donation browser
│   │   ├── NGOAssignmentsController.java     # NGO assignment manager
│   │   ├── NGOAssignDriverController.java    # Assign driver dialog
│   │   ├── DriverDashboardController.java    # Driver home
│   │   ├── DriverAssignmentsController.java  # Driver pickup list
│   │   └── ReportController.java             # Sustainability reports
│   │
│   └── resources/com/sfwrms/sfwrms/
│       ├── login-view.fxml
│       ├── donor-dashboard.fxml
│       ├── donor-donations.fxml
│       ├── add-edit-donation.fxml
│       ├── ngo-dashboard.fxml
│       ├── ngo-donations.fxml
│       ├── ngo-assignments.fxml
│       ├── ngo-assign-driver.fxml
│       ├── driver-dashboard.fxml
│       ├── driver-assignments.fxml
│       └── report-view.fxml
│
├── sfwrms.sql                    # Main database schema
├── SFWRMS_AlterTables.sql        # Schema migration scripts
├── SFWRMS_AddQuantityUnit.sql    # Add quantity unit column
├── SFWRMS_ClearDonations.sql     # Reset donation data
├── build.gradle.kts              # Gradle build configuration
├── settings.gradle.kts           # Gradle settings
└── gradlew / gradlew.bat         # Gradle wrapper
```

---

## 🗄️ Database Schema

The system uses **9 normalized tables** in SQL Server:

```
┌─────────────┐     ┌──────────────┐     ┌────────────┐
│  FoodDonor   │     │     NGO      │     │   Driver   │
│─────────────│     │──────────────│     │────────────│
│ donorId (PK) │     │ ngoId (PK)   │◄────│ ngoId (FK) │
│ name         │     │ name         │     │ driverId   │
│ email        │     │ location     │     │ name       │
│ phone        │     │ capacity     │     │ vehicleInfo│
│ organization │     │ operArea     │     │ phone      │
│ passwordHash │     │ contactEmail │     │ passwordHash│
└──────┬───────┘     └──────┬───────┘     └─────┬──────┘
       │                    │                    │
       │              ┌─────┴──────┐             │
       │              │ NGORep     │             │
       │              │────────────│             │
       │              │ repId (PK) │             │
       │              │ ngoId (FK) │             │
       │              └─────┬──────┘             │
       │                    │                    │
       ▼                    ▼                    ▼
┌──────────────┐    ┌───────────────┐    ┌───────────────┐
│  Donation    │    │ PickupAssign  │    │ DeliveryRecord│
│──────────────│    │───────────────│    │───────────────│
│ donationId   │◄───│ donationId    │───►│ assignmentId  │
│ donorId (FK) │    │ driverId (FK) │    │ driverId (FK) │
│ foodType     │    │ repId (FK)    │    │ deliveryStatus│
│ quantity     │    │ status        │    │ pickedUpAt    │
│ expiryTime   │    │ assignedAt    │    │ deliveredAt   │
│ status       │    └───────────────┘    └───────────────┘
│ urgencyScore │
└──────────────┘
       │
       ▼
┌──────────────┐    ┌───────────────────┐
│ UrgencyScore │    │ Notification      │
│──────────────│    │───────────────────│
│ donationId   │    │ recipientId       │
│ score        │    │ donationId (FK)   │
│ calculatedAt │    │ message           │
└──────────────┘    │ recipientType     │
                    └───────────────────┘

┌─────────────────────┐
│ SustainabilityReport│
│─────────────────────│
│ repId (FK)          │
│ period              │
│ totalMealsSaved     │
│ successRate         │
└─────────────────────┘
```

---

## 🚀 Getting Started

### Prerequisites
- **Java JDK 17** or higher
- **Microsoft SQL Server** (2019 or later recommended)
- **SQL Server Management Studio (SSMS)** or Azure Data Studio
- **Gradle** (wrapper included — no separate install needed)

### Database Setup

1. **Create the database and tables** — Run the main schema:
   ```sql
   -- Open sfwrms.sql in SSMS and execute
   ```

2. **Apply migrations** (if needed):
   ```sql
   -- Run in order:
   -- SFWRMS_AlterTables.sql
   -- SFWRMS_AddQuantityUnit.sql
   ```

3. **Seed sample data** (optional):
   ```
   The DataSetup.java class can generate initial test data.
   ```

### Configure Database Connection

Edit `src/main/java/com/sfwrms/sfwrms/DBConnection.java`:
```java
String url = "jdbc:sqlserver://localhost:1433;databaseName=SFWRMS;encrypt=true;trustServerCertificate=true";
String user = "sa";
String password = "your_password";
```

### Build & Run

```bash
# Clone the repository
git clone https://github.com/umarsidiqui04/Surplus-Food-Waste-Redistribution-Management-System.git
cd Surplus-Food-Waste-Redistribution-Management-System

# Build the project
./gradlew build

# Run the application
./gradlew run
```

---

## 📋 Use Cases

| UC   | Title                              | Actor       |
|------|------------------------------------|-------------|
| UC01 | Log Surplus Food (Create Donation) | Donor       |
| UC02 | Modify Existing Donation           | Donor       |
| UC03 | Delete Donation                    | Donor       |
| UC04 | Track Donation Status              | Donor       |
| UC05 | View Total Meals Saved             | All         |
| UC06 | Generate Sustainability Report     | NGO Rep     |
| UC07 | View Available Donations           | NGO Rep     |
| UC08 | Auto-Match Donation with NGO       | System      |
| UC09 | Accept / Reject Donation           | NGO Rep     |
| UC10 | Assign / Reassign Driver           | NGO Rep     |
| UC11 | Update Pickup Status               | Driver      |
| UC12 | Confirm Delivery                   | Driver      |

---

## 🔒 Security Notes

- Passwords are hashed before storage (never stored in plaintext)
- SQL Server connection uses `trustServerCertificate=true` for local development
- Role-based access enforced through `SessionManager`
- **Note**: Update `DBConnection.java` credentials before deployment

---

## 👥 Team

Built as a university semester project for **Software Design & Architecture (SDA)**.

---

## 📄 License

This project is for educational purposes.
