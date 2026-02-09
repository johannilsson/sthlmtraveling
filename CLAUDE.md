# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

STHLM Traveling is an Android app for Stockholm public transport journey planning. It is a Java-based legacy Android project (pre-Kotlin, pre-Jetpack) licensed under Apache 2.0.

## Build Commands

Use the `build-check` skill to run Gradle tasks:

- **Build debug APK**: `/build-check assembleDebug`
- **Build release APK**: `/build-check assembleRelease` (requires STORE_FILE, STORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD env vars)
- **Run instrumented tests**: `/build-check connectedAndroidTest` (requires emulator or device)
- **Run unit tests**: `/build-check testDebugUnitTest`
- **Clean build**: `/build-check clean`

Alternatively, run Gradle directly: `./gradlew <task>`

**Build prerequisites:** Gradle 8.10.2 with AGP 8.8.0. Compatible with JDK 17+. The `.java-version` file says 17.0.

**Build configuration:** Uses `build_prod.properties` if available, otherwise falls back to `build_default.properties`. Keys configured there include API keys, analytics, and feature flags.

## Project Structure

Single-module project. The app module is at `sthlmtraveling/` (not `app/`).

- **Package root:** `com.markupartist.sthlmtraveling`
- **Application ID:** `com.markupartist.sthlmtraveling` (debug suffix: `.debug`)

### Source Layout (`sthlmtraveling/src/main/java/com/markupartist/sthlmtraveling/`)

| Package | Purpose |
|---------|---------|
| Root | Activities and Fragments (UI layer) — `StartActivity`, `RoutesActivity`, `DeparturesActivity`, `PlannerFragment`, etc. |
| `data/api/` | Retrofit 1.9 REST API interface (`ApiService`) |
| `data/models/` | API data transfer objects (`Plan`, `Leg`, `Route`, `Place`, `Fare`, etc.) |
| `data/misc/` | `ApiServiceProvider`, `HttpHelper` |
| `provider/` | Data access — SQLite adapters (`FavoritesDbAdapter`, `HistoryDbAdapter`), Content Providers (`JourneysProvider`, `PlacesProvider`), Stores (`SitesStore`, `DeparturesStore`), and the `Planner`/`Router` classes |
| `service/` | Background services (`DeviationService`, `DataMigrationService`) |
| `ui/adapter/` | List/RecyclerView adapters |
| `ui/view/` | Custom views and UI components |
| `utils/` | Helpers for analytics, dates, location, themes, views |

## Architecture

**Pattern:** Traditional Activity-centric MVC. Activities and Fragments contain business logic directly. No ViewModel, LiveData, or modern architecture components.

**Key patterns:**
- **AsyncTask** for background work (e.g., `GetDeparturesTask` in `DeparturesActivity`)
- **Retrofit 1.9** with async callbacks for API calls
- **Content Providers** (`JourneysProvider`, `PlacesProvider`) for data encapsulation
- **Manual SQLite** via adapter classes (`FavoritesDbAdapter`, `HistoryDbAdapter`)
- **No dependency injection** — services accessed via `MyApplication` singleton or direct instantiation
- **Base classes:** `BaseActivity`, `BaseFragmentActivity`, `BaseFragment` provide shared functionality

**API layer:** REST calls go through `ApiService` (Retrofit interface) → STHLM Traveling backend API at `/v1/planner/`, `/v1/semistatic/site/near/`, etc.

## Key Dependencies

- Retrofit 1.9.0 + OkHttp 2.7.5 (legacy versions)
- Gson 2.8.9
- Google Play Services (Maps 18.1.0, Location 21.0.1, Base 18.2.0)
- AndroidX AppCompat 1.6.1, Material 1.9.0
- compileSdk/targetSdk 33, minSdk 23

## Tests

Test coverage is minimal. Instrumented tests are at `sthlmtraveling/src/instrumentTest/`. The existing `RouteParserTest` is a stub.

## Automated ADR Creation

### When to Auto-Create ADRs
Automatically create and commit an ADR when:
- Choosing between multiple technical approaches or tools
- Making architectural decisions (data flow, service boundaries, etc.)
- Establishing new coding patterns or conventions
- Adding significant dependencies or third-party services
- Making trade-offs that have long-term implications
- Rejecting a seemingly-obvious approach for non-obvious reasons

### ADR Format & Location
Create ADRs in `/docs/adr/ADR-{NNNN}-{short-kebab-title}.md`

**Template**:
```
# ADR-{NNNN}: {Title}

**Date**: {YYYY-MM-DD}
**Status**: Accepted

## Context
{What problem or question prompted this decision? What were the constraints, requirements, or goals?}

## Decision
{What we decided to do, clearly and concisely}

## Alternatives Considered
{List other options discussed with key pros/cons for each}

## Rationale
{Why this option was chosen over the alternatives - capture the reasoning process}

## Consequences
**Benefits**:
- {positive outcomes}

**Trade-offs**:
- {what we're giving up or accepting}

**Risks**:
- {potential issues to monitor}
```

### Process
1. **Recognize** decision points during our work
2. **Create** the ADR file with sequential numbering
3. **Capture** the full context and reasoning from our discussion
4. **Commit** immediately with message: `docs: add ADR-{NNNN} {title}`
5. **Inform** me briefly that the ADR was created (just mention the number and title)

### What to Capture
Focus on preserving:
- The **why** behind decisions, not just the what
- Alternatives we discussed and why they were rejected
- Assumptions we made at the time
- Context that might not be obvious later

Don't create ADRs for:
- Trivial implementation details
- Reversible choices with no long-term impact
- Bug fixes (unless they reveal a systemic issue)

## Development Workflow

### Branching Strategy

**CRITICAL: Never commit directly to the main branch.**

When starting any new feature, bug fix, or change:

1. **Ensure you're on main branch**: `git checkout master`
2. **Pull latest changes**: `git pull origin master`
3. **Create feature branch**: `git checkout -b <branch-name>`
   - Use descriptive names: `feature/offline-mode`, `fix/crash-on-search`, `refactor/api-client`
4. **Make changes and commit** on the feature branch
5. **Create PR** when ready (see below)

**Branch naming conventions:**
- `feature/*` — New features or enhancements
- `fix/*` — Bug fixes
- `refactor/*` — Code improvements without behavior changes
- `docs/*` — Documentation updates
- `chore/*` — Build, tooling, or maintenance tasks

**Important:**
- Always branch from `master`, never from feature branches
- This keeps the history clean and avoids cascading dependencies
- See ADR-0004 for rationale

### Creating Pull Requests

Use the `create-pr` skill for an end-to-end PR workflow:

```
/create-pr [branch-name] [base-branch]
```

This skill will:
1. Check current git state and handle uncommitted changes
2. Create a new branch (or use existing)
3. Stage and commit changes with proper commit messages
4. Analyze changes and generate intelligent PR description
5. Push to remote
6. Create PR via `gh pr create`

**Examples:**
- `/create-pr` — Interactive mode, prompts for branch name
- `/create-pr feature/offline-mode` — Create PR on specified branch
- `/create-pr fix/crash-123 develop` — Target a different base branch

The skill follows all repository conventions automatically (no AI attribution, proper commit style, etc.).

## Git & PR Conventions

- Do NOT add `Co-Authored-By` trailers to commits
- Do NOT add "Generated with Claude Code" or similar AI attribution to PR descriptions
- Commit messages should be concise and focus on the "why"