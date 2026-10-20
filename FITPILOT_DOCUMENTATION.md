# FitPilot Beta - Master Project Documentation

This document serves as the absolute source of truth for the current architecture, features, and design system of the FitPilot Beta project.

## 1. Project Overview & Tech Stack

**FitPilot Beta** is a highly capable, offline-first Android application geared towards smart coaching, fitness tracking, and nutrition planning. Its offline-first philosophy ensures rapid, resilient operation without strict internet dependency for daily logging, while seamlessly integrating optional AI recommendations and JSON-based program syncing.

### Core Tech Stack
- **Language**: Kotlin 2.0.20 (with JDK 17 target compatibility).
- **UI Framework**: Jetpack Compose (Material 3).
- **Architecture**: Clean Architecture / MVVM.
- **Local Persistence**:
  * Room Database (v2.6.1) with rigorous relational mapping (`GymFlowDatabase`).
  * DataStore Preferences (v1.1.1) for settings and global targets (`ProfileDataStore`, `WorkoutSettingsDataStore`, `AiSettingsDataStore`).
- **Dependency Injection**: Hilt (v2.51.1).
- **Serialization**: Kotlinx.Serialization for high-performance JSON parsing.
- **Asynchronous Execution**: Kotlin Coroutines & Flow.

---

## 2. Architecture (Clean Architecture & MVVM)

The project heavily adheres to the Clean Architecture pattern, dividing responsibilities strictly into three layers with a strict unidirectional data flow.

### Data Flow Diagram

```mermaid
flowchart TD
    UI[Jetpack Compose UI] -->|User Intent| VM[ViewModel]
    VM -->|State/Flow Collection| UI
    VM -->|Domain Logic| UC[Use Cases]
    VM -.->|Direct Data Request| REP[WorkoutRepository]
    UC -->|Data Request| REP
    REP -->|Query/Mutation| DAO[Room DAOs]
    REP -->|Preferences| DS[DataStores]
    DAO -->|SQLite Operations| DB[(Room Database)]