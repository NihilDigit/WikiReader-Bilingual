# Repository Guidelines

## Project Structure & Module Organization

WikiReader is a Kotlin Android app built with Gradle Kotlin DSL. The main app lives in `app/`, with source under `app/src/main/java/org/nsh07/wikireader`. Important packages include `ui/` for Jetpack Compose screens, `data/` for Room entities and repositories, `network/` for Wikipedia API access, and `parser/` for wikitext handling. Resources are in `app/src/main/res`, including localized strings in `values-*`. Unit tests are in `app/src/test`, instrumentation tests are in `app/src/androidTest`, Room schemas are in `app/schemas`, baseline profile code is in `baselineprofile/`, and store metadata/screenshots are in `fastlane/metadata/android`.

## Build, Test, and Development Commands

Use the checked-in Gradle wrapper.

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
./gradlew lintDebug
./gradlew :baselineprofile:connectedAndroidTest
./scripts/build-local-release.sh 26.5.0
```

`assembleDebug` builds a local debug APK. `testDebugUnitTest` runs JVM tests such as parser and data utilities. `connectedDebugAndroidTest` requires an emulator or device. `lintDebug` runs Android lint. The baseline profile task requires a connected benchmark-capable device or emulator.
`scripts/build-local-release.sh` runs JVM tests and a signed `assembleRelease`; it requires the untracked `release-signing.properties` file.

## Coding Style & Naming Conventions

Target Java/Kotlin 17 and follow standard Kotlin style with four-space indentation. Prefer Compose functions named in `PascalCase`, ViewModels ending in `ViewModel`, UI events ending in `Action`, and state holders ending in `State`. Keep feature UI near its screen package, for example `ui/homeScreen`, and reusable theme code in `ui/theme`. Store dependency versions in `gradle/libs.versions.toml`.

## Testing Guidelines

Add focused JVM tests under `app/src/test/java` for pure Kotlin logic, parsers, repository helpers, and data utilities. Add instrumentation or Compose UI tests under `app/src/androidTest/java` when behavior depends on Android framework APIs, Room integration, or rendered UI. Name test files after the unit under test, for example `MiscKtTest.kt`.

## Commit & Pull Request Guidelines

Recent history uses short imperative subjects with optional Conventional Commit prefixes, such as `fix: ...`, `feat: ...`, and `chore: ...`; follow that style and keep each commit scoped. Pull requests should include a concise description, testing performed, linked issues when applicable, and screenshots or screen recordings for visible UI changes. Mention translation, schema, baseline profile, or Fastlane metadata changes explicitly so reviewers can check release impact.

## Security & Configuration Tips

Do not commit local signing keys, device-specific files, or generated build outputs. Keep API and networking changes compatible with Wikimedia/Wikipedia endpoints, and avoid storing article or preference data outside the existing Room and preferences layers without documenting the migration path.

## Fork Decisions

This fork is named `WikiReader Bilingual`. The installable Android `applicationId` is `dev.nihildigit.wikireader.bilingual`; keep the Kotlin source namespace close to upstream unless a change is required, so upstream rebases stay manageable. The project remains GPL-3.0 because the original repository is GPL-3.0; retain upstream copyright and license notices when distributing modified builds.

For bilingual reading, use an OpenAI-compatible API provider rather than Codex OAuth. The built-in default backend is DeepSeek with `baseUrl = https://api.deepseek.com` and `model = deepseek-v4-flash`; keep base URL, model, target translation language, and API key configurable. Prefer a maintained client library for provider calls instead of hand-written HTTP protocol code. Codex OAuth/private ChatGPT backend routes are intentionally out of scope for this fork.

Machine translation is for Wikipedia content only, not the app framework. Do not translate app chrome such as settings labels, `Featured article`, `Trending articles`, navigation labels, or other first-party UI strings. Article titles should prefer the target-language Wikipedia title from existing langlinks when available; only fall back to model translation when no target-language title exists. Article body paragraphs remain eligible for bilingual source/target rendering.

Current bilingual settings live in the normal Settings screen. Keep defaults aligned with `TranslationConfig`: DeepSeek base URL `https://api.deepseek.com`, model `deepseek-v4-flash`, target language `zh`, `user_id = wikireader-bilingual`, and concurrency `4` clamped to `1..16`. Translation requests should skip references-style sections such as References, Further reading, External links, Notes, Footnotes, and Bibliography.

Live provider smoke tests are opt-in to protect personal API keys and spend:

```bash
DEEPSEEK_API_KEY=... RUN_LIVE_TRANSLATION_SMOKE=true ./gradlew testDebugUnitTest --tests org.nsh07.wikireader.translation.TranslationRepositorySmokeTest.deepSeekProvider_translatesShortText_whenApiKeyProvided
```

Keep bilingual translation changes isolated in new provider/repository/UI helpers where possible. Existing upstream files should receive minimal connection edits only, reducing conflicts when syncing with upstream through a normal remote plus rebase workflow.

## Upstream Sync

Use `origin` for `NihilDigit/WikiReader-Bilingual` and `upstream` for `nsh07/WikiReader`. Keep long-lived fork changes small and isolated so `git fetch upstream` plus rebase remains practical. Do not rename the Kotlin package tree only to match the fork package name; the Android `applicationId` provides install separation while preserving upstream mergeability.

## Release Workflow

Release builds are tag-driven. CI creates the GitHub Release and attaches the signed APK plus `SHA256SUMS.txt`; do not hand-build packages for public release.

1. Finish code, docs, version metadata, signing config, and CI changes in normal commits.
2. Run a local signed release smoke test before tagging:

   ```bash
   ./scripts/build-local-release.sh 26.5.0
   ```

3. Create and push a non-interactive time-based tag in `YY.M.patch` format:

   ```bash
   git tag -a 26.5.0 -m "WikiReader Bilingual 26.5.0"
   git push origin main 26.5.0
   ```

4. Wait for CI to build the signed release APK and create or update the GitHub Release.
5. After CI succeeds, edit the generated Release title/body with `gh release edit` or the GitHub UI.

`assembleRelease` must fail when release signing material is missing. Local signing material lives in untracked `release-signing.properties` and `.signing/`. CI signing material lives in GitHub Secrets named `NIHILDIGIT_RELEASE_KEYSTORE_BASE64`, `NIHILDIGIT_RELEASE_STORE_PASSWORD`, `NIHILDIGIT_RELEASE_KEY_ALIAS`, and `NIHILDIGIT_RELEASE_KEY_PASSWORD`. GitHub Secrets cannot be read back with `gh`; local release builds need a local copy of the same keystore and passwords.
