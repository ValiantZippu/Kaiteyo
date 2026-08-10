# Code Signing & Notarization

Signing rules per platform. When a platform requires it (Windows, macOS), the
**release pipeline refuses to publish unsigned artifacts** — we respect the OS,
we never ask users to bypass security dialogs.

## Windows

- **Certificate**: an OV/EV code-signing cert (SHA-256 only).
- **Tool**: `signtool.exe` from the Windows SDK.
- **Signing**: the installer EXE is signed with a timestamp server
  (`/tr http://timestamp.digicert.com /td SHA256`). The inner MSI, when shipped,
  is signed the same way.
- **SmartScreen**: with an EV cert + `Signtool` timestamp, downloads show the
  publisher name ("syt0r"). No EV → expect "Unknown publisher"; document it.
- **CI secrets**: `WINDOWS_CERT_BASE64`, `WINDOWS_CERT_PASSWORD`. The workflow
  decodes the cert into a temp `.pfx`, signs, then deletes it.

```powershell
# local (thumbprint)
powershell -File installer/windows/build.ps1 -Version 2.2.1 -Sign -CertThumbprint "<sha1>"
```

## macOS

- **Developer ID Application** certificate + **notarization** is mandatory for
  a clean Gatekeeper experience.
- **Hardened Runtime**: enabled on every binary, including bundled JRE dylibs
  (`codesign --options runtime`), with `installer/macos/entitlements.plist`
  (JIT + library-validation exceptions).
- **Notarization**: `xcrun notarytool submit --wait` then `stapler staple`.
- **CI secrets**: `APPLE_ID`, `APPLE_APP_PASSWORD` (app-specific), `APPLE_TEAM_ID`,
  `CODESIGN_IDENTITY`.

```bash
APPLE_ID=me@example.com APPLE_APP_PASSWORD=xxxx APPLE_TEAM_ID=ABCDE12345 \
CODESIGN_IDENTITY="Developer ID Application: syt0r (ABCDE12345)" \
bash installer/macos/notarize.sh path/to/Kaiteyo-2.2.1-macos-arm.dmg
```

### Verifying before publish

```bash
codesign --verify --deep --strict --verbose=2 Kaiteyo.app
spctl --assess --type execute --verbose=4 Kaiteyo.app   # → accepted
xcrun stapler validate Kaiteyo-2.2.1-macos-arm.dmg      # → The validate action worked!
```

## Linux

- No central signing authority. deb/rpm ship unsigned (standard practice).
- **Integrity**: every artifact ships with a sha256 in `artifact-manifest.json`,
  and the update feed verifies hashes before install.
- Flatpak: Flathub signs with its own key on publish.

## Policy summary

| Platform | Required | Tool | CI behavior when credentials are missing |
|----------|----------|------|------------------------------------------|
| Windows | Code signature | signtool | Job skips signing (`build.ps1 -Sign` only) |
| macOS | Signature + notarization | codesign + notarytool | `notarize.sh` skips cleanly; the DMG uploads unsigned with a warning |
| Linux | — (sha256 manifest) | — | Release gate still runs (integrity only) |

**Honest note:** until signing secrets are configured in GitHub Actions, release
DMGs and EXEs ship **unsigned**. Until then, call it out in the release notes
and treat signing as the #1 item on the release checklist. The integrity gate
(`verify-artifacts.sh`) always runs regardless of signing.
