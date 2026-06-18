# Repository Guidelines

## Project Structure & Module Organization
Shappky is a single-module Android application. Core Java source lives in `app/src/main/java/com/yn/shappky`, with feature helpers split into `adapter/`, `model/`, `shizuku/`, and `util/`. Android resources are under `app/src/main/res`, including layouts in `layout/`, menus in `menu/`, theme values in `values/`, and drawables/colors in their matching folders. The Shizuku AIDL contract is in `app/src/main/aidl`. Release store metadata is kept in `fastlane/metadata/android/en-US`, and documentation images are in `docs/images`.

## Build, Test, and Development Commands
Use the checked-in Gradle wrapper.

- `./gradlew assembleDebug` builds a local debug APK.
- `./gradlew build` runs the standard Android build, lint, and verification tasks.
- `./gradlew lint` runs Android lint using `app/lint-baseline.xml` when present.
- `./gradlew updateLintBaseline` refreshes the lint baseline; use only when accepting known lint findings.
- `./gradlew clean` removes generated Gradle build output.

CI in `.github/workflows/android.yml` uses JDK 17 and runs `./gradlew updateLintBaseline` followed by `./gradlew build`.

## Coding Style & Naming Conventions
This project uses Java 17, AndroidX, Material Components, view binding, libsu, and Shizuku. Follow existing Java style: 4-space indentation, `PascalCase` classes, `camelCase` fields and methods, and `UPPER_SNAKE_CASE` constants. Keep package paths under `com.yn.shappky`. Name XML resources descriptively by type and role, such as `activity_main.xml`, `item_filter_app.xml`, and `switch_thumb_tint.xml`. Prefer view binding over manual `findViewById` for new UI code.

## Testing Guidelines
No unit or instrumentation test directories are currently present. Add JVM tests under `app/src/test/java` and Android instrumentation tests under `app/src/androidTest/java` when introducing logic with meaningful branching or platform behavior. Name test classes after the target class, for example `ShellManagerTest` or `BackgroundAppManagerTest`. Run `./gradlew test` for local JVM tests and `./gradlew connectedAndroidTest` when device-backed tests are added.

## Commit & Pull Request Guidelines
Recent history mostly uses short Conventional Commit-style messages such as `feat: add search bar to filter dialog`; keep that format for feature and fix work (`feat:`, `fix:`, `chore:`). Pull requests should describe user-visible behavior, list validation commands run, link related issues, and include screenshots or screen recordings for UI changes. Mention permission-mode impacts when touching Root, Shizuku, services, or Quick Tile behavior.

## Security & Configuration Tips
Do not commit signing keys, local Gradle caches, generated APKs, or device-specific configuration. Treat shell execution and app-killing logic as security-sensitive: preserve protected-app safeguards and test both Root and Shizuku paths when changing process control code.
