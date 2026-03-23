# MDM Backend — Android Enterprise Device Management

## Overview
This is the Spring Boot backend for the Android Enterprise Device Management solution.
It exposes REST APIs to support device enrollment, device information collection,
and installed application inventory from Android Device Owner apps.

## Tech Stack
- Java 17
- Spring Boot 4.x
- PostgreSQL 18
- Spring Security (Basic Auth)
- Spring Data JPA / Hibernate
- Maven

## Project Structure
```
backend/
├── src/main/java/com/mdm/mdm_backend/
│   ├── controller/        # REST API endpoints
│   ├── service/           # Business logic
│   ├── repository/        # Database operations
│   ├── model/
│   │   ├── entity/        # Database table mappings
│   │   └── dto/           # Request/Response objects
│   ├── security/          # Authentication config
│   └── exception/         # Global error handling
└── src/main/resources/
    └── application.properties
```

## Database Setup
1. Install PostgreSQL
2. Open pgAdmin and run:
```sql
CREATE DATABASE mdm_db;
CREATE USER mdm_user WITH PASSWORD 'mdm_password';
GRANT ALL PRIVILEGES ON DATABASE mdm_db TO mdm_user;
GRANT USAGE ON SCHEMA public TO mdm_user;
GRANT CREATE ON SCHEMA public TO mdm_user;
ALTER SCHEMA public OWNER TO mdm_user;
```

## How to Run
1. Clone the repository
2. Navigate to the backend folder
3. Run the application:
```bash
./mvnw spring-boot:run
```
Server starts on: `http://localhost:8080`

## API Endpoints

### Authentication
All endpoints use Basic Authentication:
- Username: `mdm-device`
- Password: `SecurePass123`

### 1. Enroll Device
```
POST /api/enroll
```
Request body:
```json
{
  "deviceId": "uuid-string",
  "enrollmentToken": "TOKEN123",
  "enrollmentMethod": "QR_CODE"
}
```
Response:
```json
{
  "message": "Device enrolled successfully",
  "deviceId": "uuid-string",
  "status": "ACTIVE"
}
```

### 2. Send Device Info
```
POST /api/device-info
```
Request body:
```json
{
  "deviceId": "uuid-string",
  "model": "Pixel 7",
  "manufacturer": "Google",
  "osVersion": "13",
  "sdkVersion": "33",
  "serialNumber": "SN123456",
  "imei": "123456789012345",
  "deviceType": "SMARTPHONE"
}
```

### 3. Send App Inventory
```
POST /api/app-inventory
```
Request body:
```json
{
  "deviceId": "uuid-string",
  "apps": [
    {
      "appName": "Chrome",
      "packageName": "com.android.chrome",
      "versionName": "114.0",
      "versionCode": 114,
      "installSource": "system",
      "isSystemApp": true
    }
  ]
}
```

## Database Tables
| Table | Description |
|---|---|
| devices | Enrolled device records |
| device_info | Hardware and OS information |
| app_inventory | Installed application list |

## Team
- Backend: [Your Name]
- Android App: [Friend's Name]