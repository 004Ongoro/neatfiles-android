# NeatFiles (Android)

NeatFiles is an intelligent file manager and download tidier for Android devices built with Modern Android (Jetpack Compose, Material You) and Clean Architecture.

Unlike conventional file managers that treat files as raw byte streams, NeatFiles understands file semantics, duplicate versions, naming conventions, and storage impact, with a core focus on maintaining an organized and uncluttered Downloads directory.

---

## Key Features

1. Intelligent Downloads Health Dashboard
   - Live metrics: Total downloads file count, category distribution (Documents, Images, Installers, Archives, Media, Code, Others).
   - Dynamic storage breakdown visualization.
   - Safe to Remove detection with one-tap cleanup review.

2. Smart Duplicate Detection
   - Fast size pre-filtering coupled with chunked SHA-256 hash validation.
   - Automatically preserves the oldest file as the Original and marks redundant copies for safe removal.

3. Stale File Detection
   - Configurable retention thresholds (14, 30, 60, 90 days) for untouched files.
   - Flags obsolete APK installers (older than 7 days) and incomplete cache files (.crdownload, .tmp).

4. In-App File Inspection & Viewing
   - Documents and PDFs: Embedded title and page count inspection.
   - Images: Fast dimension decoding and in-app image viewer.
   - Text & Code: In-app preview viewer for text, logs, scripts, and markdown.
   - Installers (APKs): Package archive metadata inspection.
   - System File Viewer: Direct opening via Android FileProvider integration.

5. Smart Renaming Engine
   - Strips redundant duplicate tags like (1) or (copy).
   - Normalizes camera, WhatsApp, and screenshot naming structures into clear, human-readable titles.
   - Automatically decodes URL-encoded filenames.

6. One-Tap Auto-Organization
   - Relocates loose root files into designated category folders:
     - Downloads/Documents/
     - Downloads/Images/
     - Downloads/Installers/
     - Downloads/Archives/
     - Downloads/Media/
   - Robust fallback mechanisms ensuring zero file loss during moves.

7. Mandatory Storage Permission Gate & Setup Wizard
   - Transparent onboarding process guiding users through All Files Access and optional notification permissions.
   - Strictly requires storage permissions before app operation to guarantee data integrity.
   - Resumes automatically when permissions are granted in system settings.

8. Scheduled Background Cleanup
   - AndroidX WorkManager periodic background worker monitors clutter growth.
   - Dispatches actionable notifications when cleanup candidates accumulate.

9. Storage Analysis & Third-Party Licenses
   - Device vs. Downloads storage utilization.
   - Open source software notices and license disclosures.

---

## Architecture

The project adheres to Clean Architecture principles:

- `core/`: Immutable data models (NeatFile, FileCategory, CleanupOverview), formatting utilities, SHA-256 hashing engine, and file inspection helpers.
- `domain/`: Repository contracts (FileRepository, PreferencesRepository) and isolated use cases (ScanDownloads, DetectDuplicates, DetectCleanupCandidates, SmartRename, AutoOrganize, PerformCleanup).
- `data/`: Concrete implementations, storage I/O, DataStore preferences, and WorkManager background workers.
- `ui/`: Modern Jetpack Compose UI with Material 3 theming, state-driven navigation, and animated feedback components.

---

## How to Build & Run

### Prerequisites
- JDK 21 LTS
- Android SDK (API 35, Build-Tools 34+)

### Build Debug APK
```bash
./gradlew assembleDebug
```
The compiled APK will be output to:
`app/build/outputs/apk/debug/app-debug.apk`

### Run Tests
```bash
./gradlew test
```

### Automated CI/CD Releases
- Pushing a version tag (`v*`) triggers the GitHub Actions release workflow in `.github/workflows/release.yml`.
- The pipeline builds the application, packages the debug APK, and publishes a formal GitHub Release.
