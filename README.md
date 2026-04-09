# KalviConnect - Frontend

KalviConnect is a modern, mobile-first Academy Management System Android application. This repository contains the Frontend project built with native Kotlin and XML over a traditional View-based architecture.

## 🚀 Phase 1: Authentication Flow

The main focus of Phase 1 is a robust, production-ready OTP-based authentication system enforcing modern UI aesthetics with standardized design parameters.

### 📱 Tech Stack
- **Language**: Kotlin
- **UI Components**: Native Android XML Views (No Jetpack Compose)
- **Networking**: Retrofit 2 + Gson Converter + OkHttp Logging Interceptor
- **Local Storage**: SharedPreferences (Managed safely via `TokenManager`)
- **Navigation**: Traditional `Intent` architecture 

### 🎨 Design System

All elements correspond to the designated UI standard:
- **Primary Color**: `#3B82F6` (Blue)
- **Secondary Color**: `#F3F4F6` (Light Gray)
- **Accent Color**: `#10B981` (Success Green)
- **Rounded Corners**: standardized strictly to `12dp` through global drawables (`bg_rounded_button.xml`, `bg_rounded_input.xml`).

### 🔐 Authentication Flow Steps

1. **Enter Phone**: User opens `LoginActivity`, inputs minimum 10 digit number.
2. **Request OTP**: App calls `POST /api/auth/send-otp/`. Loading state triggers.
3. **Handle Intent**: User redirected to `OtpActivity`.
4. **Enter OTP**: User keys in 6-digit verification code.
5. **Verify OTP**: App calls `POST /api/auth/verify-otp/`.
6. **Token Persistence**: App extracts `JWT Access`, `JWT Refresh`, and `User Role`. Stored safely onto internal storage via `SharedPreferences`.
7. **Redirection Context**: Navigates to `DashboardActivity`. Dashboard reflects tailored data based on whether you are an *Admin, Teacher, Parent, or Student*.

### 📂 Key Project Architecture

```plaintext
app/src/main/
├── java/com/example/frontend/
│   ├── api/                   # Retrofit singletons and API Service Interface.
│   ├── models/                # GSON serializable data classes mapping API schema.
│   ├── ui/                    # App Activities (Login, OTP, Dashboard Context).
│   └── utils/                 # Utility helpers handling app logic (TokenManager).
├── res/
│   ├── layout/                # UI definitions leveraging Linear and Constraint constraints.
│   ├── drawable/              # Modern XML shape tokens holding global corner radii & strokes.
│   └── values/                # Centralized colors and standard string attributes.
└── AndroidManifest.xml        # Contains Cleartext configs referencing local emulator APIs.
```

### ⚙️ Local Setup Guide

1. Clone backend logic explicitly routing APIs to port `:8000`.
2. Sync the Android Gradle toolchain in Android Studio.
3. Because Android emulators containerize local traffic, standard `localhost` won't map correctly. **Do not change the base URL randomly.**
   * Ensure `RetrofitClient.kt` specifically remains pointed to `http://10.0.2.2:8000/`.
4. Make sure your local emulator runs securely.
5. Click **Run** on Android Studio (`Shift + F10`) or via terminal: `./gradlew assembleDebug`.

### 🔮 Future Modules Structure

This Phase 1 template maps cleanly onto future module expansions including:
- Student Registration logic
- Attendance marking
- Academic Marks parsing
- Fee tracking
- Realtime Announcements
