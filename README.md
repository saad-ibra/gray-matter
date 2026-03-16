# Gray Matter

Gray Matter is a personal knowledge management and deliberation platform designed for researchers, students, deep thinkers, and book readers. It provides a structured environment for deep reading, in-context annotation, and high-level topic synthesis.

Built with Kotlin Multiplatform (KMP), Gray Matter provides a seamless experience across Android and (coming soon) iOS, ensuring your intellectual assets are always accessible and **100% offline-first**.

---

## Core Philosophy: Forced Deliberation

We believe that passive collection is the enemy of actual knowledge. Gray Matter is built around an **Opinion-First** methodology:
*   **No mindless data collection:** When you add any resource (a PDF, a Markdown note or a Web Link), you are **required** to write an initial reflection (an "Opinion") and assign a confidence score to your preconceived notion about the file before saving.
*   **Logging Further Thoughts:** Further opinions can be added to the timeline. Deliberation is favoured as users are prompted to add opinion, whenever they create bookmarks or text annotations in pdf reader.
*   **Active synthesis:** Your library is not a dump of files; it's a curated collection of resources, categorized into topics. Opinions, bookmarks and annotations of those resources are stored in a chronolgical timeline.

---

## Technical Architecture

Gray Matter is architected for stability, local performance, and cross-platform consistency.

### Module Breakdown
*   `shared`: The core engine of the application.
    *   `domain`: Contains pure business logic and entities (`Topic`, `Resource`, `Opinion`, `ReadingProgress`).
    *   `data`: Implementation of offline-first repositories and local persistence using SQLDelight.
    *   `business`: High-level services including the `ExportService` and local search engines.
*   `androidApp`: The native Android client.
    *   Built entirely with Jetpack Compose.
    *   Uses Hilt for dependency injection.
    *   Features custom PDF rendering, paged navigation, and precise text selection over canvas elements.
*   `iosApp`: (In Development) The native iOS client built with SwiftUI.

### Technology Stack
*   **Language:** Kotlin (Multiplatform)
*   **UI Framework:** Jetpack Compose (Android), SwiftUI (iOS)
*   **Database:** SQLDelight (Multi-platform SQLite)
*   **Concurrency:** Kotlin Coroutines & Flow

---

## Getting Started

### Prerequisites
*   Android Studio Ladybug or later.
*   JDK 17+.

### Installation
1.  Clone the repository:
    ```bash
    git clone https://github.com/your-repo/gray-matter.git
    ```
2.  Open the project in Android Studio.
3.  Sync Gradle and run the `androidApp` configuration.

---

## Future Roadmap: iOS Integration
A primary development goal is achieving 100% feature parity on the iOS platform using Swift and SwiftUI.
