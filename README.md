# Botgram

Botgram is a native Android application written in Kotlin and Jetpack Compose that works as a Telegram bot client. The app validates a bot token, starts a foreground long-polling service, stores chats/messages/users in Room, and renders them through Compose screens.

## Main points

- login by Telegram bot token via `getMe`
- foreground service for `getUpdates`
- local Room cache for chats, users, and messages
- lazy avatar and media downloading into app cache
- Compose UI with screens for login, chat list, and chat/group view

## Stack

- Kotlin
- Jetpack Compose
- Room
- OkHttp
- Navigation Compose
- Coil

## Where To Start

- entry point: [`MainActivity.kt`](/home/heofen/AndroidStudioProjects/botgram/app/src/main/java/com/heofen/botgram/MainActivity.kt)
- background sync: [`GetUpdates.kt`](/home/heofen/AndroidStudioProjects/botgram/app/src/main/java/com/heofen/botgram/services/GetUpdates.kt)
- Telegram transport: [`HttpTelegramGateway.kt`](/home/heofen/AndroidStudioProjects/botgram/app/src/main/java/com/heofen/botgram/data/remote/telegramapi/HttpTelegramGateway.kt)
- full repository documentation: [`docs/REPOSITORY_GUIDE.md`](/home/heofen/AndroidStudioProjects/botgram/docs/REPOSITORY_GUIDE.md)

## Run

1. Open the project in Android Studio.
2. Sync Gradle.
3. Run the `app` configuration on a device or emulator.
4. Enter a Telegram bot token from [@BotFather](https://t.me/BotFather).

## Documentation

Detailed documentation for the repository structure, runtime flows, data model, and technical constraints is here:

[`docs/REPOSITORY_GUIDE.md`](/home/heofen/AndroidStudioProjects/botgram/docs/REPOSITORY_GUIDE.md)
