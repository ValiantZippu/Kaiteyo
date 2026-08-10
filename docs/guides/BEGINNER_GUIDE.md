# Kaiteyo — Beginner's Guide

This guide is for people who have never programmed before. It explains everything from absolute zero.

## What is Git?

Git is a tool that tracks changes to files over time. Think of it like "undo" for your entire project — but better, because multiple people can work on the same project at the same time.

**Key concepts:**
- **Repository (repo)**: A folder that Git is watching
- **Commit**: A saved snapshot of your changes
- **Branch**: A separate line of development (like a parallel universe)
- **Push**: Send your changes to GitHub
- **Pull**: Get the latest changes from GitHub

## What is GitHub?

GitHub is a website that stores Git repositories online. It's like Google Drive for code. Multiple people can collaborate, review each other's work, and track issues.

## What is VS Code?

VS Code is a text editor for writing code. It's free, runs on Windows/macOS/Linux, and has extensions that add features like syntax highlighting, debugging, and AI assistance.

## What is Java?

Java is a programming language. Kaiteyo is written in Kotlin, which runs on the Java Virtual Machine (JVM). This means you need Java installed to run Kotlin code.

## What is Kotlin?

Kotlin is a modern programming language that runs on the JVM. It's concise, safe, and fully compatible with Java. Kaiteyo's entire codebase is written in Kotlin.

## What is Gradle?

Gradle is a build system. It:
1. Downloads libraries (dependencies) that the project needs
2. Compiles Kotlin code into a runnable program
3. Packages the program for distribution (MSI, DMG, APK)
4. Runs tests

When you run `./gradlew :desktopApp:run`, Gradle:
1. Checks if dependencies are downloaded
2. Compiles the code
3. Starts the application

## What is Compose Multiplatform?

Compose Multiplatform is a UI framework that lets you write the same user interface code for Windows, macOS, Linux, Android, and iOS. Instead of writing separate code for each platform, you write it once in Kotlin.

## What is Desktop Compose?

Desktop Compose is the version of Compose that runs on Windows, macOS, and Linux. It creates native desktop applications with hardware-accelerated graphics.

## What is Android Studio?

Android Studio is a specialized version of IntelliJ IDEA for Android development. It includes the Android SDK, emulator, and debugging tools. You need it to build the Android version of Kaiteyo.

## What is IntelliJ IDEA?

IntelliJ IDEA is a powerful IDE (Integrated Development Environment) for Kotlin and Java development. The free Community Edition is sufficient for Kaiteyo development.

## Common Terms

| Term | Meaning |
|------|---------|
| **IDE** | Integrated Development Environment — a program for writing code |
| **SDK** | Software Development Kit — tools for building software |
| **JDK** | Java Development Kit — needed to compile Kotlin |
| **JVM** | Java Virtual Machine — runs compiled Kotlin/Java code |
| **API** | Application Programming Interface — how different software talks to each other |
| **Dependency** | A library that the project uses |
| **Compile** | Convert human-readable code into machine-executable code |
| **Runtime** | When the program is actually running |
| **Debug** | Find and fix problems in code |
| **Terminal** | A text-based interface for running commands |

## First Steps

1. Install Git (see `../development/DEVELOPMENT_SETUP.md`)
2. Install Java JDK 17
3. Install VS Code
4. Install VS Code extensions
5. Clone the repository
6. Run `./gradlew :desktopApp:run`

## Getting Help

- Read the documentation in `docs/`
- Check `docs/planning/CURRENT_ISSUES.md` for known problems
- Open a GitHub Issue for bugs
- Ask in the project's discussion forum
