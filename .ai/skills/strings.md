# Skill — Add i18n Strings

> Interface-based, not resource files.

Steps (all three):

1. `core/.../presentation/common/resources/string/Strings.kt`
```kotlin
val myFeatureTitle: String
```

2. `core/.../common/resources/string/EnglishStrings.kt`
```kotlin
override val myFeatureTitle = "My Feature"
```

3. `core/.../common/resources/string/JapaneseStrings.kt`
```kotlin
override val myFeatureTitle = "マイ機能"
```

Usage: `resolveString { myFeatureTitle }` (selected by `Locale.current.language`).

Rule: interface enforces it — missing impl = compile error. Add to interface first.

