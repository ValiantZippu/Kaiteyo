---
title: Download
description: Get Kaiteyo for Windows, macOS, Linux, Android, or iOS — free and open source.
---

## Platforms

<div class="platform-grid">
  <div class="card platform-card">
    <div class="feature-icon"><svg class="icon" aria-hidden="true"><use href="#icon-windows"/></svg></div>
    <h3>Windows</h3>
    <p>Installers for Windows 10 and 11 (x64 and arm64).</p>
    <a class="btn btn-primary btn-sm" href="{{ site.repository }}/releases" target="_blank" rel="noopener">
      <svg class="icon" aria-hidden="true"><use href="#icon-download"/></svg>
      Get on GitHub
    </a>
  </div>
  <div class="card platform-card">
    <div class="feature-icon"><svg class="icon" aria-hidden="true"><use href="#icon-apple"/></svg></div>
    <h3>macOS</h3>
    <p>Universal builds for Apple silicon and Intel Macs.</p>
    <a class="btn btn-primary btn-sm" href="{{ site.repository }}/releases" target="_blank" rel="noopener">
      <svg class="icon" aria-hidden="true"><use href="#icon-download"/></svg>
      Get on GitHub
    </a>
  </div>
  <div class="card platform-card">
    <div class="feature-icon"><svg class="icon" aria-hidden="true"><use href="#icon-linux"/></svg></div>
    <h3>Linux</h3>
    <p>AppImage and .deb packages for popular distributions.</p>
    <a class="btn btn-primary btn-sm" href="{{ site.repository }}/releases" target="_blank" rel="noopener">
      <svg class="icon" aria-hidden="true"><use href="#icon-download"/></svg>
      Get on GitHub
    </a>
  </div>
  <div class="card platform-card">
    <div class="feature-icon"><svg class="icon" aria-hidden="true"><use href="#icon-android"/></svg></div>
    <h3>Android</h3>
    <p>APK releases for Android 8.0 and newer.</p>
    <a class="btn btn-primary btn-sm" href="{{ site.repository }}/releases" target="_blank" rel="noopener">
      <svg class="icon" aria-hidden="true"><use href="#icon-download"/></svg>
      Get on GitHub
    </a>
  </div>
  <div class="card platform-card">
    <div class="feature-icon"><svg class="icon" aria-hidden="true"><use href="#icon-ios"/></svg></div>
    <h3>iOS & iPadOS</h3>
    <p>Builds for iPhone and iPad (TestFlight).</p>
    <a class="btn btn-primary btn-sm" href="{{ site.repository }}/releases" target="_blank" rel="noopener">
      <svg class="icon" aria-hidden="true"><use href="#icon-download"/></svg>
      Get on GitHub
    </a>
  </div>
</div>

<div class="prose-note">
  <svg class="icon" aria-hidden="true"><use href="#icon-info"/></svg>
  <strong>No app store required.</strong> Releases ship as direct downloads on GitHub —
  no account, no tracking, no telemetry. Older versions are always available on the
  <a href="{{ site.repository }}/releases" target="_blank" rel="noopener">releases page</a>.
</div>

## Verify your download

Installer checksums are listed next to every release asset on GitHub. On Windows, right-click the installer → <em>Properties</em> → <em>File hashes</em> to compare; on macOS and Linux, use `shasum -a 256` on the downloaded file.

## From source

You can also build Kaiteyo yourself. See the [Development Guide](/docs/development/) section of the documentation for build instructions, requirements, and the recommended toolchain.
