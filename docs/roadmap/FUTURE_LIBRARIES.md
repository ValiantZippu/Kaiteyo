# Kaiteyo — Future Libraries & Dependencies Roadmap

> Libraries Kaiteyo could adopt to accelerate development. Organized by domain,
> with priority (P0 = critical for next release, P1 = next quarter, P2 = future),
> KMP compatibility, and integration notes.

---

## 1. Language Processing & NLP

| Library | Priority | KMP | What It Does | Integration Notes |
|---------|----------|-----|-------------|-------------------|
| **MeCab / UniDic** | P1 | ❌ JVM/Android only | Morphological analysis — tokenizes Japanese into words with readings, POS, conjugation | Replace the approximate `WordSegmenter` with real MeCab. JNI wrapper on JVM, native `.so` on Android. iOS needs a wrapper. Desktop: `com.github.nicholasgasior.kmp-mecab` or JNI bridge. |
| **Kuromoji** | P1 | ✅ Pure Java | Japanese morphological analyzer (lighter than MeCab) | Drop-in for `WordSegmenter`. Already Java, works on all JVM targets. `com.atilika.kuromoji:kuromoji-ipadic` |
| **KanjiVG** | P0 | ✅ Data only | Stroke order data for 6,000+ kanji | Already used in `kjd` data platform. Continue importing via `KanjiVgImporter`. |
| **JMdict** | P0 | ✅ Data only | Japanese-English dictionary (200k+ entries) | Already bundled in `AppDataDatabase`. Updated quarterly via `kjd`. |
| **JLPT Word Lists** | P0 | ✅ Data only | JLPT N5–N1 vocabulary lists | Already in `kjd` pipeline. Continue enriching with frequency data. |
| **Tatoeba** | P2 | ✅ Data only | Example sentences with translations | Free dataset (CC BY 2.0). Good for sentence mining examples. Import via `kjd`. |
| **WaniKani Radical Data** | P2 | ✅ Data only | Community radical naming system | Useful for mnemonic system. Check license compatibility. |
| **KANJIDIC2** | P0 | ✅ Data only | Kanji metadata (readings, meanings, JLPT, grade) | Already in `kjd` pipeline. |

---

## 2. SRS & Spaced Repetition

| Library | Priority | KMP | What It Does | Integration Notes |
|---------|----------|-----|-------------|-------------------|
| **FSRS-rs** | P0 | ❌ Rust→JNI/WASM | Next-gen SRS algorithm (FSRS-5 successor) | Kaiteyo already implements FSRS-5 in pure Kotlin. Consider Rust rewrite for performance if card pools exceed 100k. WASM target for iOS. |
| **Anki .apkg parser** | P0 | ✅ Pure Kotlin | Read/write Anki deck packages | Already implemented (`AnkiPackage.kt`). Continue hardening edge cases. |
| **AnkiConnect bridge** | P1 | ❌ Desktop only | Live sync with desktop Anki | Desktop-only: HTTP client to `localhost:8765`. Already architected in `AnkiConnectBridge`. |

---

## 3. UI & Rendering

| Library | Priority | KMP | What It Does | Integration Notes |
|---------|----------|-----|-------------|-------------------|
| **Compose Multiplatform 1.9+** | P0 | ✅ | Next Compose version with improved performance | Already on 1.8.2. Upgrade when stable. |
| **Voyager** | P1 | ✅ | Type-safe navigation (already used in some screens) | Consider deeper integration for the desktop suite's multi-panel navigation. |
| **Lottie Compose** | P1 | ✅ | Lottie animation playback | For onboarding animations, celebration effects, loading states. `com.airbnb.android:lottie-compose` |
| **Rive Compose** | P2 | ✅ | Real-time interactive animations | Alternative to Lottie for more complex mascot/character animations. |
| **Landscapist** | P2 | ✅ | Image loading (Coil/Glide abstraction) | For media thumbnails, user avatars. `com.github.skydoves:landscapist-glide` |
| **KMP Cookies** | P2 | ✅ | Cookie management for web views | For the learning browser component. |

---

## 4. Data & Storage

| Library | Priority | KMP | What It Does | Integration Notes |
|---------|----------|-----|-------------|-------------------|
| **SQLDelight 2.x** | P0 | ✅ | Already used — type-safe SQL | Continue on current version. Upgrade to 2.x when ready. |
| **DataStore** | P0 | ✅ | Key-value + protobuf preferences | Already used for theme settings. Extend for new preference types. |
| **Room (KMP)** | P2 | ❌→✅ | Alternative to SQLDelight | Room KMP is in alpha. Evaluate if SQLDelight migration pain exceeds. |
| **Kotlin Serialization** | P0 | ✅ | JSON/protobuf serialization | Already used throughout. Continue. |
| **kotlinx.datetime** | P0 | ✅ | Date/time handling | Already used. Continue. |

---

## 5. Networking & Sync

| Library | Priority | KMP | What It Does | Integration Notes |
|---------|----------|-----|-------------|-------------------|
| **Ktor Client** | P1 | ✅ | HTTP client for sync, API calls | For account sync, plugin marketplace, update feeds. `io.ktor:ktor-client-core` |
| **Ktor WebSocket** | P1 | ✅ | Real-time sync updates | For live sync status, push notifications. |
| **Firebase (Android)** | P0 | ❌ Android only | Analytics, crash reporting, FCM | Already in Google Play flavor. Keep isolated behind `FlavorModule`. |
| **GitHub API** | P1 | ✅ via Ktor | Device-flow auth, gist sync | Already partially implemented. Complete with Ktor. |

---

## 6. Media & OCR

| Library | Priority | KMP | What It Does | Integration Notes |
|---------|----------|-----|-------------|-------------------|
| **JavaCV / FFmpeg** | P0 | ❌ JVM only | Video/audio processing, subtitle extraction | Already in `mediaGenerator`. Extend for desktop media player. |
| **Tesseract OCR** | P1 | ❌ JVM/Android | Optical character recognition | Desktop: JNI to Tesseract. Android: Tesseract4Android. iOS: need native wrapper. |
| **ML Kit OCR** | P1 | ❌ Android/iOS | Google's on-device OCR | Alternative to Tesseract on mobile. Better CJK support. |
| **VLC4Android** | P1 | ❌ Android | Video playback backend | For media center on Android. |
| **mpv** | P1 | ❌ Desktop | Video playback backend | Already used on desktop via JNI. Continue. |
| **Java Sound API** | P0 | ❌ JVM | Audio playback | Already used. Continue. |
| **Coil** | P1 | ✅ | Image loading + caching | For media thumbnails, user content. Already partially used. |

---

## 7. Desktop & Platform

| Library | Priority | KMP | What It Does | Integration Notes |
|---------|----------|-----|-------------|-------------------|
| **JNA** | P0 | ❌ JVM | Native platform access (Windows API, Linux evdev) | Already used for window chrome, gamepad. Continue. |
| **SystemTray** | P2 | ❌ JVM | System tray icon + menu | For minimize-to-tray, quick actions. |
| **jInput** | P2 | ❌ JVM | Gamepad input (alternative to JNA XInput) | Cross-platform gamepad support. |
| **AwtUtils** | P0 | ❌ JVM | Screen detection, DPI, work areas | Already implemented. Continue. |

---

## 8. Testing & Quality

| Library | Priority | KMP | What It Does | Integration Notes |
|---------|----------|-----|-------------|-------------------|
| **kotlin.test** | P0 | ✅ | Already used for common tests | Continue. |
| **JUnit 5** | P0 | ❌ JVM | Desktop test runner | Already configured. Continue. |
| **Turbine** | P1 | ✅ | Flow testing | `app.cash.turbine:turbine` — test StateFlow emissions. |
| **MockK** | P1 | ✅ | Mocking framework | For ViewModel and repository tests. |
| **Robolectric** | P1 | ❌ Android | Android unit tests without device | For Android-specific code paths. |
| **Shot** | P2 | ❌ Android | Screenshot testing | Visual regression testing for UI. |

---

## 9. Analytics & Monitoring

| Library | Priority | KMP | What It Does | Integration Notes |
|---------|----------|-----|-------------|-------------------|
| **Firebase Analytics** | P0 | ❌ Android | User analytics | Already in Google Play flavor. |
| **Sentry** | P1 | ✅ | Error tracking, performance monitoring | Cross-platform alternative to Firebase Crashlytics. |
| **Privacy-friendly analytics** | P2 | ✅ | F-Droid compatible analytics | Plausible / Umami self-hosted. No tracking. |

---

## 10. Build & Tooling

| Library | Priority | KMP | What It Does | Integration Notes |
|---------|----------|-----|-------------|-------------------|
| **Gradle Version Catalog** | P0 | — | Dependency management | Already using `libs.versions.toml`. Continue. |
| **Detekt** | P1 | ✅ | Static analysis for Kotlin | Code quality enforcement. |
| **Ktlint** | P1 | ✅ | Code formatting | Already mentioned in CODING_STANDARDS. Configure plugin. |
| **Binary Compatibility Validator** | P2 | ✅ | API compatibility checking | For library modules if Kaiteyo publishes APIs. |

---

## 11. Future Integrations (Yomitan Ecosystem)

| Library | Priority | KMP | What It Does | Integration Notes |
|---------|----------|-----|-------------|-------------------|
| **Yomitan dictionary format** | P0 | ✅ Pure Kotlin | Import Yomitan-compatible dictionaries | Already implemented (`DictionaryImporter.kt`). Extend with full format support. |
| **AnkiConnect API** | P1 | ✅ via Ktor | Live Anki integration | Bridge to desktop Anki for card creation sync. |
| **Yomichan handlebars** | P2 | ✅ Pure Kotlin | Template rendering for dictionary entries | For rendering rich dictionary cards with custom templates. |
| **EPUB parser** | P1 | ✅ Pure Kotlin | Read EPUB ebooks for reading mode | `nl.siegmann.epublib:epublib-core` or KMP alternative. |
| **PDF renderer** | P2 | ❌ JVM/Android | Render PDF documents for reading | Apache PDFBox on JVM, Android PDF renderer on mobile. |

---

## 12. Recommended Adoption Order

### Phase 1: Core Improvements (Next Release)
1. **Kuromoji** — real morphological analysis (replaces approximate WordSegmenter)
2. **Turbine** — Flow testing for ViewModels
3. **Ktor Client** — networking foundation for sync
4. **EPUB parser** — reading mode content

### Phase 2: Desktop Enhancement (Q2)
1. **Tesseract OCR** — desktop OCR engine
2. **Lottie Compose** — onboarding animations
3. **Sentry** — error tracking
4. **Detekt** — static analysis

### Phase 3: Mobile & Cross-Platform (Q3)
1. **ML Kit OCR** — mobile OCR
2. **VLC4Android** — media playback
3. **Coil** — image loading
4. **MockK** — comprehensive testing

### Phase 4: Advanced Features (Q4+)
1. **MeCab** — advanced NLP (if Kuromoji proves insufficient)
2. **Rive** — complex character animations
3. **FSRS-rs** — Rust-based SRS for large card pools
4. **Yomichan handlebars** — rich dictionary rendering
