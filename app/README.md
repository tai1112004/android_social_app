# Social Messaging Android App

Android client for social messaging application with real-time WebSocket support.

## Stack
- **Language**: Java
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34
- **Build Tool**: Gradle
- **Architecture**: MVVM + Repository Pattern

## Project Structure

```
app/src/main/
├── java/com/yourapp/
│   ├── ui/
│   │   ├── auth/        # Login, Register screens
│   │   ├── home/        # Home feed
│   │   ├── chat/        # Conversations, Chat room
│   │   └── profile/     # User profile, settings
│   ├── data/
│   │   ├── remote/      # Retrofit API interfaces
│   │   ├── local/       # Room database
│   │   └── repository/  # Repository pattern layer
│   ├── model/           # Data classes (User, Message, etc.)
│   ├── network/         # WebSocket client
│   ├── util/            # Constants, helpers
│   └── MainActivity.java
├── res/
│   ├── layout/          # XML layouts
│   ├── values/          # Strings, colors, themes
│   └── mipmap/          # App icons
└── AndroidManifest.xml
```

## Key Dependencies

| Dependency | Version | Purpose |
|---|---|---|
| Retrofit | 2.9.0 | HTTP REST client |
| OkHttp | 4.11.0 | HTTP logging |
| Lifecycle | 2.6.1 | ViewModel, LiveData |
| Navigation | 2.7.1 | Fragment navigation |
| Material Components | 1.9.0 | Material Design UI |
| Glide | 4.15.1 | Image loading |
| Room | 2.5.2 | Local database |
| NV WebSocket | 2.14 | WebSocket client |
| Android Security | 1.1.0-alpha06 | Encrypted SharedPreferences |

## Constants

**API Endpoints:**
```
BASE_URL = "http://10.0.2.2:8080/api/"
WS_URL = "ws://10.0.2.2:8080/ws"
```

**SharedPreferences Keys:**
```
TOKEN_KEY = "access_token"
REFRESH_TOKEN_KEY = "refresh_token"
USER_ID_KEY = "user_id"
USERNAME_KEY = "username"
```

## Permissions

- `INTERNET` — API communication
- `ACCESS_NETWORK_STATE` — Network availability check
- `CAMERA` — Profile picture capture
- `READ_EXTERNAL_STORAGE` — Pick images
- `WRITE_EXTERNAL_STORAGE` — Save images

## Build & Run

### Prerequisites
- Android Studio 2023.1+
- JDK 17+
- Android SDK 34 installed

### Build
```bash
./gradlew build
```

### Run
```bash
./gradlew installDebug
# Or open in Android Studio and run
```

## Configuration

### Network
- Base API URL: `http://10.0.2.2:8080/api/` (emulator)
- WebSocket: `ws://10.0.2.2:8080/ws`
- Timeout: 30 seconds (connect, read, write)
- Cleartext traffic enabled for emulator testing

### Security
- JWT stored in encrypted SharedPreferences
- Tokens: Access (15 min), Refresh (7 days)
- Authorization header: `Bearer {token}`

### Database
- Room local cache: `social_app.db`
- Used for offline message caching

## Architecture

**MVVM Pattern:**
- **Model**: Data classes, repositories
- **View**: Fragments, Activities
- **ViewModel**: LiveData, state management

**Data Flow:**
```
UI Layer (Activities/Fragments)
    ↕ (Observe LiveData)
ViewModel Layer
    ↕ (Request/Response)
Repository Layer
    ├─→ Remote (Retrofit API)
    └─→ Local (Room Database)
```

## Next Implementation Steps

1. Implement model classes (User, Message, Conversation)
2. Create Retrofit API interfaces
3. Implement repositories
4. Build authentication screens (Login/Register)
5. Implement WebSocket client
6. Create chat/messaging UI
7. Add navigation flow

## Notes

- This is a skeleton project - no UI logic implemented yet
- All network configuration uses emulator IP (10.0.2.2)
- Ready for incremental feature development
- For physical device testing, replace 10.0.2.2 with actual backend IP
