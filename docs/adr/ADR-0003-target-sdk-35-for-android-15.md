# ADR-0003: Target SDK 35 for Android 15

**Date**: 2026-02-09
**Status**: Accepted

## Context

Google Play requires targetSdk 34+ for app updates by Q3 2025, and targetSdk 35 by August 2025. The STHLM Traveling app is currently on targetSdk 33 (Android 13), which will soon block new releases.

The main question was whether to upgrade to SDK 34 (Android 14) or jump directly to SDK 35 (Android 15).

**Critical blocker identified:** The app's deviation notification feature (currently disabled) sends notifications without the POST_NOTIFICATIONS runtime permission, which has been required since Android 13 (targetSdk 33). This needs to be fixed as part of the upgrade.

**Key constraints:**
- Must maintain backward compatibility with minSdk 23 (Android 6.0)
- Must implement POST_NOTIFICATIONS permission for notification feature
- Must handle Android 15's forced edge-to-edge display mode
- Limited resources for UI testing and fixes

## Decision

**Upgrade directly to compileSdk/targetSdk 35 (Android 15)** and implement POST_NOTIFICATIONS permission support.

## Alternatives Considered

### Option 1: Upgrade to SDK 34 only
**Pros:**
- One version increment, potentially less complexity
- Android 14 is more widely deployed
- Fewer breaking changes to handle

**Cons:**
- Would require another upgrade to SDK 35 within 6 months
- Double migration effort (SDK 33→34→35)
- POST_NOTIFICATIONS still required (introduced in SDK 33)
- No significant technical benefit over SDK 35

### Option 2: Delay upgrade
**Pros:**
- More time to prepare and test
- Wait for Android 15 adoption to increase

**Cons:**
- Blocks app releases to Google Play after Q3 2025
- Technical debt accumulates
- Doesn't address notification permission blocker
- Not a viable long-term option

## Rationale

**Why SDK 35 instead of SDK 34:**
1. **Avoid double migration:** Going 33→35 in one step is less total work than 33→34→35
2. **Longer runway:** SDK 35 is current as of 2026, giving us through 2026+ before the next forced upgrade
3. **Minimal incremental risk:** Breaking changes between 34→35 are small compared to 33→34
4. **Already handling POST_NOTIFICATIONS:** Since we must implement runtime notification permissions anyway (required for SDK 33+), we're already addressing the major behavioral change

**Notification permission strategy:**
- Implement NotificationHelper utility for centralized permission checks
- Request permission when user enables deviation notifications in settings
- Create notification channels (required for Android 8+)
- Re-enable the previously disabled notification preferences

**Edge-to-edge display strategy:**
- Android 15 forces edge-to-edge mode (transparent system bars)
- Enable predictive back gesture with `android:enableOnBackInvokedCallback="true"`
- Defer comprehensive UI fixes to follow-up work
- Accept potential minor visual issues initially (conservative approach for legacy app with many screens)

## Consequences

### Benefits
- **Compliance:** Meets Google Play requirements through 2026+
- **Feature unlock:** Deviation notification feature becomes functional
- **Modern API access:** Access to Android 14/15 APIs and improvements
- **Single migration:** Avoid a second upgrade cycle in 6 months

### Trade-offs
- **Testing burden:** Must test on Android 14 and 15
- **Edge-to-edge complexity:** Potential UI issues that may require screen-by-screen fixes
- **Notification UX change:** Users must explicitly grant notification permission (industry standard since Android 13)

### Risks
- **Visual regressions:** Edge-to-edge may cause system bar overlap on some screens
  - *Mitigation:* Extensive manual testing; conservative initial approach; fix incrementally
- **Permission denial:** Users may deny notification permission
  - *Mitigation:* Clear explanation; toast message on denial; preserve user choice
- **Backward compatibility:** Changes may affect older Android versions
  - *Mitigation:* API level checks (Build.VERSION.SDK_INT); test on minSdk 23

### Technical Debt Addressed
- Unblocks modernization roadmap Phase 1
- Removes disabled/broken notification feature
- Brings app into compliance with modern Android permission model

### Follow-up Work Required
- Comprehensive visual testing on Android 15 devices
- Screen-by-screen edge-to-edge fixes if issues found
- Consider implementing proper window insets handling in base activities
