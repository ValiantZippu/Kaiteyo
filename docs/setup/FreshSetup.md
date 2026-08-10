# Fresh Setup

Use this guide when cloning Kaiteyo onto a new computer. Complete [Required Software](RequiredSoftware.md) first.

```bash
git clone <repository-url>
cd kaiteyo
```

Windows:

```powershell
.\gradlew.bat --version
```

macOS/Linux:

```bash
chmod +x gradlew
./gradlew --version
```

Then follow [First Build](FirstBuild.md). Do not commit `local.properties`, signing files, or machine-specific Gradle settings.

Related: [Troubleshooting](../troubleshooting/README.md), [Git Guide](../guides/GIT_GUIDE.md), [Command Library](../development/COMMANDS.md).
