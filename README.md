# AniTrack

A lightweight Android starter app for anime, manga and manhwa news and release tracking.

## Features
- News tab: RSS articles from Anime News Network and Crunchyroll News.
- Schedule tab: upcoming anime episodes from AniList and latest chapter uploads from MangaDex.
- Library tab: save items during the current app session.
- Article/title cards open their original source in the browser.
- GitHub Actions builds a debug APK automatically.

## Build an APK with GitHub
1. Create a new GitHub repository.
2. Upload all files and folders from this project to the repository root.
3. Open the **Actions** tab and allow workflows if GitHub asks.
4. Push to `main`, or choose **Build AniTrack APK** > **Run workflow**.
5. When the workflow finishes, open its run and download the `AniTrack-debug-apk` artifact.
6. Extract the downloaded ZIP. The APK is `app-debug.apk`.

## Data and limitations
- AniList provides anime airing schedule data. Airing times are displayed in the device's local timezone.
- MangaDex provides recent chapter uploads. These are not predictions of future chapters.
- Chapter release dates are only knowable ahead of time when a publisher/platform announces them. AniTrack does not fabricate future dates.
- RSS availability and API rate limits are controlled by their providers. A feed may occasionally be unavailable.
- Saved items currently remain in memory only and reset when the app process is restarted. Persistent storage and user-configurable follows are planned improvements.

## Tech
- Kotlin + Jetpack Compose
- AniList GraphQL API
- MangaDex API
- Publisher RSS feeds
