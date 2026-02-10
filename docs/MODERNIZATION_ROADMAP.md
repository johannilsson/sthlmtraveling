# STHLM Traveling Modernization Roadmap

## Overview

This document tracks the multi-phase modernization of the STHLM Traveling Android app from a legacy Java codebase to a modern Kotlin + Jetpack architecture.

**Strategy:** Each phase builds on the previous one, moving from low-risk mechanical changes to higher-level architectural improvements.

## Progress Overview

- [x] **Phase 1:** Upgrade compileSdk/targetSdk to 34/35 (Completed 2026-02-09)
- [ ] **Phase 2:** Convert Java codebase to Kotlin
- [ ] **Phase 3:** Modernize networking layer (Retrofit 2.x + OkHttp 4.x)
- [ ] **Phase 4:** Introduce modern architecture (Hilt, ViewModel, coroutines)
- [ ] **Phase 5:** Replace API backend with new API

---

## Phase 1: Upgrade compileSdk/targetSdk to 34/35

**Status:** ✅ Completed (2026-02-09)

**Rationale:** Already on the future work list and is a prerequisite for Play Store updates. Small, mechanical, low-risk change that must be completed before other modernization work.

### Scope
- Update `compileSdk` and `targetSdk` from 33 to 34/35 in build.gradle
- Address Android 14/15 behavior changes and new requirements
- Handle new permission models (e.g., notification permissions, foreground service types)
- Update deprecated APIs if any
- Test on Android 14/15 devices/emulators to ensure no regressions

### Completion Summary

**Upgraded to SDK 35 (Android 15)** on 2026-02-09

**Changes implemented:**
- Updated compileSdk and targetSdk from 33 to 35
- Implemented POST_NOTIFICATIONS runtime permission (required for Android 13+)
- Created NotificationHelper utility for channel management and permission checks
- Re-enabled deviation notification feature with proper permission flow
- Added notification channels for Android 8+ compatibility
- Enabled predictive back gesture for Android 15 (`android:enableOnBackInvokedCallback`)

**Key decisions:**
- Jumped directly to SDK 35 (skipping 34) to avoid double migration
- Used conservative approach for edge-to-edge display (defer comprehensive UI fixes)
- Maintained backward compatibility with minSdk 23

**See:** [ADR-0003](/docs/adr/ADR-0003-target-sdk-35-for-android-15.md) for detailed rationale

### Acceptance Criteria
- [x] App builds successfully with new SDK versions
- [x] All existing functionality works on Android 14/15
- [x] No new lint errors or deprecation warnings (AsyncTask warnings deferred to Phase 4)
- [x] App runs without crashes on target SDK versions

---

## Phase 2: Convert Java Codebase to Kotlin

**Status:** 🔄 In Progress

**Rationale:** Kotlin conversion is mostly mechanical and easier to do before the architecture rewrite. It's better to write new architecture code in Kotlin from the start than to write it in Java and convert later.

### Strategy
- Start with leaf files (utils, models, adapters) and work inward toward Activities
- Use Android Studio's Java-to-Kotlin converter followed by manual cleanup
- Each conversion should maintain existing behavior (no logic changes)
- Convert one logical group at a time

### Conversion Order
1. [x] `data/models/` - DTOs and data classes ✅ **Phase 2.1 - Completed 2026-02-09**
2. [x] `utils/` - Helper classes ✅ **Phase 2.2 - Completed 2026-02-09**
3. [x] `ui/adapter/` - RecyclerView adapters ✅ **Phase 2.3 - Completed 2026-02-09**
4. [x] `ui/view/` - Custom views ✅ **Phase 2.4 - Completed 2026-02-10**
5. [x] `provider/` - Data access layer ✅ **Phase 2.5 - Completed 2026-02-10**
6. [x] `service/` - Background services & receivers ✅ **Phase 2.6 - Completed 2026-02-10**
7. [ ] Activities, Fragments, Application, and remaining files (42 Java files remain)

### Remaining Work - Phase 2.7

**42 Java files remaining** (as of 2026-02-10):

**Core Application:**
- `MyApplication.java` (1 file)

**Base Classes:**
- `BaseActivity.java`, `BaseFragmentActivity.java`, `BaseListActivity.java`, `BaseListFragmentActivity.java` (4 files)
- `BaseFragment.java`, `BaseListFragment.java` (2 files)
- `AppCompatPreferenceActivity.java` (1 file)

**Activities** (17 files):
- Main: `StartActivity`, `DeparturesActivity`, `RoutesActivity`, `RouteDetailActivity`
- Map: `PointOnMapActivity`, `ViewOnMapActivity`, `NearbyActivity`
- Settings: `SettingsActivity`, `AboutActivity`
- Deviations: `DeviationsActivity`, `DeviationDetailActivity`
- Planner: `PlannerFragmentActivity`, `SearchDeparturesFragmentActivity`, `ChangeRouteTimeActivity`, `PlaceSearchActivity`
- Proxy: `UriLauncherActivity`, `DeparturesShortcutProxyActivity`

**Fragments** (4 files):
- `PlannerFragment`, `SearchDeparturesFragment`, `TrafficStatusFragment`, `FavoritesFragment`

**Adapters & UI Models** (4 files):
- `SectionedAdapter`, `DepartureAdapter`, `AutoCompleteStopAdapter`
- `ui/models/LegViewModel`

**Data/API Layer** (7 files):
- `data/api/ApiService`, `data/api/PlaceQuery`, `data/api/TravelModeQuery`
- `data/misc/ApiServiceProvider`, `data/misc/HttpHelper`, `data/misc/GsonProvider`, `data/misc/MockInterceptor`

**Utilities** (2 files):
- `DialogHelper`, `AppConfig`

**Strategy for Phase 2.7:**
1. Start with utilities and helpers (`DialogHelper`, `AppConfig`)
2. Convert adapters and UI models
3. Convert data/API layer (should be straightforward)
4. Convert base classes (needed by Activities/Fragments)
5. Convert simpler Activities (About, Proxy, etc.)
6. Convert Fragments
7. Finally convert main Activities and `MyApplication`

### Acceptance Criteria
- [ ] No Java files remain in `com.markupartist.sthlmtraveling` package
- [ ] All existing functionality preserved (no behavior changes)
- [ ] Code compiles and runs without errors
- [ ] Idiomatic Kotlin patterns applied (nullable types, data classes, extension functions, etc.)

---

## Phase 3: Modernize Networking Layer

**Status:** 🔲 Not Started (blocked by Phase 2)

**Rationale:** Retrofit 1.9 is ancient and blocks further modernization. Modern HTTP tooling is a prerequisite for the new API and enables coroutines-based async patterns.

### Scope
- Upgrade Retrofit 1.9 → Retrofit 2.x (latest stable)
- Upgrade OkHttp 2.7.5 → OkHttp 4.x
- Add Retrofit coroutines adapter
- Add Gson converter for Retrofit 2
- Update `ApiService` interface to use suspend functions
- Replace Retrofit 1 callbacks with coroutines/suspend functions
- Initially maintain same API contracts to minimize behavior changes
- Update `ApiServiceProvider` and `HttpHelper` for new APIs

### Migration Approach
- Can run both Retrofit versions side-by-side during migration if needed
- Migrate endpoints one by one to verify behavior
- Replace AsyncTask patterns with coroutines where API calls are made

### Acceptance Criteria
- [ ] All API calls use Retrofit 2.x + OkHttp 4.x
- [ ] No Retrofit 1.9 or OkHttp 2.x dependencies remain
- [ ] API calls use suspend functions + coroutines (no callbacks)
- [ ] Existing functionality preserved
- [ ] No regressions in network error handling

---

## Phase 4: Introduce Modern Architecture

**Status:** 🔲 Not Started (blocked by Phase 3)

**Rationale:** By this point, we have Kotlin everywhere and modern networking. We can now introduce modern architecture patterns incrementally, one screen at a time.

### Scope
- Add Hilt for dependency injection
- Introduce AndroidX ViewModel pattern
- Replace AsyncTask with coroutines + ViewModelScope
- Extract business logic from Activities/Fragments into ViewModels
- Establish clean separation between UI and data layers
- Apply modern architecture components (LiveData/StateFlow for UI state)

### Migration Strategy
1. Set up Hilt dependency injection framework
2. Start with simplest screen as proof of concept (e.g., `DeparturesActivity`)
3. For each screen:
   - Create ViewModel
   - Move data fetching logic to ViewModel
   - Replace AsyncTask with coroutines
   - Inject dependencies via Hilt
   - Use StateFlow/LiveData for UI state
4. Apply pattern to remaining screens systematically

### Target Screens (in order)
- [ ] DeparturesActivity (proof of concept)
- [ ] RoutesActivity
- [ ] PlannerFragment
- [ ] StartActivity
- [ ] Other Activities/Fragments

### Acceptance Criteria
- [ ] Hilt set up and providing DI throughout app
- [ ] All screens use ViewModel pattern
- [ ] No AsyncTask usage remains
- [ ] All background work uses coroutines
- [ ] UI properly observes state from ViewModels
- [ ] Existing functionality preserved

---

## Phase 5: Replace API Backend

**Status:** 🔲 Not Started (blocked by Phase 4)

**Rationale:** Do this last. By this point we'll have Kotlin everywhere, modern networking stack, and clean separation between UI and data layers. The API swap becomes primarily a data layer change.

### Context
By this phase, the app will have:
- Modern SDK targets (34/35)
- Kotlin codebase
- Retrofit 2.x + OkHttp 4.x networking stack
- Clean architecture with ViewModels and DI

### Scope
- Define new API contract and endpoints (details TBD in follow-up)
- Update `ApiService` interface for new API
- Update data models to match new API responses
- Update API base URL and authentication if needed
- Migrate all endpoints to new API
- Update error handling for new API error formats
- Test all functionality with new API

**Note:** The details of the new API (endpoints, authentication, data formats) will be specified in a follow-up task. This phase is a placeholder to maintain the overall modernization roadmap.

### Acceptance Criteria
- [ ] All API calls use new backend
- [ ] No calls to old STHLM Traveling API remain
- [ ] All existing functionality works with new API
- [ ] Error handling properly adapted to new API
- [ ] Performance is acceptable

---

## Notes

- Each phase should be completed and tested before moving to the next
- Update the checkboxes in this document as work progresses
- If any phase reveals unexpected complexity, break it into sub-tasks
- Document key decisions in ADRs as needed (see `/docs/adr/`)

## Last Updated

2026-02-10
