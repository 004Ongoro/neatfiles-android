# NeatFiles (Android) 📁✨

**NeatFiles** is an intelligent file manager and download tidier for Android phones built with **Modern Android (Jetpack Compose + Material You)** and **Clean Architecture**.

Unlike traditional file managers that treat files as dumb bytes, NeatFiles **understands file contents**, semantic naming structures, duplicate versions, and storage impacts—focused on keeping your **Downloads** folder spotless and organized.

---

## 🌟 Key Features

1. **Intelligent Downloads Health Dashboard**
   - Live metrics: e.g. **142 files**, Documents (38), Images (51), Installers (12), Archives (15), Media (14), Code (8), Others (4).
   - Storage breakdown with dynamic color palette.
   - Prominent **Safe to Remove** card: **31 files may be safe to remove (1.2 GB)** with `[ Review Cleanup ]` action.

2. **Smart Duplicate Detection**
   - Fast size pre-filter + chunked SHA-256 hash calculation.
   - Automatically designates the oldest file as the **Original** and redundant copies as **Safe to Remove**.

3. **Old & Stale File Detection**
   - Identifies downloads untouched past a configurable threshold (e.g. 14, 30, 60, 90 days).
   - Flags obsolete `.apk` installers (older than 7 days) and temp/incomplete `.crdownload` cache files.

4. **Deep Content & PDF / Image Inspection**
   - **PDFs**: Extracts embedded document title and page count without heavy native binaries.
   - **Images**: Lightweight bounds/dimension decoding and WhatsApp/Screenshot filename understanding.
   - **APKs**: Reads internal package archive metadata (package name, version code).

5. **Smart Renaming Engine**
   - Turns cryptic names into clean, readable filenames:
     - `Invoice_2024_Q1 (1).pdf` ➡️ `Invoice 2024 Q1.pdf`
     - `IMG-20240315-WA0004.jpeg` ➡️ `Photo_2024-03-15_WA0004.jpeg`
     - `Screenshot_20240915-182030_Chrome.png` ➡️ `Screenshot_Chrome_2024-09-15.png`
     - URL decodes messy encoded filenames (`My%20Document%2B2024.pdf`).

6. **One-Tap Auto-Organize**
   - Groups loose files in `Downloads/` into dedicated neat folders:
     - `Downloads/Documents/`
     - `Downloads/Images/`
     - `Downloads/Installers/`
     - `Downloads/Archives/`
     - `Downloads/Media/`

7. **Guided Onboarding & Permission Setup**
   - Step-by-step setup wizard explaining storage and notification permissions.
   - Live permission detection that dynamically updates when returning from Android Settings.
   - Initial preference configuration (retention periods, background check intervals).
   - Built-in sample test file generator to test real file operations immediately.

8. **Scheduled Background Cleanup (AndroidX WorkManager)**
   - Periodic background worker monitors Downloads clutter.
   - Posts actionable notifications when safe-to-remove files accumulate.

9. **Comprehensive Storage Analysis**
   - Device storage vs Downloads folder usage.
   - Largest downloads list and file age breakdown.

---

## 🏗️ Architecture

```
com.neatfiles.app
├── core/
│   ├── model/         # NeatFile, FileCategory, DuplicateGroup, StorageOverview, CleanupOverview
│   └── util/          # Formatters, HashEngine, ContentInspector, SmartRenameEngine
├── domain/
│   ├── repository/    # FileRepository, PreferencesRepository interfaces
│   └── usecase/       # ScanDownloads, DetectDuplicates, DetectCleanupCandidates, SmartRename, AutoOrganize
├── data/
│   ├── repository/    # FileRepositoryImpl, PreferencesRepositoryImpl, MockDataProvider
│   └── worker/        # ScheduledCleanupWorker (WorkManager periodic worker)
└── ui/
    ├── theme/         # Material 3 Color Schemes (Dynamic Color / Dark / Light), Typography
    ├── components/    # StorageBreakdownBar, CategoryCard, CleanupActionCard, NeatFileCard
    ├── screens/       # Onboarding, Dashboard, CleanupReview, CategoryDetail, SmartOrganizer, SmartRename, StorageAnalysis, Settings
    ├── viewmodel/     # MainViewModel, MainUiState, UiEvent
    └── MainActivity.kt
```

---

## 🚀 How to Run & Build

1. **Prerequisites**:
   - JDK 21 LTS
   - Android SDK (API 35, Build-Tools 34+)

2. **Build Debug APK**:
   ```bash
   ./gradlew assembleDebug
   ```
   The generated APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

3. **Automated CI/CD GitHub Releases**:
   - Pushing a version tag (e.g., `git tag v1.0.0 && git push origin v1.0.0`) automatically triggers the GitHub Actions workflow in `.github/workflows/release.yml`.
   - The workflow compiles the APK and publishes an official GitHub Release with release notes and the APK asset attached.
