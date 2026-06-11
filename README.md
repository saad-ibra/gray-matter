# <div align="center">

<br/>

# 𝗥𝗲𝗹𝗮𝘁𝗿𝗶𝘅

**An active knowledge‑management and multi‑modal note‑taking platform for deep thinkers.**

*Stop collecting. Start reflecting.*

<br/>

[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Language](https://img.shields.io/badge/Language-Kotlin%20Multiplatform-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/docs/multiplatform.html)
[![UI](https://img.shields.io/badge/UI-Jetpack_Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/compose)
[![Database](https://img.shields.io/badge/Database-SQLDelight%20%2B%20SQLCipher-003B57?style=for-the-badge&logo=sqlite&logoColor=white)](https://github.com/cashapp/sqldelight)
[![Offline](https://img.shields.io/badge/Data-100%25_Offline_First-2ECC71?style=for-the-badge)](/)
[![iOS](https://img.shields.io/badge/iOS-Coming_Soon-999999?style=for-the-badge&logo=apple&logoColor=white)](/)

<br/>

![Hero Banner](screenshots/infographics/hero_banner.png)

<br/>

---

## The Problem

Most knowledge‑management tools are glorified bookmarks. You gather articles, highlight text, save links, and then forget everything.

**Relatrix** changes that. Built on a single, uncompromising philosophy:

> **Information only becomes knowledge when you engage with it through active reflection.**

Every resource you save is a starting point, not a destination. Relatrix provides a multi‑modal toolkit to break down, question, and connect information across diverse formats.

---

## ✨ Feature Overview

<br/>

![Feature Strip](screenshots/infographics/feature_strip.png)

<br/>

### 📚 Topic Library
Organise your intellectual world into **Topics**: curated folders of resources. Each topic has an overview, holds web links, PDFs, and markdown notes, and surfaces all connected resources at a glance. Full‑text offline search across everything.

### 🗂 Multi‑Modal Resource Timeline
Every resource lives in a rich **Timeline** view: a single chronological feed that unifies your reflections, annotations, and visual captures. Watch your thinking evolve from first impression to deep mastery through a diverse range of entry types.

### ✍️ Reflection‑First Entry
When you save a PDF, web page, or markdown note, Relatrix encourages you to capture an **Opinion**, **Annotation**, or **Visual Note** immediately. This deliberate friction prevents "hoarding" and ensures you actually process what you consume.

### 🔗 Knowledge Links
While writing any entry, reference other resources, topics, or insights using `[[double‑bracket]]` wiki‑style links. These cross‑references form a bidirectional backlink graph — the raw data for Relatrix.

---

## 📖 PDF Reader & Annotation 

The built‑in PDF reader is a workspace for active reading. You can:
- **Flip pages** with smooth gestures and multiple navigation methods.
- **Highlight passages** and instantly turn them into **Annotations** (contextual snapshots) with your own added thoughts.
- **Create Bookmarks** that store not just a page number, but your confidence level and a summary of that section.
- **Visual Captures (Vision)**: Attach photos of handwritten notes or diagrams directly to the document timeline.
- **Dictionary Lookups**: Mark specific terms to build a global glossary across your entire library.

---

## 🖊️ Note Editor 

A minimalist markdown canvas designed for clarity. Features include:
- **Real-time markdown preview** and a quick formatting toolbar.
- **Multi-modal sidebar**: View attached images, sketches, or screenshots alongside your text notes.
- **Custom Templates**: Use structured forms (Socratic questioning, Cornell notes, etc.) to guide your reflections.
- **Export** to PDF, Markdown, or plain‑text.

---

## 🕸 Relatrix — 3D Knowledge Discovery

<br/>

<p align="center">
  <img src="screenshots/infographics/relatrix_showcase.png" height="500" />
  <img src="screenshots/relatrix_demo.gif" height="500" />
</p>

<br/>

Relatrix is a live, physics‑simulated 3D graph that renders the connective tissue of your knowledge base. Every node is a resource or topic; every edge is a knowledge link you created. Filter by entry types and explore the relationships between your ideas. Pinch to zoom, drag to rotate, and adjust the physics engine (repulsion, spring tension) in real time.

---

## 🏗 Architecture

Relatrix is built for long‑term stability and privacy. The codebase uses **Kotlin Multiplatform** so that the core business logic is shared across Android and (upcoming) iOS.

```
Relatrix/
├── shared/                     # Platform‑agnostic core
│   ├── domain/                 # Models: Topic, Resource, Opinion, Annotation, Bookmark, Template
│   ├── data/                   # SQLDelight repositories, encrypted with SQLCipher
│   └── business/               # SearchEngine, AutoLinkService, ExportService
│
└── androidApp/                 # Native Android client
    ├── ui/                     # Jetpack Compose screens & components
    ├── viewmodel/              # MVVM architecture using shared ViewModels
    ├── di/                     # Koin dependency injection modules
    └── workers/                # WorkManager background tasks (Cleanup, etc.)
```

### Tech Stack

| Layer | Technology |
|:---|:---|
| Language | Kotlin Multiplatform |
| Android UI | Jetpack Compose |
| iOS UI | SwiftUI *(in development)* |
| Database | SQLDelight + SQLCipher (encrypted) |
| Concurrency | Kotlin Coroutines & Flow |
| Dependency Injection | Koin |
| Background Work | WorkManager |
| PDF Rendering | Custom paged renderer via PDFBox-Android |

---

## 🚀 Getting Started

### Prerequisites

- **Android Studio** Ladybug or later
- **JDK 17+**
- Android SDK (API 26+)

### Installation

```bash
# 1. Clone the repo
git clone https://github.com/saad-ibra/relatrix.git
cd relatrix

# 2. Open in Android Studio
# File → Open → select the cloned folder

# 3. Sync Gradle & run
# Select the 'androidApp' configuration and press Run
```

> The database is encrypted with SQLCipher and fully offline — no backend, no account, no tracking.

---

## 📋 Core Concepts: Entry Types

Relatrix is built around diverse ways of capturing thoughts.

| Entry Type | Description |
|:---|:---|
| **Opinion** | A timestamped personal reflection with a confidence score. |
| **Annotation** | A highlight or inline note anchored to a specific text passage. |
| **Visual Note (Vision)** | A visual capture (photo, sketch, screenshot) with an attached caption. |
| **Bookmark** | A milestone in your reading journey, often containing a summary. |
| **Dictionary Entry** | A definition or lookup for a specific phrase, synced across the app. |
| **Template** | Structured reflections using guided forms (e.g., Summary, Key Takeaway). |
| **Knowledge Link** | A `[[wiki-link]]` that creates a bidirectional edge between any of the above. |

---

## 🗺 Roadmap

- [x] Multi‑modal resource entry with confidence scoring
- [x] Unified Timeline (Opinions + Annotations + Bookmarks + Visuals)
- [x] Relatrix 3D knowledge graph with physics simulation
- [x] SQLCipher‑encrypted offline database
- [x] Full‑text offline search across resources and entries
- [x] PDF reader with text selection and annotation
- [x] Dark mode
- [ ] iOS app (SwiftUI) — *in development*
- [ ] iCloud / local sync between devices
- [ ] AI‑assisted reflection prompts *(planned)*
- [ ] Web clipper browser extension *(planned)*

---

<div align="center">

Built by a sleep-deprived human who read way too many PDFs just to make sure the progress bar felt right

*For researchers, deep readers, students, and anyone who actually wants to understand what they consume.*

</div>
