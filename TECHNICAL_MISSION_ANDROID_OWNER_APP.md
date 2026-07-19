# Technical Mission: Toyxona Owner Android App (Kotlin)

## 1. Project Overview

**Buron / Toyxona** is a wedding and banquet hall booking platform for Uzbekistan. The customer (client) web app is already complete. Your task is to build a **native Android app for venue owners only** (`role: owner`).

| Component | Technology | Status |
|-----------|------------|--------|
| Backend | Node.js, Express, MongoDB, JWT | Deployed on [ToyBron](http://api.toybron.uz/) |
| Frontend (web) | Next.js 15, TypeScript, Tailwind | Client + owner web dashboard exist |
| **Android (your task)** | **Kotlin, Jetpack Compose** | **Owner app only — to be built** |

**Important:** This is **not** a food ordering or restaurant POS system. Core entities are **Venue** (banquet hall), **Booking** (reservation), and **Review**. There are no Menu, Order, Cart, or Payment models in the backend.

Map concepts as follows:

- **Venue** = banquet hall / event space (called "to'yxona" in the product)
- **Booking** = reservation (analogous to an order in other domains)
- **Sessions** = time slots (morning / afternoon / evening)

---

## 2. Goal and Scope

### 2.1 Goal

Give venue owners a mobile app to:

- Log in and register as an owner
- Manage their venues (create, read, update, delete)
- View, confirm, and cancel bookings
- View availability on a calendar and add external (offline) bookings
- View analytics and export reports (Excel/PDF)
- Read venue reviews (read-only)

### 2.2 In Scope

- Only users with `role: owner`
- Feature parity with the web owner dashboard (`/dashboard/*`)
- Uzbek UI labels (matching the existing web app terminology)
- Integration with the production backend on Render

### 2.3 Out of Scope

- Customer (client) mobile app — already done elsewhere
- Admin panel
- Real-time push notifications / WebSockets (not supported by backend)
- Profile update API (placeholder on web as well)
- Password reset, OTP, payment processing
- Menu / food order management

---

## 3. Backend Integration (ToyBron)

### 3.1 Production API

```
Base URL:  http://api.toybron.uz/api
Health:    GET /api/health  →  { "status": "ok" }
Uploads:   http://api.toybron.uz/uploads/{filename}
```

The health endpoint is verified and the backend is live.

### 3.2 Deployment Considerations

| Issue | Solution |
|-------|----------|
| Cold start | Show splash/loading screen, implement retry logic, inform the user |
| JWT expiry (7 days) | Persist token securely; on 401 redirect to login |
| CORS | Does not apply to native Android apps |
| Image uploads | Use `multipart/form-data`; max 5 MB per file; JPEG, PNG, WebP only |

### 3.3 Android Configuration

```kotlin
// build.gradle.kts (BuildConfig)
const val BASE_URL = "http://api.toybron.uz/api/"
const val UPLOADS_BASE_URL = "http://api.toybron.uz/uploads/"
```

**AndroidManifest.xml permissions:**

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<uses-permission android:name="android.permission.CALL_PHONE" />
```

HTTP is used; `android:usesCleartextTraffic="true"` is required in `AndroidManifest.xml`.

---

## 4. Recommended Architecture

### 4.1 Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Kotlin 2.x |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + simplified Clean Architecture |
| DI | Hilt |
| Networking | Retrofit 2 + OkHttp + Gson/Moshi |
| Async | Kotlin Coroutines + Flow |
| Token storage | DataStore + EncryptedSharedPreferences |
| Images | Coil |
| Navigation | Navigation Compose |
| Charts | Vico or MPAndroidChart |
| File export | OkHttp blob download → Storage Access Framework |

### 4.2 Package Structure

```
com.buron.owner/
├── data/
│   ├── remote/          # ApiService, DTOs, AuthInterceptor
│   ├── local/           # TokenStore, UserPreferences
│   └── repository/      # AuthRepo, VenueRepo, BookingRepo
├── domain/
│   ├── model/           # User, Venue, Booking, DashboardStats
│   └── usecase/
├── presentation/
│   ├── auth/            # Login, Register
│   ├── dashboard/       # Stats + Calendar tabs
│   ├── venues/          # List, Create, Edit, Detail
│   ├── bookings/        # List, Filters, Actions
│   ├── reviews/         # Read-only list
│   └── profile/         # View profile
├── util/                # Formatters, Constants, Validators
└── di/                  # Hilt modules
```

---

## 5. Authentication

### 5.1 Flow

```
Register/Login → JWT token + User object
       ↓
Store in DataStore (token + user JSON)
       ↓
Every request: Authorization: Bearer {token}
       ↓
401 → clearAuth() → Login screen
403 → Show "Permission denied" message
```

### 5.2 API Endpoints

#### POST `/auth/register` (Owner)

```json
{
  "role": "owner",
  "firstName": "Ali",
  "lastName": "Valiyev",
  "phone": "+998901234567",
  "telegram": "@ali_owner",
  "password": "123456"
}
```

**Validation:** `password` min 6 characters; `telegram` is required for owners.

#### POST `/auth/login`

```json
{
  "phone": "+998901234567",
  "password": "123456"
}
```

**Response (register/login):**

```json
{
  "token": "eyJhbG...",
  "user": {
    "_id": "...",
    "role": "owner",
    "firstName": "Ali",
    "lastName": "Valiyev",
    "phone": "+998901234567",
    "telegram": "@ali_owner",
    "isEnabled": true,
    "createdAt": "..."
  }
}
```

#### GET `/auth/me` (Bearer token required)

Returns the current user profile.

### 5.3 AuthInterceptor (OkHttp)

```kotlin
class AuthInterceptor @Inject constructor(
    private val tokenStore: TokenStore
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenStore.getTokenSync()
        val request = if (token != null) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else chain.request()
        return chain.proceed(request)
    }
}
```

### 5.4 Test Accounts (from seed script)

| Role | Phone | Password |
|------|-------|----------|
| Owner | +998901111111 | owner123 |
| Owner | +998902222222 | owner123 |
| Owner | +998903333333 | owner123 |

---

## 6. Data Models (Kotlin)

Mirror the web types from `toyxona-frontend/lib/types.ts`:

```kotlin
enum class UserRole { client, owner, admin }

data class User(
    @SerializedName("_id") val id: String,
    val role: UserRole,
    val firstName: String,
    val lastName: String,
    val phone: String,
    val telegram: String? = null,
    val isEnabled: Boolean
)

data class Venue(
    @SerializedName("_id") val id: String,
    val owner: Any?,
    val name: String,
    val description: String,
    val address: String,
    val mapLink: String? = null,
    val location: Location? = null,
    val region: String,
    val district: String,
    val phone: String,
    val images: List<String>,
    val pricePerSession: Double,
    val capacity: Int,
    val rating: Double,
    val totalBookings: Int,
    val status: VenueStatus,
    val isEnabled: Boolean
)

enum class VenueStatus { pending, approved, rejected }

data class Booking(
    @SerializedName("_id") val id: String,
    val venue: Any?,
    val clientName: String,
    val clientPhone: String,
    val date: String,
    val sessions: List<Session>,
    val status: BookingStatus,
    val isExternalBooking: Boolean? = false,
    val createdAt: String? = null
)

enum class Session { morning, afternoon, evening }
enum class BookingStatus { pending, confirmed, cancelled }
```

### UI Label Constants (from web `lib/constants.ts`)

```kotlin
object Labels {
    val SESSION = mapOf(
        "morning" to "Nahorgi (09:00–14:00)",
        "afternoon" to "Abetgi (14:00–18:00)",
        "evening" to "Kechgi (18:00–23:00)"
    )
    val BOOKING_STATUS = mapOf(
        "pending" to "Kutilmoqda",
        "confirmed" to "Tasdiqlangan",
        "cancelled" to "Bekor qilingan"
    )
    val VENUE_STATUS = mapOf(
        "pending" to "Ko'rib chiqilmoqda",
        "approved" to "Tasdiqlangan",
        "rejected" to "Rad etilgan"
    )
}
```

UI strings remain in Uzbek to match the existing web product.

---

## 7. Full API Contract (Owner App)

All paths are relative to `http://api.toybron.uz/api`.

### 7.1 Auth

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/auth/register` | No | Register as owner |
| POST | `/auth/login` | No | Login with phone + password |
| GET | `/auth/me` | Bearer | Get current profile |

Rate limit on register/login: 50 requests per 15 minutes.

### 7.2 Venues

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/venues/owner/my` | Bearer (owner) | List my venues |
| GET | `/venues/{id}` | Bearer | Get single venue |
| POST | `/venues` | Bearer (owner) | Create venue (multipart) |
| PUT | `/venues/{id}` | Bearer (owner) | Update venue (multipart) |
| DELETE | `/venues/{id}` | Bearer (owner) | Delete venue and its bookings |

**POST/PUT FormData fields:**

| Field | Type | Required |
|-------|------|----------|
| name | String | Yes |
| description | String | Yes |
| address | String | Yes |
| region | String | Yes |
| district | String | Yes |
| phone | String | Yes |
| pricePerSession | Number | Yes |
| capacity | Int (≥ 1) | Yes |
| mapLink | URL | No |
| images | File[] | Yes on create (min 3 total) |

**On PUT:** pass existing image URLs as a JSON array in the `images` field; append new files in the `images` multipart field.

**Moderation rule:** editing an `approved` venue resets status to `pending`.

### 7.3 Bookings

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/bookings/owner/dashboard` | Bearer (owner) | Analytics dashboard |
| GET | `/bookings/owner/dashboard/export` | Bearer (owner) | Export report |
| GET | `/bookings/venue/{venueId}` | Bearer (owner) | All bookings for a venue |
| GET | `/bookings/venue/{venueId}/calendar` | No | Monthly availability calendar |
| POST | `/bookings` | Bearer (owner) | Create external booking |
| PUT | `/bookings/{id}/status` | Bearer (owner) | Confirm or cancel booking |

**Dashboard query params:** `?month=YYYY-MM`, `?venueId=<id>`

**Export query params:** `?format=xlsx|pdf`, `?month=`, `?venueId=`

**POST `/bookings` (external booking):**

```json
{
  "venueId": "...",
  "clientName": "Aziz",
  "clientPhone": "+998901234567",
  "date": "2026-06-20",
  "sessions": ["morning", "evening"],
  "isExternalBooking": true
}
```

When `isExternalBooking: true`, status is immediately set to `confirmed`.

**PUT `/bookings/{id}/status`:**

```json
{ "status": "confirmed" }
// or
{ "status": "cancelled" }
```

### 7.4 Reviews (read-only)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/reviews/venue/{venueId}` | No | List reviews for a venue |

### 7.5 Dashboard Response Shape

```json
{
  "totalBookingsThisMonth": 5,
  "revenueEstimate": 40000000,
  "activeVenues": 2,
  "pendingBookings": 3,
  "totalVenues": 3,
  "recentBookings": [],
  "bookingsByMonth": [{ "month": "2026-06", "label": "...", "count": 5 }],
  "revenueByMonth": [{ "month": "2026-06", "label": "...", "revenue": 40000000 }],
  "statusBreakdown": [{ "name": "Kutilmoqda", "value": 3, "status": "pending" }],
  "perVenue": [{
    "venueId": "...",
    "name": "...",
    "status": "approved",
    "bookings": 5,
    "confirmed": 3,
    "revenue": 20000000
  }]
}
```

### 7.6 Calendar Response Shape

```json
{
  "month": "2026-06",
  "calendar": {
    "2026-06-01": {
      "booked": ["morning"],
      "available": ["afternoon", "evening"],
      "status": "partial"
    }
  }
}
```

Day statuses: `available` | `partial` | `full`

**Session rules:** max 3 sessions per day across all bookings; sessions are `morning`, `afternoon`, `evening`.

---

## 8. Screens and Navigation

### 8.1 Screen Map

```
SplashScreen
    ↓
[Has token?] → Dashboard    [No token?] → Login
                                              ↓
                                         Register (owner)

Bottom Navigation:
├── Home (Dashboard)
├── Bookings
├── Venues
└── Profile

Nested screens:
├── Add Venue
├── Edit Venue / {id}
├── Venue Details
├── Add External Booking (Modal / BottomSheet)
├── Reviews / {venueId}
└── Export Statistics
```

### 8.2 Screen Specifications

#### S1. Splash Screen
- Check stored token
- Optional `GET /api/health` ping
- Loading animation for Render cold start

#### S2. Login
- Phone + password fields
- "Remember me" checkbox (DataStore persistence)
- Link to registration
- Only allow `role == owner`; reject other roles with an error message

#### S3. Register (Owner)
- Fields: first name, last name, phone, Telegram (@username), password
- Automatically set `role: "owner"`
- On success → Dashboard

#### S4. Dashboard (2 tabs)

**Tab 1 — Calendar:**
- Venue selector (dropdown)
- Monthly calendar view
- Day colors: green (available), yellow (partial), red (full)
- Select date + sessions → create external booking

**Tab 2 — Statistics:**
- 4 stat cards: bookings this month, revenue estimate, active venues, pending bookings
- Line chart: bookings by month
- Bar chart: revenue by month
- Pie chart: status breakdown
- Per-venue table
- Export to Excel / PDF buttons

#### S5. Bookings
- Filter by venue
- Filter by status: All / Pending / Confirmed / Cancelled
- Booking card: client name, phone (click-to-call), date, sessions, status badge
- For `pending`: **Confirm** / **Cancel** actions
- FAB: **Add External Booking**

#### S6. Venues List
- Cards: image, name, status badge, rating, price
- Actions: Edit, Delete, View Reviews
- FAB: Add New Venue

#### S7. Add / Edit Venue
- Same form fields as web `VenueForm`
- Image picker (gallery; minimum 3 on create)
- Region / district dropdowns (Uzbekistan regions)
- Optional map link
- Submit via multipart POST/PUT

#### S8. Reviews (Read-only)
- List reviews for a venue
- Star rating, comment, date

#### S9. Profile
- Load data from `GET /auth/me`
- Phone — display only
- Telegram — display only (no update API exists)
- Logout button

---

## 9. User Stories

| ID | Story | Acceptance Criteria |
|----|-------|---------------------|
| US-01 | As an owner, I log in | JWT stored; Dashboard opens |
| US-02 | As a new owner, I register | Telegram required; role=owner |
| US-03 | I add a new venue | Min 3 images; status=pending |
| US-04 | I edit my venue | Existing images preserved |
| US-05 | I delete my venue | Confirmation dialog; API DELETE succeeds |
| US-06 | I confirm a pending booking | Status → confirmed |
| US-07 | I cancel a booking | Status → cancelled |
| US-08 | I add a phone/walk-in booking | isExternalBooking=true; immediately confirmed |
| US-09 | I view availability on calendar | 3 sessions shown with correct colors |
| US-10 | I view monthly statistics | 4 cards + 3 charts displayed |
| US-11 | I export a report | Excel/PDF downloads via system save dialog |
| US-12 | I call a client | Intent.ACTION_DIAL opens phone app |
| US-13 | My token expires | 401 redirects to Login screen |

---

## 10. Retrofit API Interface (Sample)

```kotlin
interface BuronApiService {

    // Auth
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): AuthResponse

    @GET("auth/me")
    suspend fun getMe(): User

    // Venues
    @GET("venues/owner/my")
    suspend fun getMyVenues(): List<Venue>

    @GET("venues/{id}")
    suspend fun getVenue(@Path("id") id: String): Venue

    @Multipart
    @POST("venues")
    suspend fun createVenue(
        @PartMap fields: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part images: List<MultipartBody.Part>
    ): Venue

    @Multipart
    @PUT("venues/{id}")
    suspend fun updateVenue(
        @Path("id") id: String,
        @PartMap fields: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part images: List<MultipartBody.Part>
    ): Venue

    @DELETE("venues/{id}")
    suspend fun deleteVenue(@Path("id") id: String): MessageResponse

    // Bookings
    @GET("bookings/owner/dashboard")
    suspend fun getDashboard(
        @Query("month") month: String? = null,
        @Query("venueId") venueId: String? = null
    ): DashboardStats

    @GET("bookings/venue/{venueId}")
    suspend fun getVenueBookings(@Path("venueId") venueId: String): List<Booking>

    @GET("bookings/venue/{venueId}/calendar")
    suspend fun getCalendar(
        @Path("venueId") venueId: String,
        @Query("month") month: String
    ): CalendarResponse

    @POST("bookings")
    suspend fun createBooking(@Body body: CreateBookingRequest): Booking

    @PUT("bookings/{id}/status")
    suspend fun updateBookingStatus(
        @Path("id") id: String,
        @Body body: StatusUpdateRequest
    ): Booking

    @Streaming
    @GET("bookings/owner/dashboard/export")
    suspend fun exportDashboard(
        @Query("format") format: String,
        @Query("month") month: String? = null,
        @Query("venueId") venueId: String? = null
    ): ResponseBody

    // Reviews
    @GET("reviews/venue/{venueId}")
    suspend fun getVenueReviews(@Path("venueId") venueId: String): List<Review>
}
```

---

## 11. UI/UX Requirements

### 11.1 Design
- Material 3, light theme (dark theme optional for Phase 2)
- Colors aligned with the existing web app
- Status badge colors matching web constants:
  - Pending → yellow
  - Confirmed → green
  - Cancelled → red

### 11.2 Formatting

```kotlin
// Price
fun formatPrice(amount: Double): String =
    NumberFormat.getNumberInstance(Locale("uz", "UZ")).format(amount) + " so'm"

// Date
fun formatDate(iso: String): String =
    Instant.parse(iso).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale("uz")))
```

### 11.3 Error Handling

| HTTP | User Message |
|------|--------------|
| 400 | Validation error (show server message) |
| 401 | "Session expired, please log in again" |
| 403 | "You don't have permission for this action" |
| 404 | "Data not found" |
| 500 | "Server error, please try again later" |
| Network | "No internet connection" |

### 11.4 Offline Behavior
The backend does not support offline mode. Cache last-loaded data for display only, with an "Offline" banner. All mutations require network.

---

## 12. Real-Time and Refresh Strategy

The backend has **no WebSocket, SSE, or push notifications**. Phase 1 solutions:

1. **Pull-to-refresh** on all list screens
2. **Optional polling** on Bookings screen — refresh every 60 s while screen is visible
3. **Pending badge** — show `pendingBookings` count from dashboard on app bar

Phase 2 (future): FCM push + backend WebSocket support.

---

## 13. Image Upload Implementation

```kotlin
suspend fun buildVenueParts(
    fields: VenueFormData,
    newImages: List<Uri>,
    keptImageUrls: List<String> = emptyList()
): Pair<Map<String, RequestBody>, List<MultipartBody.Part>> {
    val textParts = mapOf(
        "name" to fields.name.toRequestBody(),
        "description" to fields.description.toRequestBody(),
        // ... other fields
        "images" to keptImageUrls.toJson().toRequestBody("application/json".toMediaType())
    )
    val imageParts = newImages.map { uri ->
        val file = uriToCompressedFile(uri) // enforce max 5 MB
        MultipartBody.Part.createFormData(
            "images", file.name, file.asRequestBody("image/*".toMediaType())
        )
    }
    return textParts to imageParts
}
```

Uploaded images are served at:

```
http://api.toybron.uz/uploads/{filename}
```

---

## 14. Security

- Store JWT in `EncryptedSharedPreferences` or Android Keystore
- Do not log passwords; use OkHttp BODY logging only in debug builds
- Set `android:allowBackup="false"` or exclude token from backup
- HTTP is used for the backend; cleartext traffic is enabled.

---

## 15. Testing Plan

### 15.1 Unit Tests
- Validators (phone, telegram, password)
- Repository tests with mocked API
- ViewModel state transitions

### 15.2 Integration Tests
- Retrofit + MockWebServer for API responses
- Auth flow: login → token → protected endpoint

### 15.3 Manual Test Checklist

- [ ] Owner login and registration
- [ ] Client account login is rejected
- [ ] Full venue CRUD cycle
- [ ] Creating venue with fewer than 3 images fails
- [ ] Confirm and cancel bookings
- [ ] Create external booking
- [ ] Calendar displays correct day colors
- [ ] Excel/PDF export downloads successfully
- [ ] Expired token redirects to login
- [ ] App works with new backend URL
- [ ] Click-to-call intent works

---

## 16. Development Phases

### Phase 1 — Foundation (2 weeks)
- Project setup (Compose, Hilt, Retrofit)
- Auth (Login, Register, TokenStore)
- Bottom Navigation skeleton
- Backend connection

### Phase 2 — Venues (1.5 weeks)
- List, create, edit, delete
- Multipart image upload
- Moderation status display

### Phase 3 — Bookings (1.5 weeks)
- Bookings list + filters
- Confirm / cancel actions
- External booking modal
- Click-to-call

### Phase 4 — Dashboard (1 week)
- Statistics cards
- Charts (line, bar, pie)
- Calendar view
- Export functionality

### Phase 5 — Polish (1 week)
- Profile screen
- Reviews (read-only)
- Error handling, loading states
- UI polish, testing, APK build

**Estimated total: 6–7 weeks**

---

## 17. Deliverables

1. Android Studio project (Kotlin source code)
2. Debug and Release APK/AAB
3. README.md with:
   - Setup instructions
   - Backend URL configuration
   - Test account credentials
4. Screenshots (minimum 8 screens)
5. Short demo video (2–3 minutes)

---

## 18. Backend Limitations

These features **do not exist** in the backend — do not implement them in the app:

| Feature | Status |
|---------|--------|
| Profile update API | Not available |
| Password reset | Not available |
| Push notifications | Not available |
| WebSocket real-time | Not available |
| Menu / food orders | Not available |
| Payment integration | Not available |
| Refresh token | Not available (7-day JWT only) |

---

## 19. Web ↔ Android Feature Mapping

| Web Route | Android Screen | API |
|-----------|----------------|-----|
| `/dashboard` (Calendar tab) | Dashboard Tab 1 | calendar, POST bookings |
| `/dashboard` (Statistics tab) | Dashboard Tab 2 | dashboard, export |
| `/dashboard/bookings` | Bookings | venue bookings, status update |
| `/dashboard/venues` | Venues | owner/my, DELETE |
| `/dashboard/add-venue` | Add Venue | POST venues |
| `/dashboard/edit-venue/[id]` | Edit Venue | GET/PUT venues |
| `/dashboard/profile` | Profile | GET auth/me |
| `/login`, `/register` | Auth screens | auth/* |

---

## 20. Reference: Existing Codebase

| Concern | Path |
|---------|------|
| Backend entry & routes | `toyxona-backend/src/app.js` |
| Auth controller | `toyxona-backend/src/controllers/authController.js` |
| JWT middleware | `toyxona-backend/src/middleware/auth.js` |
| Role guard | `toyxona-backend/src/middleware/roleGuard.js` |
| Upload config | `toyxona-backend/src/middleware/upload.js` |
| Owner dashboard logic | `toyxona-backend/src/services/statsService.js` |
| Web owner dashboard | `toyxona-frontend/app/dashboard/` |
| Web API client | `toyxona-frontend/lib/api.ts` |
| Web types | `toyxona-frontend/lib/types.ts` |
| Web constants/labels | `toyxona-frontend/lib/constants.ts` |
| Venue form | `toyxona-frontend/components/VenueForm/VenueForm.tsx` |

---

## 21. Gradle Dependencies (Minimum)

```kotlin
dependencies {
    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.51")
    kapt("com.google.dagger:hilt-compiler:2.51")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Network
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Images
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Charts
    implementation("com.patrykandpatrick.vico:compose-m3:1.14.0")
}
```

---

## 22. Role-Based Access Summary

| Action | client | owner | admin |
|--------|--------|-------|-------|
| Browse approved venues | Yes | Yes | Yes |
| Create venue | No | Yes | No |
| Edit/delete own venue | No | Yes | Yes |
| View own venues (any status) | No | Yes | Yes |
| Manage venue bookings | No | Yes (own) | Yes |
| Confirm/cancel bookings | No | Yes (own venues) | Yes |
| Owner dashboard | No | Yes | No |
| Admin panel | No | No | Yes |

The Android app must enforce `role == owner` on login and block access to admin/client flows.

---

**Production Backend:** http://api.toybron.uz/api

**Document based on analysis of:** `toyxona-backend` and `toyxona-frontend` codebases.
