# AI Start 1.3

Quick access to your AI apps and accounts from the Android home screen.
Быстрый доступ к ИИ-приложениям и аккаунтам с главного экрана Android.

- **Language / Язык**: automatic (Russian if the phone is Russian, otherwise English), or choose in settings.
- **Menu by sections**: Choose AI, My AIs, Appearance, Widgets, Language, Backup, About and support.
- **Choose AI**: catalog by category (text, images, video, music and voice, code, research and work; about 90 tools including Google Flow, Flow Music, Stitch, Jules, Opal, AI Studio); expand a category and tick what you need.
- **My AIs**: several entries per AI (app, website in a chosen browser, other accounts), reorder, hide from widgets.
- **If the app is not installed**, a tap offers Google Play or the official website.
- **Widgets**: Rows (1-4 rows, 4-6 buttons, up to 6x4 cells), Columns (vertical, 1-2 columns), Groups (a row per AI).
- **Aquarium mode**: buttons sway in the water, each in its own style and rhythm; shuffle button; paging with arrows.
- Icon shape: rounded or round. Icons come from installed apps, otherwise letter tiles.
- Backup of settings as text, quick add of widgets, diagnostics, donate button (set the link in `Links.kt`).

No permissions, no internet access, nothing collected.

Android limit: a widget cannot read motion sensors or scroll sideways by swipe, so aquarium motion is an imitation
(automatic frame changes) and pages are turned with arrows.

## Signing
The project includes the standard public debug key `app/debug.keystore` (password `android`) so new builds update
the installed app in place. Use it for personal installs only.

## Build
1. Upload the project files to a GitHub repository (branch main), including `.github/workflows/build.yml`.
2. Actions, Build APK, wait for the green check.
3. Download `ai-hub-apk` from Artifacts, unzip and install `app-debug.apk`.
