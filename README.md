# Exam

Open this directory in Android Studio:

```text
/Applications/presentation_andriod/exam
```

Do not open the parent `presentation_andriod` directory.

## SDK Path

`local.properties` is intentionally not included because it contains a local Android SDK path, which is different on macOS and Windows.

Android Studio usually creates `local.properties` automatically during Gradle sync. If it does not, copy `local.properties.example` to `local.properties` and set `sdk.dir` to your local Android SDK path.

Examples:

```properties
# macOS
sdk.dir=/Users/<your-user>/Library/Android/sdk

# Windows
sdk.dir=C\:\\Users\\<your-user>\\AppData\\Local\\Android\\Sdk
```
