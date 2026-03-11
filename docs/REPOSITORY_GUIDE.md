# Botgram Repository Guide

## What This Repository Contains

`Botgram` is a native Android application that lets a user work with a Telegram bot account through a mobile UI. The app is built with Kotlin, Jetpack Compose, Room, and a custom HTTP client for Telegram Bot API.

At runtime the app does four main things:

1. Stores a bot token locally and validates it against Telegram.
2. Starts a foreground service that long-polls Telegram for updates.
3. Saves chats, users, messages, and downloaded media metadata into Room.
4. Renders chats and messages through Compose screens backed by ViewModels and repositories.

## High-Level Architecture

The project is a single Android app module: [`app`](/home/heofen/AndroidStudioProjects/botgram/app).

Main layers:

- `ui`: Compose screens, top bars, message/chat cells, and screen ViewModels.
- `data/remote`: Telegram gateway abstraction plus HTTP implementation over Telegram Bot API.
- `data/repository`: Application-facing data access for chats, messages, and users.
- `database`: Room database, entities, converters, and DAO interfaces.
- `services`: Background foreground service that receives Telegram updates continuously.
- `utils`: DTO-to-database mappers and small extension helpers.

The app is not using a DI framework in practice, even though `koin-androidx-compose` exists in dependencies. Objects are wired manually in [`MainActivity.kt`](/home/heofen/AndroidStudioProjects/botgram/app/src/main/java/com/heofen/botgram/MainActivity.kt).

## Runtime Flow

### 1. App launch

Entry point: [`MainActivity.kt`](/home/heofen/AndroidStudioProjects/botgram/app/src/main/java/com/heofen/botgram/MainActivity.kt)

Startup sequence:

1. `TokenManager` reads the saved bot token from local storage.
2. If there is no token, the app renders `LoginScreen`.
3. If there is a token, the app starts `GetUpdates` as a foreground service.
4. `MainActivity` builds:
   - `AppDatabase`
   - `TelegramGateway`
   - `MediaManager`
   - `ChatRepository`
   - `MessageRepository`
   - `UserRepository`
5. Navigation starts on the chat list screen.

### 2. Login flow

UI: [`LoginScreen.kt`](/home/heofen/AndroidStudioProjects/botgram/app/src/main/java/com/heofen/botgram/ui/screens/login/LoginScreen.kt)

Validation logic lives directly in `MainActivity`:

- An `OkHttpClient` sends `GET https://api.telegram.org/bot<TOKEN>/getMe`.
- If Telegram returns `ok = true`, the token is persisted through `TokenManager`.
- The activity restarts itself to rebuild the application in authenticated mode.

This means login is currently not handled through a repository/use-case layer; it is embedded in the activity.

### 3. Background updates

Service: [`GetUpdates.kt`](/home/heofen/AndroidStudioProjects/botgram/app/src/main/java/com/heofen/botgram/services/GetUpdates.kt)

The service is central to the repository behavior:

- Runs as a foreground `dataSync` service.
- Reads the current token from `TokenManager`.
- Creates a `TelegramGateway`.
- Enters a retry loop around `collectUpdates`.
- Waits 5 seconds and retries if polling crashes.

For every incoming Telegram update:

1. The service converts the update to `TelegramIncomingMessage`.
2. The related chat is upserted into Room.
3. The sender user is upserted into Room when present.
4. The message is inserted into Room.
5. Media download is launched asynchronously for messages with downloadable files.

This service is what keeps local state fresh. The UI mostly observes Room and reacts to those inserts.

### 4. Chat list screen

Files:

- [`ChatListViewModel.kt`](/home/heofen/AndroidStudioProjects/botgram/app/src/main/java/com/heofen/botgram/ui/screens/chatlist/ChatListViewModel.kt)
- [`ChatListScreen.kt`](/home/heofen/AndroidStudioProjects/botgram/app/src/main/java/com/heofen/botgram/ui/screens/chatlist/ChatListScreen.kt)

Behavior:

- The screen observes `chatRepository.getAllChats()`.
- Search switches the source flow to `searchChats(query)`.
- The first visible chats trigger lazy avatar loading through `chatRepository.loadAvatarIfMissing`.
- Tapping a chat navigates to `group/{chatId}`.
- Logout clears the token, stops `GetUpdates`, and restarts the activity.

### 5. Group/chat screen

Files:

- [`GroupViewModel.kt`](/home/heofen/AndroidStudioProjects/botgram/app/src/main/java/com/heofen/botgram/ui/screens/group/GroupViewModel.kt)
- [`GroupScreen.kt`](/home/heofen/AndroidStudioProjects/botgram/app/src/main/java/com/heofen/botgram/ui/screens/group/GroupScreen.kt)

Behavior:

- Loads the current chat once from `ChatRepository`.
- Subscribes to `MessageRepository.getChatMessages(chatId)`.
- Reverses the database order for `LazyColumn(reverseLayout = true)`.
- Resolves sender users from `UserRepository`.
- Triggers lazy download of missing message media.
- Triggers lazy loading of missing user avatars.
- Sends text messages through `MessageRepository.sendTextMessage`.
- Updates the chat preview after a successful outgoing send.

UI specifics:

- Messages are grouped visually if they belong to the same sender, same direction, same day, and are within 5 minutes.
- Date dividers are inserted between message days.
- The same screen is used for private chats and groups, with small rendering differences.

## Telegram Integration

Gateway abstraction: [`TelegramGateway.kt`](/home/heofen/AndroidStudioProjects/botgram/app/src/main/java/com/heofen/botgram/data/remote/TelegramGateway.kt)

Concrete implementation: [`HttpTelegramGateway.kt`](/home/heofen/AndroidStudioProjects/botgram/app/src/main/java/com/heofen/botgram/data/remote/telegramapi/HttpTelegramGateway.kt)

Low-level client: [`TelegramBotApiClient.kt`](/home/heofen/AndroidStudioProjects/botgram/app/src/main/java/com/heofen/botgram/data/remote/telegramapi/TelegramBotApiClient.kt)

Responsibilities are split like this:

- `TelegramBotApiClient`: raw HTTP requests, JSON parsing, DTO parsing, exception shaping, file download.
- `HttpTelegramGateway`: converts Telegram DTOs into app-level models and maintains `nextUpdateOffset`.
- `TelegramGatewayFactory`: creates the gateway used by the activity and the service.

Implemented Telegram operations:

- `getUpdates` with long polling
- `sendMessage`
- `getFile`
- `getUserProfilePhotos`
- `getChat`
- file download via Telegram file endpoint

### Message parsing

`HttpTelegramGateway` converts Telegram `MessageDto` to app model `TelegramIncomingMessage` and determines:

- message type
- file extension
- local media metadata
- thumbnail ids
- sender/chat preview information

Supported content classes in current code:

- text
- photo
- video
- animation
- audio
- voice
- video note
- document
- sticker
- animated sticker
- video sticker
- contact
- location

Unsupported or not yet modeled message kinds will fall back to `TEXT`.

## Data Model

Core enums: [`Types.kt`](/home/heofen/AndroidStudioProjects/botgram/app/src/main/java/com/heofen/botgram/Types.kt)

### Room database

Database: [`AppDatabase.kt`](/home/heofen/AndroidStudioProjects/botgram/app/src/main/java/com/heofen/botgram/database/AppDatabase.kt)

Entities:

- [`Chat.kt`](/home/heofen/AndroidStudioProjects/botgram/app/src/main/java/com/heofen/botgram/database/tables/Chat.kt)
- [`Message.kt`](/home/heofen/AndroidStudioProjects/botgram/app/src/main/java/com/heofen/botgram/database/tables/Message.kt)
- [`User.kt`](/home/heofen/AndroidStudioProjects/botgram/app/src/main/java/com/heofen/botgram/database/tables/User.kt)

Stored objects:

- `Chat`: Telegram chat identity, type, title/name fields, last message preview, avatar metadata.
- `Message`: message identity, sender, timestamps, reply links, file metadata, local file path, edit state, outgoing flag.
- `User`: sender identity, names, avatar metadata, private messaging capability.

### DAO layer

DAO files:

- [`ChatDao.kt`](/home/heofen/AndroidStudioProjects/botgram/app/src/main/java/com/heofen/botgram/database/dao/ChatDao.kt)
- [`MessageDao.kt`](/home/heofen/AndroidStudioProjects/botgram/app/src/main/java/com/heofen/botgram/database/dao/MessageDao.kt)
- [`UserDao.kt`](/home/heofen/AndroidStudioProjects/botgram/app/src/main/java/com/heofen/botgram/database/dao/UserDao.kt)

Important queries:

- chats are ordered by `lastMessageTime DESC`
- chat messages are loaded in `timestamp ASC`
- media can be reused across messages through `fileUniqueId`
- avatar cache reuse is supported through `avatarFileUniqueId`

### Mapping layer

Mapping helpers live in [`Mappers.kt`](/home/heofen/AndroidStudioProjects/botgram/app/src/main/java/com/heofen/botgram/utils/Mappers.kt).

They translate app-level Telegram models into Room entities:

- `TelegramIncomingMessage -> Message`
- `TelegramChat -> Chat`
- `TelegramUser -> User`

## Repository Layer

Files:

- [`ChatRepository.kt`](/home/heofen/AndroidStudioProjects/botgram/app/src/main/java/com/heofen/botgram/data/repository/ChatRepository.kt)
- [`MessageRepository.kt`](/home/heofen/AndroidStudioProjects/botgram/app/src/main/java/com/heofen/botgram/data/repository/MessageRepository.kt)
- [`UserRepository.kt`](/home/heofen/AndroidStudioProjects/botgram/app/src/main/java/com/heofen/botgram/data/repository/UserRepository.kt)

These repositories are thin wrappers around DAO access plus media side effects.

### ChatRepository

- exposes all chats and chat search
- updates last-message preview
- loads chat avatars on demand

### MessageRepository

- exposes chat messages and media groups
- inserts and updates messages
- sends text messages through the gateway
- downloads media on demand
- deduplicates downloads by `fileUniqueId`
- limits concurrent downloads with a semaphore of 3

### UserRepository

- loads/upserts users
- loads user avatars on demand
- reuses cached avatar downloads

## Media and File Caching

Media wrapper: [`MediaManager.kt`](/home/heofen/AndroidStudioProjects/botgram/app/src/main/java/com/heofen/botgram/data/MediaManager.kt)

Downloaded files are stored under the app cache directory:

- `cacheDir/avatars/...`
- `cacheDir/media/...`

Caching strategy:

- if a file already exists locally, reuse it
- if another message already downloaded the same `fileUniqueId`, reuse its `fileLocalPath`
- avatar downloads use the same idea through `avatarFileUniqueId`

This means media is cached opportunistically and tied to cache storage, not long-term durable app storage.

## Navigation and UI Composition

Navigation is created directly in [`MainActivity.kt`](/home/heofen/AndroidStudioProjects/botgram/app/src/main/java/com/heofen/botgram/MainActivity.kt).

Routes:

- `chat_list`
- `group/{chatId}`

Important UI component area:

- [`app/src/main/java/com/heofen/botgram/ui/components`](/home/heofen/AndroidStudioProjects/botgram/app/src/main/java/com/heofen/botgram/ui/components)

These components handle:

- top app bars
- chat rows
- grouped message bubbles
- message input
- date dividers

Visual style:

- Material 3
- Compose-only UI
- `Haze` is used for blurred top bar/source effects

## Persistence of Authentication

Token storage class: [`TokenManager.kt`](/home/heofen/AndroidStudioProjects/botgram/app/src/main/java/com/heofen/botgram/data/local/TokenManager.kt)

The token is stored locally and used by both:

- `MainActivity` for direct-login bootstrap
- `GetUpdates` for service restart/bootstrap

Logging out clears the token and stops the polling service.

## Android Platform Integration

Manifest: [`AndroidManifest.xml`](/home/heofen/AndroidStudioProjects/botgram/app/src/main/AndroidManifest.xml)

Declared pieces:

- internet permission
- foreground service permission
- foreground data sync service permission
- `GetUpdates` foreground service
- `MainActivity`
- `FileProvider`

The service is marked `exported="true"`, which is worth reviewing if no external app needs to start it.

## Build and Dependencies

Project config:

- root build: [`build.gradle.kts`](/home/heofen/AndroidStudioProjects/botgram/build.gradle.kts)
- app module build: [`app/build.gradle.kts`](/home/heofen/AndroidStudioProjects/botgram/app/build.gradle.kts)
- version catalog: [`libs.versions.toml`](/home/heofen/AndroidStudioProjects/botgram/gradle/libs.versions.toml)

Important stack from the current build:

- Android Gradle Plugin `8.13.2`
- Kotlin `2.2.20`
- Java/Kotlin target `17`
- compileSdk `36`
- minSdk `29`
- Jetpack Compose
- Room
- Navigation Compose
- OkHttp/Ktor dependencies
- Coil
- Haze

Notable detail: some libraries are present but not visibly used everywhere yet, especially `Koin`.

## Repository Structure

Top-level layout:

```text
.
├── app
│   └── src/main/java/com/heofen/botgram
│       ├── data
│       ├── database
│       ├── services
│       ├── ui
│       ├── utils
│       ├── MainActivity.kt
│       └── Types.kt
├── gradle
├── README.md
└── docs
    └── REPOSITORY_GUIDE.md
```

## Main Data Flows

### Incoming message flow

```text
Telegram Bot API
  -> TelegramBotApiClient.getUpdates()
  -> HttpTelegramGateway.collectUpdates()
  -> GetUpdates.handleMessage()
  -> mapper functions
  -> Room (chat/user/message)
  -> ViewModel Flow collection
  -> Compose UI recompose
```

### Outgoing message flow

```text
GroupScreen
  -> GroupViewModel.sendMessage()
  -> MessageRepository.sendTextMessage()
  -> TelegramGateway.sendTextMessage()
  -> Telegram Bot API sendMessage
  -> returned message saved in Room
  -> chat preview updated
  -> UI reflects new state
```

### Media flow

```text
Message visible in UI or received in background
  -> repository checks file/path cache
  -> MediaManager
  -> TelegramGateway.downloadFile()
  -> Telegram getFile + file download
  -> local cache file path saved in Room
  -> UI loads local file
```

## Current Constraints and Technical Debt

Observed directly from the repository:

- Dependency wiring is manual in `MainActivity`, so lifecycle and testability are limited.
- Login validation is implemented inside the activity instead of a separate data/domain layer.
- There are almost no meaningful tests yet; only template test files exist.
- Room schema version is `1` and there are no migrations yet.
- The app currently focuses on text/media reception and text sending; broader bot interactions are not modeled.
- Service restart/recovery logic is simple retry with fixed delay.
- Media is stored in cache, so Android may evict files at any time.
- Some dependencies appear unused or only partially integrated.
- `GetUpdates` and `MainActivity` both create gateway/repository graphs independently.

## How To Reason About Changes In This Repo

If you need to change behavior, the likely entry points are:

- authentication/bootstrap: [`MainActivity.kt`](/home/heofen/AndroidStudioProjects/botgram/app/src/main/java/com/heofen/botgram/MainActivity.kt)
- background sync/polling: [`GetUpdates.kt`](/home/heofen/AndroidStudioProjects/botgram/app/src/main/java/com/heofen/botgram/services/GetUpdates.kt)
- Telegram transport/parsing: [`HttpTelegramGateway.kt`](/home/heofen/AndroidStudioProjects/botgram/app/src/main/java/com/heofen/botgram/data/remote/telegramapi/HttpTelegramGateway.kt)
- network client behavior/errors: [`TelegramBotApiClient.kt`](/home/heofen/AndroidStudioProjects/botgram/app/src/main/java/com/heofen/botgram/data/remote/telegramapi/TelegramBotApiClient.kt)
- persisted models and queries: [`app/src/main/java/com/heofen/botgram/database`](/home/heofen/AndroidStudioProjects/botgram/app/src/main/java/com/heofen/botgram/database)
- chat list behavior: [`ChatListViewModel.kt`](/home/heofen/AndroidStudioProjects/botgram/app/src/main/java/com/heofen/botgram/ui/screens/chatlist/ChatListViewModel.kt)
- message screen behavior: [`GroupViewModel.kt`](/home/heofen/AndroidStudioProjects/botgram/app/src/main/java/com/heofen/botgram/ui/screens/group/GroupViewModel.kt)

## Short Summary

This repository is an Android Telegram bot client with a simple but clear architecture:

- `MainActivity` bootstraps authenticated or unauthenticated app state.
- `GetUpdates` is the continuous sync engine.
- `TelegramGateway` adapts Telegram Bot API into app-friendly models.
- Room is the source of truth for UI state.
- Compose screens observe repository data and trigger lazy avatar/media loading.

If you need onboarding documentation for developers, this file should be the primary starting point.
