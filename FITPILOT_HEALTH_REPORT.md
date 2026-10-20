# FitPilot Beta Codebase Health & Quality Audit

## Overview
This document outlines the findings from the comprehensive architectural and health check review of the FitPilot Beta Android codebase. The audit focused on Clean Architecture, Jetpack Compose performance, Room database usage, and general Kotlin code quality.

## High Severity Issues

### 1. Clean Architecture Violations
- **Issue:** Domain logic is leaking into ViewModels, and ViewModels are bypassing the repository layer to access the database directly using `WorkoutDao`.
- **Locations:**
  - `WorkoutDetailViewModel` directly depends on `WorkoutDao` and uses `dao.getWorkoutDetail(sessionId)`.
  - `DashboardViewModel` directly depends on `WorkoutDao` and uses `workoutDao.getAllTemplates()`.
  - `ActiveWorkoutViewModel` (and likely others) injects `WorkoutDao` instead of using a UseCase or Repository.
- **Recommendation:** Implement a robust Repository layer (e.g., `WorkoutRepository`) that abstracts Room DAOs. ViewModels should only interact with UseCases or Repositories, not DAOs directly.

### 2. Missing Indices on Foreign Keys
- **Issue:** Several Room entities have foreign keys that are not properly indexed, which could lead to full table scans and severe performance degradation on updates/deletions as the database grows.
- **Locations:**
  - `SetAdviceEntity` has a foreign key to `setLogId` but lacks an index for it in the `@Entity` annotation indices list.
  - `AdaptiveCardioRecommendationEntity` has a foreign key to `workoutSessionId` but lacks an index for it.
  - `CardioSessionEntity` lacks foreign keys and indices despite referencing `workoutSessionId`.
- **Recommendation:** Ensure every column used in a `ForeignKey` constraint or frequently queried in a `WHERE` clause is explicitly defined in the `indices = [Index(...)]` block of the `@Entity` annotation.

## Medium Severity Issues

### 1. Jetpack Compose Performance Risks
- **Issue:** Incorrect usage of state collection in Compose. Using `collectAsState()` without lifecycle awareness can cause the UI to continue collecting flows even when the app is in the background, wasting resources.
- **Locations:**
  - `DashboardScreen.kt`: Uses `viewModel.uiState.collectAsState()`
  - `ActiveWorkoutScreen.kt`: Uses `viewModel.workoutSession.collectAsState()` and `viewModel.restTimerState.collectAsState()`
  - `WorkoutDetailScreen.kt`: Uses `viewModel.detail.collectAsState()`
- **Recommendation:** Replace `collectAsState()` with `collectAsStateWithLifecycle()` from `androidx.lifecycle.compose` to ensure flows are safely collected.
- **Issue:** Missing `key` parameter in `LazyColumn` items, which can cause excessive recompositions when the list changes.
- **Locations:** `DashboardScreen` and `ActiveWorkoutScreen` `items` blocks.
- **Recommendation:** Always provide a stable `key` to `items()` (e.g., `key = { it.id }`).

### 2. Room Multi-table Queries (N+1 Problem & Complex Queries)
- **Issue:** Heavy `@Transaction` queries fetching deep relations might block execution or perform poorly if not paginated or scoped correctly.
- **Locations:** `getWorkoutHistoryList` and `getWorkoutDetail` in `WorkoutDao.kt` use very complex nested queries and subqueries instead of leveraging standard Room relations effectively.
- **Recommendation:** Ensure these queries are executed off the main thread. Consider simplifying subqueries using proper `@Relation` tags in DTOs.

## Low Severity Issues

### 1. Code Quality & Formatting
- **Issue:** Unused imports and somewhat messy dependency injection.
- **Issue:** Some ViewModels inject `SavedStateHandle` but don't handle state restoration fully.
- **Issue:** `MojibakeMigrationUtils.kt` is a clever workaround but introduces hardcoded repairs on legacy data. While functional, it should ideally be handled via a proper Room Migration rather than a manual UseCase/Utility call.
- **Recommendation:** Run a code formatting pass (e.g., `ktlint` or `detekt`) to clean up imports and structure. Consider moving mojibake repair directly into a Room `Migration` class.

### 2. Hardcoded Strings
- **Issue:** Several UI elements, especially in UseCases (like `ExportSessionToJsonUseCase`) and ViewModels, contain hardcoded strings instead of utilizing string resources.
- **Recommendation:** Extract all user-facing strings to `res/values/strings.xml`.
