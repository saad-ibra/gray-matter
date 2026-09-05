# Changelog

## [v2.0] - Relatrix 2.0 Major Update
### Added
- **Tag Management Hub**: A dedicated interface to manage, explore, and filter your resources across all topics using custom tags.
- **Tag Bulk Actions & Undo**: Long-press in the Tag Management screen to multi-select entries for bulk removal or deletion, complete with an Undo snackbar.
- **Export to PDF (Tags)**: Generate beautifully formatted PDF reports for all entries associated with a specific tag.
- **Enhanced Color Customization**: The Topic color picker has been completely overhauled with 36 suggested colors, an interactive HSV color wheel, brightness slider, HEX code input, and a recently used colors history (up to 18 colors).
- **Unified Connections Dropdown**: A streamlined UI for quickly attaching both knowledge links and custom tags to your Timeline entries.
- **Profile Support Actions**: Added new quick-access buttons in the Profile section for "Get in Touch" and "Report Issue / Request Feature".

### Changed
- **Blazing Fast Search (FTS5)**: Entirely rebuilt the local search engine using SQLite FTS5 virtual tables and triggers, making full-text search near-instantaneous.
- **Multi-Module Architecture**: Refactored the monolithic codebase into a modern feature-based multi-module architecture (Core DesignSystem, Feature Tags) for significantly faster build times.
- **Battery & CPU Optimization**: Migrated the entire UI to `collectAsStateWithLifecycle`, automatically pausing database watchers when the app is in the background.
- **Data Safety Guarantees**: Configured robust SQLDelight schema migrations (`.sqm`) with baseline verification to ensure flawless database upgrades (e.g., from v1.8 to v2.0).
- **Timeline UI**: Adaptive styling for tags and knowledge links directly on resource timelines, seamlessly adjusting between Light and Dark modes.
- **3D Graph Interface**: Restored the classic filter icon for the console toggle and explicitly set the bottom container icon to an 'X' to clarify dismiss functionality.
- **Improved Intent Handling**: Restored and bulletproofed the Android open-with intent chooser for robust cross-app file handling.
