# Pvot Documents

A documents app that you need but is not provided to you: one private, offline place to open,
organize and work with the documents on your phone. Built on the
[Pvot design system](https://github.com/PVOT-OSS/PvotLib).

> **Version 1.0.0.** It opens PDF, Word, OpenDocument, Excel, PowerPoint, EPUB, CSV, Markdown,
> HTML, JSON and text, all view-only; any other file opens as text or raw bytes, or in another app.
> Editing, the PDF tools, archives and the scanner are not built yet.

## Screenshots

<p align="center">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" alt="Recent, with filters by type" width="220">
  &nbsp;
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/2.png" alt="Searching a PDF, with every match highlighted" width="220">
  &nbsp;
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/3.png" alt="A Word report with headings, a list and a table" width="220">
</p>
<p align="center">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/4.png" alt="An Excel budget with fills and number formats" width="220">
  &nbsp;
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/5.png" alt="A PowerPoint deck drawn as laid out" width="220">
  &nbsp;
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/6.png" alt="An EPUB book" width="220">
</p>

## Roadmap

| Phase | What it brings |
|---|---|
| 0. Scaffold | Project, PvotLib, app shell with Recent, Tools, Manage and Settings, CI |
| 1. Library | Folders through the system picker, "Open with" (with an autosave-or-not choice), recents with type filters, search, sort, list and grid, multi-select, share, rename, delete to Recently deleted, new file / scan from Tools |
| 2. Core viewers | PDF (zoom, thumbnails, search, go to page, night mode, resume), text, code, Markdown, CSV, HTML, JSON, DOCX |
| 3. Office formats | XLSX/ODS sheets, PPTX/ODP slides, ODT, EPUB, and any other file as text or bytes |
| 4. Editing and PDF tools | Edit text, Markdown and CSV; PDF markup and signatures; merge, split, reorder, rotate, images to PDF, compress, password |
| 5. Scanner | Camera capture, edge detection, perspective correction, filters, multi-page PDF, OCR |
| 6. Later | On-device AI, tablet two-pane layout, widgets, printing, F-Droid |

## Principles

- **Private and offline.** No network access, no analytics, no storage permission: documents are
  reached through the Storage Access Framework, one folder or file at a time.
- **Fully open source.** Every dependency is free software, so the app can ship on F-Droid.
- **One Pvot look.** Every screen uses PvotLib's components and tokens.

## Building

PvotLib is a git submodule, built as an included build:

```bash
git clone --recurse-submodules https://github.com/PVOT-OSS/Pvot-Documents.git
```

```bash
./gradlew :app:assembleDebug
```

Requires JDK 21 and the Android SDK (compile SDK 37.1). Min SDK is 27.

## Versioning

Releases follow [semantic versioning](https://semver.org): `MAJOR.MINOR.PATCH`, set in the root
`build.gradle.kts`. The version code is `MAJOR * 10000 + MINOR * 100 + PATCH` (1.0.0 is 10000), so
minor and patch numbers stay below 100. Each release gets a tag `vMAJOR.MINOR.PATCH` and a changelog
in `fastlane/metadata/android/en-US/changelogs/<version code>.txt`.

## Credits

Third-party notices are in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

## License

GPL-3.0-or-later. See [LICENSE](LICENSE).
