---
title: Download
description: Get Kaiteyo for Windows, macOS, Linux, Android, or iOS — free and open source. Stable and beta channels, all architectures.
---

<div class="prose-note">
  <svg class="icon" aria-hidden="true"><use href="#icon-info"/></svg>
  <strong>Not sure yet?</strong> Take a look at the <a href="/features/">feature overview</a> and <a href="/screenshots/">screenshots</a> first — or install and start with the built-in sample decks.
</div>

## Platforms

Every release ships as a direct download on GitHub — no app store, no account, no tracking. Installers are built by Kaiteyo's own packaging pipeline (see the [installer docs](/docs/platform/readme/) for what each format contains).

<div class="platform-grid">
  <div class="card platform-card">
    <div class="feature-icon"><svg class="icon" aria-hidden="true"><use href="#icon-windows"/></svg></div>
    <h3>Windows</h3>
    <p>Custom Inno Setup installer (EXE), plus MSI and a portable ZIP. Windows 10 and 11, x64.</p>
    <a class="btn btn-primary btn-sm" href="https://github.com/ValiantZippu/Kaiteyo/releases" target="_blank" rel="noopener">
      <svg class="icon" aria-hidden="true"><use href="#icon-download"/></svg>
      Get on GitHub
    </a>
  </div>
  <div class="card platform-card">
    <div class="feature-icon"><svg class="icon" aria-hidden="true"><use href="#icon-apple"/></svg></div>
    <h3>macOS</h3>
    <p>DMG, signed and notarized. Universal builds for Apple silicon and Intel Macs.</p>
    <a class="btn btn-primary btn-sm" href="https://github.com/ValiantZippu/Kaiteyo/releases" target="_blank" rel="noopener">
      <svg class="icon" aria-hidden="true"><use href="#icon-download"/></svg>
      Get on GitHub
    </a>
  </div>
  <div class="card platform-card">
    <div class="feature-icon"><svg class="icon" aria-hidden="true"><use href="#icon-linux"/></svg></div>
    <h3>Linux</h3>
    <p>AppImage, .deb and .rpm packages. Flatpak and Snap manifests live in the repository.</p>
    <a class="btn btn-primary btn-sm" href="https://github.com/ValiantZippu/Kaiteyo/releases" target="_blank" rel="noopener">
      <svg class="icon" aria-hidden="true"><use href="#icon-download"/></svg>
      Get on GitHub
    </a>
  </div>
  <div class="card platform-card">
    <div class="feature-icon"><svg class="icon" aria-hidden="true"><use href="#icon-android"/></svg></div>
    <h3>Android</h3>
    <p>APK for Android 8.0 and newer. F-Droid flavor is Google-free and reproducible.</p>
    <a class="btn btn-primary btn-sm" href="https://github.com/ValiantZippu/Kaiteyo/releases" target="_blank" rel="noopener">
      <svg class="icon" aria-hidden="true"><use href="#icon-download"/></svg>
      Get on GitHub
    </a>
  </div>
  <div class="card platform-card">
    <div class="feature-icon"><svg class="icon" aria-hidden="true"><use href="#icon-ios"/></svg></div>
    <h3>iOS & iPadOS</h3>
    <p>Builds for iPhone and iPad (TestFlight).</p>
    <a class="btn btn-primary btn-sm" href="https://github.com/ValiantZippu/Kaiteyo/releases" target="_blank" rel="noopener">
      <svg class="icon" aria-hidden="true"><use href="#icon-download"/></svg>
      Get on GitHub
    </a>
  </div>
  <div class="card platform-card">
    <div class="feature-icon"><svg class="icon" aria-hidden="true"><use href="#icon-globe"/></svg></div>
    <h3>Any device</h3>
    <p>Your study data, decks and progress live on your device — no account, no tracking, fully offline.</p>
    <a class="btn btn-secondary btn-sm" href="/features/">
      <svg class="icon" aria-hidden="true"><use href="#icon-layers"/></svg>
      See the features
    </a>
  </div>
</div>

## Release channels

| Channel | Who it's for | Notes |
|---|---|---|
| **Stable** | Everyone | Tested releases, tagged on GitHub. Recommended default. |
| **Beta** | Early adopters, testers | Preview of the next stable; may contain rough edges. |
| **Nightly / development** | Contributors | Built from the development branch; expected to break. |

All channels are listed on the [releases page](https://github.com/ValiantZippu/Kaiteyo/releases). Older versions are always available there too.

## Architecture

- **Windows** — x64
- **macOS** — universal (Apple silicon + Intel)
- **Linux** — x64 (AppImage/deb/rpm)
- **Android** — arm64, armv7 and x86_64 APKs

If you're unsure which architecture you need, the universal or x64 build is the safe choice on desktop.

## The Kaiteyo installer

Windows, macOS and Linux installers are Kaiteyo's own — branded, scripted, and reproducible from the repository (`installer/` in the source tree). They handle first-launch setup the same way on every platform, so the experience is consistent whether you install on Windows, macOS or Linux. See [installation guides](/install/) for step-by-step instructions.

## Verify your download

Installer checksums are listed next to every release asset on GitHub.

- **Windows** — right-click the installer → *Properties* → *File hashes*
- **macOS / Linux** — run `shasum -a 256` (or `sha256sum`) on the downloaded file and compare with the release notes

## From source

You can also build Kaiteyo yourself. The [development guide](/docs/development/developer_guide/) covers build requirements, the recommended toolchain, and the exact commands — everything runs from a single Kotlin Multiplatform codebase.
