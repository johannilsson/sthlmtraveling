# ADR-0001: Modernize Build Chain for JDK 19

**Date**: 2026-02-08
**Status**: Accepted

## Context
The project used AGP 3.6.4 + Gradle 7.2, which is incompatible with JDK 19 (the JDK installed on the development machine). AGP 3.6.4 doesn't support Gradle 7.6+, which is the minimum Gradle version that supports JDK 19. The build failed with a Java class version error, making development impossible without downgrading the JDK.

## Decision
Upgrade the build toolchain to Gradle 7.6.4 + AGP 7.4.2, compileSdk/targetSdk 33, and bump library dependencies to compatible versions. Vendor the `recyclerview-multiple-viewtypes-adapter` library (3 classes) since JCenter is offline.

## Alternatives Considered
- **Downgrade JDK to 11**: Would work but locks the project to an increasingly outdated JDK and creates friction for developers with newer JDK installations.
- **AGP 8.x**: Would require migrating to Gradle 8.x and the new `plugins {}` DSL. More disruption for limited benefit given the project's current state.
- **AGP 7.0 (minimum for Gradle 7.6)**: Would work but 7.4.2 is the latest 7.x and receives more bugfixes.

## Rationale
AGP 7.4.2 is the latest in the 7.x line, supports JDK 19 via Gradle 7.6.4, and requires minimal migration from AGP 3.6.4 (mostly `namespace` in build.gradle, `android:exported` in manifest). It avoids the larger migration to AGP 8.x/Gradle 8.x while fully unblocking modern JDK usage.

## Consequences
**Benefits**:
- Build works with JDK 11–19
- Access to newer AndroidX/Material/Play Services libraries
- targetSdk 33 meets Google Play requirements

**Trade-offs**:
- `recyclerview-multiple-viewtypes-adapter` is now vendored source (3 files) instead of a Maven dependency, since JCenter is permanently offline
- Retrofit 1.9 / OkHttp 2.x remain unchanged — upgrading these is a separate, larger effort

**Risks**:
- Material 1.2.0-alpha to 1.9.0 is a large jump; visual regressions in themed components are possible
- `jcenter()` is still in repositories for any transitive resolution; it's read-only and may go fully offline eventually
