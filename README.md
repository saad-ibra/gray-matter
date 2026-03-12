# Gray Matter

Gray Matter is a personal knowledge management and deliberation platform designed for researchers, students, deep thinkers, and book readers. It provides a structured environment for deep reading, in-context annotation, and high-level topic synthesis.

Built with Kotlin Multiplatform (KMP), Gray Matter provides a seamless experience across Android and (coming soon) iOS, ensuring your intellectual assets are always accessible and **100% offline-first**.

---

## Core Philosophy: Forced Deliberation

We believe that passive collection is the enemy of actual knowledge. Gray Matter is built around an **Opinion-First** methodology:
*   **No mindless data collection:** When you add any resource (a PDF, a Markdown note or a Web Link), you are **required** to write an initial reflection (an "Opinion") and assign a confidence score to your preconceived notion about the file before saving.
*   **Logging Further Thoughts:** Further opinions can be added to the timeline. Deliberation is favoured as users are prompted to add opinion, whenever they create bookmarks or text annotations in pdf reader.
*   **Active synthesis:** Your library is not a dump of files; it's a curated collection of resources, categorized into topics. Thoughts and reflections on those resources are stored in a chronolgical timeline.

---

## Features

### 1. Fully Offline & Privacy-First Architecture
*   **What it does:** Everything runs locally on your device. There are no cloud accounts, no telemetry, and no external AI dependencies.
*   **Why it exists:** Your thoughts, research data, and personal libraries are profoundly private. An offline-only approach ensures absolute privacy, zero latency, and perpetual access regardless of your internet connection.
*   **How it works:** All metadata, opinions, and topic structures are stored purely locally using SQLDelight (a multi-platform SQLite implementation).

### 2. High-Performance PDF & Novel Reader
*   **What it does:** A robust document viewer optimized specifically for deep reading of novels, textbooks, and research papers.
*   **Why it exists:** To provide an uncompromised native reading experience where you can seamlessly transition between getting lost in a novel and actively reflecting on its themes without switching apps.
*   **How it works:** The Android implementation utilizes native rendering pipelines (e.g., `PdfRenderer`) paired with modern Jetpack Compose. It includes a custom coordinate mapping engine to allow precise text selection and highlight creation directly over the rendered pages.
*   **When to use:** When reading long-form PDFs, studying technical manuals, or enjoying a novel where you want to keep track of characters, key quotes, and chapter summaries.
*   **Supported Formats:** `PDF`, `Markdown (.md)`, `Images`, and `Web Links`.


### 3. In-Situ Annotation & Marginalia
*   **What it does:** Allows you to highlight text snippets within your documents and attach your reflections directly to that specific passage.
*   **Why it exists:** To anchor your thoughts to their exact source context, preventing the "why did I write this?" problem months later.
*   **How it works:** Selected text is extracted and stored as a blockquote alongside your new "Opinion" record, permanently bound to the specific page number of the resource.


### 4. Hierarchical Topic Synthesis
*   **What it does:** Organizes diverse resources (a mix of PDFs, local notes, and web links) into cohesive "Topics," allowing you to write an overarching summary or thesis for the entire subject.
*   **Why it exists:** To facilitate the transition from isolated reading to high-level understanding. Knowledge requires viewing multiple sources through the lens of a single thematic objective.
*   **How it works:** A flexible relational database model allows you to organize items into topics, accompanied by a built-in markdown editor for your "Overall Opinion."

### 5. Markdown Export Engine
*   **What it does:** Generates clean, formatted Markdown reports containing your reflections, highlighted quotes, and topic summaries.
*   **Why it exists:** To ensure you are never locked into the platform. Your knowledge can easily be exported to tools like Obsidian, Logseq, or directly published to the web.
*   **How it works:** The internal `ExportService` templates your offline database objects and annotations into structured `.md` files, complete with source citations and timestamps.

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
