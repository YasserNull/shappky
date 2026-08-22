# Changelog

## About the App
- Updated app icon to support monochrome dynamic icons ([#6](https://github.com/YasserNull/shappky/issues/6)).
- Changed app package name from `com.yn.shappy` to `com.yassernull.shappky`.
- Raised minimum supported Android version from 6.0 to 7.0 due to Shizuku library updates.
- Migrated codebase and tech stack from XML & Java to Kotlin & Jetpack Compose.

## User Interface
Numerous UI improvements and visual enhancements, including:
- Added icons to buttons.
- Added type-specific indicators/icons for apps.
- Added app search bar, which was also integrated into the "Hide App" menu ([#17](https://github.com/YasserNull/shappky/issues/17)).
- Fixed layout and design bugs ([#13](https://github.com/YasserNull/shappky/issues/13), [#15](https://github.com/YasserNull/shappky/issues/15)).
- Added smooth animations across the app list.
- Added a long-press context dialog showing detailed app information with direct actions to mark an app as protected or hidden ([#17](https://github.com/YasserNull/shappky/issues/17)).

## App-Killing Algorithm & Background Detection
- Enhanced the app-killing algorithm to intelligently stop applications and terminate stubborn background processes if an application fails to stop normally.
- Improved background app detection logic to identify individual sub-processes rather than monitoring only the main process.
- For full technical details, see [`docs/how_shappky_works.md`](docs/how_shappky_works.md).

## Settings
Introduced a dedicated Settings screen featuring multiple customization options:
- **Language**: Added support for English and Arabic, with community localization support.
- **Theme**: Added Light, Dark, and OLED Black themes ([#9](https://github.com/YasserNull/shappky/issues/9)).
- **Dynamic Colors**: Added Material You dynamic color scheme support based on system wallpaper ([#6](https://github.com/YasserNull/shappky/issues/6)).
- **Permission Switching**: Seamlessly toggle between Root and Shizuku authorization modes.
- **Protected Apps**: Manage protected applications globally to exempt them from background termination.
- **Backup & Restore**: Export and import application settings ([#17](https://github.com/YasserNull/shappky/issues/17)).

## Shappky Service
- Added configuration options to customize the Shappky background service, preventing performance issues or system lag caused by killing essential background tasks ([#12](https://github.com/YasserNull/shappky/issues/12)).

## Triggers
Implemented trigger features ([#4](https://github.com/YasserNull/shappky/issues/4), [#16](https://github.com/YasserNull/shappky/issues/16)):
1. **Profile Trigger**: A manual trigger allowing instant termination of a predefined group of apps with one tap.
2. **Service Trigger**: A rule-based background service trigger that automatically enables or disables execution based on customizable conditions.

## Tasker Integration
- Added Tasker plugin support for seamless integration and custom automated workflows ([#2](https://github.com/YasserNull/shappky/issues/2)).

## Intent
- Added Broadcast Intent support to enable external apps and automation tools to trigger commands directly ([#17](https://github.com/YasserNull/shappky/issues/17), [#21](https://github.com/YasserNull/shappky/issues/21)).

## Widgets
- Added 2 interactive home screen widgets: Action Widget and App List Widget ([#19](https://github.com/YasserNull/shappky/issues/19)).

## Other Fixes & Improvements
- Updated and fixed donation link ([#18](https://github.com/YasserNull/shappky/issues/18)).
- Resolved issue regarding Shappky service status reporting ([#8](https://github.com/YasserNull/shappky/issues/8)).
