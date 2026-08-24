# AGENTS.md — Sonara

Android app project. Read this before changing anything; per-project rules here
override the global ones in `~/AGENTS.md` where they conflict.

## What this is

**Sonara** is a premium ambient music-player app for Android: an *always-dark*,
immersive environment whose background will react to album art (Palette-based
color extraction), built on Jetpack Compose with Media3/ExoPlayer playback.

Current state: **Stage 2 complete — playback core.** The app has the Stage 1
foundation (splash, shell with Home/Search/Library/Flow placeholder screens,
design-token system) plus a working playback engine: `PlaybackService`
(Media3 `MediaSessionService` + ExoPlayer with audio focus / noisy-pair
handling), a `PlayerConnection` exposing session state as a Compose-observable
`StateFlow`, a mini-player bar, and five bundled demo tracks (`res/raw/*.wav`)
that back the UI until the catalog stage. Not yet implemented (later stages):
catalog providers, networking, persistence, artwork color extraction, and the
reactive visual engine; the full now-playing screen and notification/media
button polish beyond Media3 defaults also remain open. Do not assume any of
that exists.

## Tech stack (fixed — do not change without strong reason)

| Concern      | Choice                                              |
|--------------|-----------------------------------------------------|
| Language     | Kotlin 2.2.10                                       |
| UI           | Jetpack Compose 1.11.x (BOM `2026.05.01`) + Material 3 |
| Build        | Gradle Kotlin DSL, AGP **9.3.2**                    |
| Playback     | Media3 (ExoPlayer + MediaSession) — declared, unused yet |
| Images       | Coil 3.3 (compose + okhttp network stack)           |
| Color        | androidx Palette                                    |
| Persistence  | DataStore Preferences                               |
| Networking   | OkHttp 5                                            |
| Serialization| kotlinx.serialization JSON                          |
| Testing      | JUnit4, Truth (+ Turbine, Robolectric, coroutines-test wired up) |

### Version pins are load-bearing (compileSdk 36)

This project compiles against **SDK 36** and `platforms;android-37` is NOT
available from this machine's SDK repository. The newest lines of several
libraries require compileSdk >= 37 and were therefore pinned back:

- `coil` **3.3.0** — 3.5.x is built with Kotlin 2.4; its stdlib metadata is
  unreadable by the Kotlin 2.2.10 compiler AGP 9.3.2 ships (build breaks).
- `composeBom` **2026.05.01** — BOMs after it pull Compose 1.12 which needs
  compileSdk 37.
- `coreKtx` **1.18.0**, `lifecycle` **2.10.0** — 1.19.x / 2.11.x need SDK 37.
- `okhttp` **5.4.0** — 5.5.0 needs SDK 37.

Do not "upgrade" these without solving the compileSdk problem first. To check
a version's requirement, read `minCompileSdk` from its AAR's
`META-INF/com/android/build/gradle/aar-metadata.properties`.

`material-icons-extended` is frozen upstream at 1.7.8 and no longer managed by
the Compose BOM, so its version is pinned explicitly in the catalog.

## Critical build detail: AGP 9 has no kotlin-android plugin

AGP 9 compiles Kotlin itself. There is intentionally **no**
`org.jetbrains.kotlin.android` plugin in this project — only the Compose and
serialization *compiler* plugins (`kotlin.plugin.compose`,
`kotlin.plugin.serialization`), version-pinned to the Kotlin matching the AGP
(`libs.versions.toml`). Do not "fix" the missing plugin; adding it will break
the build.

## Commands

Run from the repo root via the wrapper (`./gradlew`). Requires JDK 17+ (JDK 21
works on this machine) and the Android SDK (`local.properties` → compileSdk 36).

```bash
./gradlew assembleDebug          # build debug APK
./gradlew installDebug           # install on connected device/emulator
./gradlew testDebugUnitTest      # JVM unit tests (Truth; Robolectric available)
./gradlew connectedDebugAndroidTest  # instrumented tests (needs device)
./gradlew lintDebug              # Android lint
```

Verification gate before claiming anything works:
`./gradlew testDebugUnitTest lintDebug assembleDebug`.

There is no CI configured and no formatter/linter beyond Android lint.

## Project layout

```
app/src/main/java/com/sonara/
  MainActivity.kt          # single activity; splashscreen + edge-to-edge + shell
  SonaraApp.kt             # Application class; owns di.AppContainer
  di/AppContainer.kt       # hand-rolled container (NO DI framework) + Context.appContainer
  playback/
    PlaybackService.kt     # MediaSessionService + ExoPlayer; audio focus, noisy-pair, wake mode
    PlayerConnection.kt    # MediaController wrapper -> StateFlow<NowPlayingState>
    NowPlayingState.kt     # immutable UI state + duration formatting
    DemoCatalog.kt         # 5 bundled tracks backing the UI until the catalog stage
  ui/player/SonaraMiniPlayer.kt  # now-playing bar docked above the bottom nav
  ui/theme/
    Color.kt               # palette + extended roles (glass, player, gradients)
    Theme.kt               # always-dark scheme + LocalSonaraColors (sonaraColors())
    Type.kt                # premium type hierarchy (Light/Medium/SemiBold only)
  ui/designsystem/
    SonaraSpacing.kt       # spacing scale + semantic roles — NO ad-hoc dp in UI
    SonaraShapes.kt        # corner tokens incl. pill + player; asMaterialShapes()
    SonaraMotion.kt        # durations + easings — NO ad-hoc tween values in UI
  ui/components/
    SonaraSurface.kt       # Modifier.sonaraGlass() — translucent fill + hairline
    ArtworkPlaceholder.kt  # gradient stand-in for album art
    SectionHeader.kt       # section title + optional action label
  ui/shell/
    SonaraTab.kt           # Home / Search / Library / Flow enum with icons
    SonaraShell.kt         # AnimatedContent tab host + floating glass bottom bar
  ui/screens/{home,search,library,flow}/   # placeholder screens per tab
app/src/test/java/com/sonara/ui/DesignTokensTest.kt   # token invariant tests
app/src/test/java/com/sonara/playback/*.kt           # state + catalog tests
app/src/main/res/raw/demo_*.wav                      # generated ambient demo audio
app/src/main/res/values/themes.xml    # style IDs keep historical Theme.Luma names
```

- Package namespace is `com.sonara`; applicationId `com.sonara`
  (`com.sonara.debug` for debug builds). The folder is named `sonara`.
- Single-module project (`settings.gradle.kts` includes only `:app`).
- Resource/style IDs still say `Theme.Luma` (manifest-referenced); visible
  branding is Sonara everywhere.

## Conventions

- **Always-dark theme by design.** `ui/theme/Theme.kt` hard-codes
  `darkColorScheme`. Do not add day/night theming; runtime color comes from
  the future reactive background.
- **Semantic colors only.** Composables consume `sonaraColors()` /
  `MaterialTheme.colorScheme`, never raw `Color(0x…)`. Extended roles live in
  `LocalSonaraColors`; a later stage swaps their values for artwork-derived
  colors at that single injection point.
- **Tokens only.** Spacing via `SonaraSpacing`, corners via `SonaraShapes`,
  animation via `SonaraMotion`. New UI must not invent dp/duration/easing
  literals.
- **Playback access.** UI never touches ExoPlayer directly — it talks to
  `PlayerConnection` (obtained via `Context.appContainer`), which owns the
  `MediaController`. The player/session live in `PlaybackService`; the
  controller is activity-scoped and disposable, the service is not.
- Dependency versions only in `gradle/libs.versions.toml` (version catalog).
  Never hard-code versions in module build files.
- Repositories are locked down (`FAIL_ON_PROJECT_REPOS`); don't add repos to
  modules.
- Edge-to-edge is enabled; handle insets in composables (the shell does).
- `gradle.properties` forces IPv4 (`preferIPv4Stack=true`) because this
  machine's IPv6 egress stalls Maven/Google downloads — leave it alone even
  if your host doesn't need it.

## Environment notes

- `local.properties` holds the machine-specific SDK path; never commit or edit
  it manually.
- Building requires the Android SDK (compileSdk 36) and JDK 17+.

## Working rules

1. Inspect before editing; smallest safe change; follow existing style.
2. New features come with unit tests under `app/src/test/java/…`
   (Truth assertions; Turbine for flows; Robolectric where Android classes are
   involved). Note: reading CompositionLocals requires a compose test context —
   plain JUnit tests should assert on token objects directly.
3. Verify with `./gradlew testDebugUnitTest lintDebug assembleDebug` before
   claiming anything works.
