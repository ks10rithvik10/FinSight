# FinSight — Personal Finance Management System (Java Spring Boot)

A full-stack **Java 21 + Spring Boot 3** personal finance web application with **JWT Login & Registration**, per-user data isolation, transaction CRUD, monthly budgets, savings goals, analytics charts, moving-average expense forecasting, Z-score anomaly detection, CSV report export, and full **User Settings**.

---

## Getting Started (Registration & Login)
1. Start the application and open **http://localhost:8080** in your browser.
2. Click **Create account** to register your user profile (stored securely in H2 with BCrypt password hashing).
3. Sign in to manage transactions, budgets, savings goals, reports, and account settings.

---

## Real Settings Module (`/api/settings`)
- **Profile & Preferences:** Update display name, switch preferred currency (`INR ₹`, `USD $`, `EUR €`, `GBP £`), and set your monthly savings target percentage.
- **Security:** Change account password with current-password verification and BCrypt re-hashing.
- **Data Management:** Clear all personal financial records or permanently delete your account.

---

## How to Run on Windows (10 / 11)
Open **Command Prompt (`cmd`)** inside `FinSight_Project` and run:
```bat
mvn spring-boot:run
```
Or double-click **`run.bat`** inside `FinSight_Project`.

---

## Project Package Structure
```text
src/main/java/com/finsight/
├── FinSightApplication.java       # Spring Boot main entry point
├── controller/
│   ├── AuthController.java        # /api/auth/login, /api/auth/register, /api/me
│   ├── FinanceController.java     # /api/dashboard, /api/transactions, /api/budgets, /api/goals
│   ├── AnalyticsController.java   # /api/analytics/*, /api/insights/*, /api/reports/monthly
│   ├── SettingsController.java    # /api/settings/profile, /password, /data, /account
│   └── AdminController.java       # /api/admin/overview (ROLE_ADMIN)
├── dto/
│   ├── LoginRequest.java          # Validated Java Record for login
│   ├── RegisterRequest.java       # Validated Java Record for registration
│   ├── TransactionRequest.java    # Validated Java Record for transactions
│   ├── BudgetRequest.java         # Validated Java Record for budgets
│   ├── GoalRequest.java           # Validated Java Record for savings goals
│   ├── UpdateSettingsRequest.java # Validated Java Record for profile/currency/target settings
│   └── ChangePasswordRequest.java # Validated Java Record for password updates
├── model/
│   ├── AppUser.java               # JPA Entity (users table + currency & savings target)
│   ├── Transaction.java           # JPA Entity (transactions table)
│   ├── Budget.java                # JPA Entity (budgets table)
│   └── FinancialGoal.java         # JPA Entity (financial_goals table)
├── repository/
│   ├── UserRepository.java        # Spring Data JPA Repository
│   ├── TransactionRepository.java # Spring Data JPA Repository
│   ├── BudgetRepository.java      # Spring Data JPA Repository
│   └── FinancialGoalRepository.java
├── security/
│   ├── SecurityConfig.java        # Spring Security + BCrypt + Stateless JWT chain
│   ├── JwtService.java            # JWT token generation & verification
│   └── JwtAuthenticationFilter.java
└── service/
    ├── AuthService.java           # Authentication, settings updates & BCrypt password change
    ├── FinanceService.java        # Core business logic & data reset/account deletion
    └── AnalyticsService.java      # 6-month trends, moving average forecast, Z-score anomalies, CSV
```
