# ADR-0002: Upgrade to Gradle 8.10.2 and AGP 8.8.0

**Date**: 2026-02-08
**Status**: Accepted

## Context
Android Studio requires Gradle 8.5+ for JDK 21 compatibility. The project was on Gradle 7.6.4 + AGP 7.4.2, which maxes out at JDK 19. This broke Android Studio sync for developers using JDK 21.

## Decision
Upgrade to Gradle 8.10.2 and AGP 8.8.0. Set JDK to 17 (highest version installed via jenv). Add compatibility flags for AGP 8.x behavioral changes.

## Alternatives Considered
- **Gradle 8.5 + AGP 8.3**: Minimum viable versions, but less battle-tested and shorter support window.
- **Gradle 8.12 + AGP 8.9**: Bleeding edge, higher risk of undocumented issues.
- **Convert switch(R.id) to if-else**: Would fix the non-final R field issue properly but requires touching 7 files with 17 switch cases — deferred to a separate effort.

## Rationale
Gradle 8.10.2 + AGP 8.8.0 are stable releases (Jan 2025) with wide adoption. This combination supports JDK 17-21 and compileSdk up to 35.

Key AGP 8.x behavioral changes required compatibility flags:
- `android.nonFinalResIds=false` — AGP 8 makes R fields non-final by default, breaking `switch(R.id.xxx)` statements. Set to false to preserve final R fields.
- `android.nonTransitiveRClass=false` — AGP 8 defaults to non-transitive R classes, which also makes fields non-constant. Set to false for compatibility.
- `buildFeatures { buildConfig = true }` — AGP 8 disables BuildConfig generation by default; this project uses `buildConfigField`.
- `packagingOptions` renamed to `packaging` with new DSL syntax.
- `compileSdkVersion`/`minSdkVersion`/`targetSdkVersion` renamed to `compileSdk`/`minSdk`/`targetSdk`.
- Removed `jcenter()` repository (already dead, all deps available on mavenCentral/google).
- Removed `android.enableJetifier=true` (all deps are AndroidX-native).

## Consequences
**Benefits**:
- Android Studio sync works with JDK 17+
- Access to newer AGP features and security fixes
- Can incrementally upgrade compileSdk/targetSdk to 34/35

**Trade-offs**:
- `android.nonFinalResIds=false` and `android.nonTransitiveRClass=false` are compatibility flags that should eventually be removed by converting switch statements to if-else
- JDK set to 17 (not 21) due to jenv availability; works fine since Gradle 8.10.2 supports JDK 17-23

**Risks**:
- `nonFinalResIds` flag may be removed in a future AGP version, forcing the switch-to-if-else migration
