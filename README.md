<div align="center">

# WikiReader Bilingual

A bilingual Android reader for learning with Wikipedia articles.

<a href="https://github.com/NihilDigit/WikiReader-Bilingual/releases/latest">
  <img src="https://img.shields.io/github/v/release/NihilDigit/WikiReader-Bilingual?logo=github&labelColor=1a1a1a">
</a>
<a href="https://github.com/NihilDigit/WikiReader-Bilingual/blob/main/LICENSE">
  <img src="https://img.shields.io/github/license/NihilDigit/WikiReader-Bilingual?logo=gnu&color=blue&labelColor=1a1a1a">
</a>
<img src="https://img.shields.io/badge/API-26+-blue?logo=android&labelColor=1a1a1a">

</div>

WikiReader Bilingual keeps WikiReader's clean Wikipedia reading experience and adds AI-assisted bilingual reading for article content. It is intended for reading English or other source-language Wikipedia pages with a configurable target-language translation beside the original text.

The app translates Wikipedia content, not the app chrome. Labels such as `Featured article`, `Trending articles`, navigation items, and settings remain normal app UI. Article titles prefer Wikipedia's target-language title when a langlink exists; model translation is only a fallback.

## Bilingual Features

- Paragraph-by-paragraph source/target reading for article body content.
- Optional automatic paragraph translation, with manual translate placeholders when disabled.
- Optional blur for translated paragraphs until tapped.
- Plain-text translations for short descriptions, headings, captions, and gallery alt/caption text.
- AI sentence translation and contextual word explanation from article text.
- Local translation cache to reduce repeated API calls.

## Screenshots

Enable bilingual reading in Settings, enter a DeepSeek API key, and choose a target Wikipedia language such as `zh`.

<p align="center">
  <img src="https://github.com/user-attachments/assets/9d073c6d-4707-4031-962a-679eabc9983d" alt="Article title and description translated into Chinese" width="45%">
  <img src="https://github.com/user-attachments/assets/d408dcb8-cc72-4e70-9d2a-f00f76439da2" alt="Paragraph translation under the original Wikipedia text" width="45%">
  <br>
  <sub>Use target-language Wikipedia titles when available.</sub>
  &nbsp;&nbsp;&nbsp;
  <sub>Tap a blurred paragraph translation to reveal it.</sub>
  <br>
  <img src="https://github.com/user-attachments/assets/5f8aec3e-2cdd-4211-8339-11c285bced6e" alt="Contextual word explanation in Chinese" width="45%">
  <img src="https://github.com/user-attachments/assets/201922e7-59a1-4f58-929f-01b675476601" alt="Double-tap sentence translation dialog" width="45%">
  <br>
  <sub>Tap a word, then tap the chip for a contextual explanation.</sub>
  &nbsp;&nbsp;&nbsp;
  <sub>Double-tap a sentence for quick sentence translation.</sub>
  <br>
  <img src="https://github.com/user-attachments/assets/27c54d22-84d4-41b3-abf5-7932f83171db" alt="Bilingual reading settings with API key, target language, blur, auto translation, and concurrency controls" width="45%">
  <br>
  <sub>Configure API key, target language, blur, automatic translation, and concurrency.</sub>
</p>

## Translation Backend

Translation uses a DeepSeek OpenAI-compatible endpoint. Configure it in Settings:

- DeepSeek API key
- Target Wikipedia language, for example `zh`
- Translation concurrency, clamped to `1..16`
- Default model: `deepseek-v4-flash`

DeepSeek v4 thinking is disabled for translation requests. The app skips reference-style sections such as References, Further reading, External links, Notes, Footnotes, and Bibliography to avoid unnecessary API spend.

## Download

Signed APKs are published from GitHub Releases. The Android package name is `dev.nihildigit.wikireader.bilingual`, so this fork can be installed beside upstream WikiReader.

## Project Notes

This fork is maintained by NihilDigit and remains GPL-3.0 because the original project is GPL-3.0. WikiReader was originally created by Nishant Mishra. The Kotlin source namespace intentionally stays close to upstream so rebasing from `nsh07/WikiReader` remains practical.

The original upstream README follows.

---

<div align="center">

<img src="app/src/main/ic_launcher-playstore.png" width="128">

# WikiReader

</div>

---

<div align="center">

<a href="https://hosted.weblate.org/engage/wikireader/?utm_source=widget">
  <img src="https://img.shields.io/weblate/progress/wikireader?logo=weblate&labelColor=1a1a1a&color=2ecba9">
</a>
<a href="https://github.com/nsh07/WikiReader/releases/latest">
  <img src="https://img.shields.io/github/v/release/nsh07/WikiReader?logo=github&labelColor=1a1a1a">
</a>
<a href="https://f-droid.org/packages/org.nsh07.wikireader">
  <img src="https://img.shields.io/f-droid/v/org.nsh07.wikireader?logo=f-droid&labelColor=1a1a1a">
</a>
<a href="https://github.com/nsh07/WikiReader/blob/main/LICENSE">
  <img src="https://img.shields.io/github/license/nsh07/wikireader?logo=gnu&color=blue&labelColor=1a1a1a">
</a>
<img src="https://img.shields.io/badge/API-26+-blue?logo=android&labelColor=1a1a1a">
<br/><br/>

A lightweight Android app for reading Wikipedia articles distraction-free

Supports light mode, dark mode, Material You dynamic colors and 300+ Wikipedia languages

<p>
  <a href="https://apt.izzysoft.de/fdroid/index/apk/org.nsh07.wikireader">
    <img src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroid.png" width="200">
  </a>
  <a href="https://f-droid.org/packages/org.nsh07.wikireader">
      <img src="https://f-droid.org/badge/get-it-on.png" width="200">
  </a>
</p>
<p>
  <a href="https://hosted.weblate.org/engage/wikireader/">
    <img src="https://hosted.weblate.org/widget/wikireader/287x66-black.png" alt="Translation status" />
  </a>
</p>

</div>

---

## Screenshots

<p align="center" width="100%">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" width="30%">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/2.png" width="30%">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/3.png" width="30%">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/4.png" width="30%">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/5.png" width="30%">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/6.png" width="30%">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/7.png" width="30%">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/8.png" width="60%">
</p>

## Features

- **Fast loading:** The article text is loaded before anything else, so you can get to reading,
  quick.
- **Feed:** Read the article of the day, find trending articles and get up to speed on the news with
  the Wikipedia Feed
- **Article image:** View an image of the topic from its Wikipedia page. Click on it to enlarge it
  and view in full-screen
- **Random article:** Feeling lucky? Click the random article button to read a random article
- **Choose your language:** Choose from over 300 languages on Wikipedia
- **Save articles:** Download articles to your device for offline reading
- **One-handed use:** Use the floating action buttons at the bottom for a complete one-handed
  experience
- **Immersive mode:** Hide UI elements while scrolling and enjoy an immersive reading experience
- **Lightweight:** The app starts instantly, and works smoothly
- **Material 3 Expressive:** Designed according to the latest Material 3 Expressive design
  guidelines
- **Smooth animations:** Smooth and fluent animations
- **Customizable colors:** Choose from light/dark themes and customize the Material 3 color palette
- **Customizable font size:** Choose your own comfortable font size
- **Data saver:** Save your limited data plan by loading text only
- **Math expressions:** View properly rendered mathematical expressions for easily reading
  mathematical articles

## Translation

This project is [available on Hosted Weblate](https://hosted.weblate.org/engage/wikireader/) for
translation.

You can contribute to this project even if you are not a developer by helping in
translating this project into languages you know.

<a href="https://hosted.weblate.org/engage/wikireader/">
<img src="https://hosted.weblate.org/widget/wikireader/horizontal-auto.svg" alt="Translation status" />
</a>

## Download

- **F-Droid** (recommended): WikiReader is available for download on the
  official [F-Droid app store repository](https://f-droid.org/). Updates on F-Droid are generally a
  week late. To get faster updates, you can download WikiReader from
  the [IzzyOnDroid repository](https://apt.izzysoft.de/fdroid/)
- **Obtainium** (recommended): You can add this GitHub repository
  on [Obtainium](https://obtainium.imranr.dev/) to get updates directly from GitHub releases. This
  is the fastest way to install and update WikiReader.
- **GitHub releases**: Alternatively, you can manually download and install APKs from
  the [Releases](https://github.com/nsh07/WikiReader/releases/latest) section of this repo (This
  method is not recommended, use Obtainium instead).

> [!TIP]
> To [verify](https://developer.android.com/studio/command-line/apksigner#usage-verify) the APK
> downloaded from Obtainium/GitHub, use the following signing certificate fingerprints:
> ```
> SHA1: B1:4E:17:93:11:E8:DB:D5:35:EF:8D:E9:FB:8F:FF:08:F8:EC:65:08
> SHA256: 07:BE:F3:05:81:BA:EE:8F:45:EC:93:E4:7E:E6:8E:F2:08:74:E5:0E:F5:70:9C:78:B2:EE:67:AC:86:BE:4C:3D
> ```
> The SHA256 and MD5 hashes of the individual APK files are also available in the `checksum.txt`
> file for each release.

## Donate

You can support WikiReader's development
through [my GitHub Sponsors page](https://github.com/sponsors/nsh07)
or [my BuyMeACoffee page](https://coff.ee/nsh07):

<a href="https://github.com/sponsors/nsh07">
  <img src=".github/repo_photos/sponsors.png" width="128px">
</a>
<a href="https://coff.ee/nsh07">
  <img src=".github/repo_photos/bmc_qr.png" width="128px">
</a>

## Special Thanks

- [Wikimedia Foundation](https://wikimediafoundation.org/): For
  providing [Wikipedia](https://wikipedia.org), the largest and most-read reference work in history,
  for free
- All the Wikipedia volunteers, contributors and donors

This app was made possible by the following libraries:

- [Retrofit 2](https://square.github.io/retrofit/) - REST APIs
- [OkHttp](https://square.github.io/okhttp/) - Networking
- [Coil](https://coil-kt.github.io/coil/) - Loading images from the web
- [MaterialKolor](https://github.com/jordond/MaterialKolor) - For custom color themes
- [ComposeCharts](https://github.com/ehsannarmani/ComposeCharts) - For the view-count history graph in the feed
- [Latex2Unicode](https://github.com/tomtung/latex2unicode) - For converting math into Unicode text

## Star History

<a href="https://star-history.com/#nsh07/wikireader&Date">
 <picture>
   <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/svg?repos=nsh07/wikireader&type=Date&theme=dark" />
   <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/svg?repos=nsh07/wikireader&type=Date" />
   <img alt="Star History Chart" src="https://api.star-history.com/svg?repos=nsh07/wikireader&type=Date" />
 </picture>
</a>
