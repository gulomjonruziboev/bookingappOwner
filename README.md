# Buron Owner — Android App

Native Kotlin / Jetpack Compose app for **toyxona (banquet hall) owners** on the Buron platform.

## Requirements

- Android Studio Ladybug or newer
- JDK 17 (Android Studio embedded JBR works)
- Android SDK 35
- minSdk 26

## Backend

| Setting | Value |
|---------|-------|
| API | `https://toyxona-backend-qb3x.onrender.com/api/` |
| Uploads | `https://toyxona-backend-qb3x.onrender.com/uploads/` |
| Health | `GET /api/health` |

> **Note:** Render free tier may cold-start in 15–30 seconds. The splash screen retries automatically.

## Test accounts (owner)

| Phone | Password |
|-------|----------|
| +998901111111 | owner123 |
| +998902222222 | owner123 |
| +998903333333 | owner123 |

## Build

```bash
# Windows (use Android Studio JBR)
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
./gradlew assembleDebug
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`

Release:

```bash
./gradlew assembleRelease
```

## Run tests

```bash
./gradlew test
```

## Features

- Owner login / register (telegram required, `role: owner` only)
- Venue CRUD with multipart image upload (min 3 images on create)
- Bookings list with filters, confirm/cancel, click-to-call
- External (walk-in) bookings
- Dashboard: calendar + monthly statistics + Excel/PDF export
- Read-only venue reviews
- Profile (read-only; no update API on backend)
- JWT stored in EncryptedSharedPreferences; 401 clears session

## Project structure

```
app/src/main/java/uz/buron/owner/
├── data/          # API, DTOs, repositories, TokenStore
├── domain/model/  # Domain models
├── presentation/  # Screens + ViewModels
├── ui/            # Theme, components, navigation
├── di/            # Hilt modules
└── util/          # Constants, validation, formatters
```

## Screenshots

Capture at least 8 screens after installing the debug APK and save them in `screenshots/`:

1. Login
2. Register
3. Dashboard — calendar tab
4. Dashboard — statistics tab
5. Bookings list
6. Venues list
7. Add / edit venue
8. Profile

## Package

- **applicationId:** `uz.buron.owner`
- Coexists with the client app `uz.buron` on the same device.
# bookingappOwner
