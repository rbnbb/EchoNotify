# Android Development Tools - Uninstall Guide

## Installed via Homebrew

### Core Tools
```bash
# Remove Android Studio
brew uninstall --cask android-studio

# Remove Java JDK (if not used elsewhere)
brew uninstall openjdk@17

# Remove Android Command Line Tools (if installed separately)
brew uninstall android-commandlinetools
```

### Additional Developer Tools (if installed)
```bash
# Remove ADB/Fastboot tools
brew uninstall android-platform-tools

# Remove Gradle (if not used by other projects)
brew uninstall gradle
```

## Manual Cleanup

### Android SDK and Data
```bash
# Remove Android SDK directory
rm -rf ~/Library/Android

# Remove Android Studio preferences and caches
rm -rf ~/Library/Preferences/com.google.android.studio*
rm -rf ~/Library/Caches/Google/AndroidStudio*
rm -rf ~/Library/Logs/Google/AndroidStudio*
rm -rf ~/Library/Application\ Support/Google/AndroidStudio*

# Remove Gradle cache (if not needed for other projects)
rm -rf ~/.gradle
```

### Environment Variables
Remove these lines from your shell profile (`~/.zshrc`, `~/.bash_profile`, etc.):
```bash
export ANDROID_HOME=$HOME/Library/Android/sdk
export PATH=$PATH:$ANDROID_HOME/emulator
export PATH=$PATH:$ANDROID_HOME/tools
export PATH=$PATH:$ANDROID_HOME/tools/bin
export PATH=$PATH:$ANDROID_HOME/platform-tools
export JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/[version]/libexec/openjdk.jdk/Contents/Home
```

## Verification
```bash
# Check if tools are removed
which android-studio  # should return nothing
which adb            # should return nothing (unless using system version)
which gradle         # should return nothing (unless using system version)
```

---
*Created: $(date)*  
*Note: Only remove Java/Gradle if not used by other projects*