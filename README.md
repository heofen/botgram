# Botgram

**Botgram** is a native Android application built with **Kotlin** and **Jetpack Compose** that functions as a Telegram Bot client. It allows users to authenticate using a Bot Token, manage chats, and handle various media types directly from an Android interface.

## 🚀 Features

- **Bot Authentication**: Secure login flow using Telegram Bot API tokens.
  - Validates tokens via the `getMe` endpoint.
  - Persists sessions securely using a local `TokenManager`.
- **Modern UI**: Fully built with **Jetpack Compose** following Material Design principles.
  - **Single Activity Architecture**: `MainActivity` orchestrates navigation.
  - **Screens**: Login, Chat List, and Group interfaces.
- **Advanced Media Handling**: Robust logic for parsing and displaying Telegram media messages:
  - Supports Photos, Videos, Animations, Audio/Voice, Video Notes, and Documents.
  - Automatic file extension detection based on MIME types (e.g., mapping `audio/ogg` to `.ogg`).
  - Thumbnail extraction for video and document previews.
- **Local Persistence**: Uses `AppDatabase` (Room) for caching chats, messages, and user data.
- **Background Services**: `GetUpdates` service for fetching new messages.

## 🛠 Tech Stack

- **Language**: Kotlin
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetbrains/compose)
- **Architecture**: MVVM (Model-View-ViewModel)
- **Telegram API**: 
  - [tgbotapi](https://github.com/InsanusMokrassar/TelegramBotAPI) (by dev.inmo) for core bot interactions.
  - Custom `OkHttp` client for initial token validation.
- **Networking**: Ktor & OkHttp.
- **Data Storage**: Android `SharedPreferences` (TokenManager) & Room Database.

## 📂 Project Structure

The project follows a clean architecture approach within the `com.heofen.botgram` package:

```
com.heofen.botgram
├── data
│   ├── local         # TokenManager, Database DAO
│   ├── repository    # ChatRepository, MessageRepository, UserRepository
│   └── MediaManager  # Media handling logic
├── ui
│   ├── screens       # Composable screens (Login, ChatList, Group)
│   └── theme         # BotgramTheme and styling
├── services          # Background services (GetUpdates)
└── database          # AppDatabase definitions
```

## 📱 Media Handling Logic

The application implements specific logic to handle Telegram's polymorphic content types. It includes helper functions to extract metadata:

- **File Extensions**: Automatically resolves extensions (e.g., `tgs` for animated stickers, `webm` for video stickers) when filenames are missing.
- **Thumbnails**: Extracts high-quality thumbnails from `PhotoContent`, `VideoContent`, and `DocumentContent`.
- **Metadata**: Parses duration, dimensions (width/height), and file IDs for efficient rendering.

## 🔧 Setup & Installation

1. **Clone the repository**:
   ```
   git clone https://github.com/yourusername/botgram.git
   ```
2. **Open in Android Studio**: Ensure you are using the latest version (Ladybug or newer recommended).
3. **Sync Gradle**: Download all dependencies including `tgbotapi`.
4. **Run the App**: Connect a device or start an emulator.

## 🔑 Usage

1. Launch the app.
2. If no session is found, you will see the **Login Screen**.
3. Paste your **Telegram Bot Token** (obtained from [@BotFather](https://t.me/BotFather)).
4. The app validates the token via `https://api.telegram.org/bot<TOKEN>/getMe`.
   - **Success**: The token is saved, and you are redirected to the Chat List.
   - **Failure**: An error message is displayed (e.g., "Network error" or "Invalid token").
