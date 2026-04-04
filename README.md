# Gray Matter

Gray Matter is a personal knowledge management and deliberation platform designed for researchers, students, deep thinkers, and book readers. It provides a structured environment for deep reading, in-context annotation, and high-level topic synthesis.

Built with Kotlin Multiplatform (KMP), Gray Matter provides a seamless experience across Android and (coming soon) iOS, ensuring your intellectual assets are always accessible and **100% offline-first**.

---

## Core Philosophy: Forced Deliberation

I believe that passive collection is the enemy of actual knowledge. Gray Matter is built around an **Opinion-First** methodology:
*   **No mindless data collection:** When you add any resource (a PDF, a Markdown note, or a Web Link), you are **required** to write an initial reflection (an "Opinion") and assign a confidence score before saving.
*   **Logging Further Thoughts:** Further opinions are added to a beautiful, unified timeline. Deliberation is prioritized as users are prompted for reflections whenever they create bookmarks or annotations.
*   **Active Synthesis:** Your library is not a file dump; it's a curated collection of **Resource Entries[files/urls/notes]**, categorized into topics[folders]. Every insight is tracked chronologically on a timeline.
*   **Relatrix[Knowledge Connections]:** Create a rich web of interconnected ideas. Seamlessly reference other resources, topics, or specific insights within your opinions to build a network of bi-directional knowledge. See the 3D visual representaion of these connected ideas in the Relatrix view. 
---

## Feature Highlights

*   **Relatrix Knowledge Graph:** See the "connective tissue" of your thoughts. Relatrix is a dynamic 3D visual map that shows how your resources and insights are linked together, helping you discover non-obvious connections across your library.
*   **Core Knowledge Artifacts:** Move beyond passive highlighting. Gray Matter captures knowledge in four distinct forms:
    *   **Opinions:** Your personal reflections and deliberation on any resource.
    *   **Annotations:** Context-specific highlights and notes precisely anchored to text in your documents.
    *   **Bookmarks:** Significant milestones in your reading journey with attached thoughts.
    *   **Templates:** Customizable structured frameworks (like "Scientific Review" or "Decision Ledger") to guide your analytical process.
*   **Multi-Format Resource Support:** Track insights across **Web Articles**, **PDF Documents**, and native **Markdown Notes**—all unified in a single intellectual system.
*   **Unified Timeline:** A modern, animated interface in the Resource Detail screen that brings together your opinions, bookmarks, and annotations into a single chronological narrative.
*   **Offline Global Search:** Instant, full-text search across all resources, opinions, and topics—completely offline.
*   **Smart Background Cleanup:** Integrated workers that automatically manage storage integrity and ensure your library stays lean and consistent.

---

## Technical Architecture

Gray Matter is architected for stability, local performance, and cross-platform consistency.

### Module Breakdown
*   `shared`: The core engine of the application.
    *   `domain`: Pure business logic and entities (`Topic`, `Resource`, `ResourceEntry`, `Opinion`).
    *   `data`: Implementation of offline-first repositories and local persistence using SQLDelight.
    *   `business`: High-level services including `LocalSearchEngine`, `RelatrixGraph` logic, and `ExportService`.
*   `androidApp`: The native Android client.
    *   Built entirely with Jetpack Compose.
    *   Features custom PDF rendering, paged navigation, and precise text selection.
    *   Uses WorkManager for background data maintenance.
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
