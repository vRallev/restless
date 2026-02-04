# ScreenOn

A macOS menu bar app that keeps your screen awake. Built with Kotlin/Native.

**GitHub:** https://github.com/vRallev/screen-on

## What It Does

ScreenOn prevents your Mac from sleeping by running the `caffeinate` command in the background. It displays a countdown timer in the menu bar showing how much
time remains.

The app has two modes:

### Automatic Mode

The app automatically keeps your screen on when **all** of these conditions are met:

- You are **idle** (no mouse/keyboard input for 10+ seconds)
- Your Mac is **on charger** (AC power)
- Your screen is **not locked**

When triggered, it starts a **2-hour timer**. If any condition changes (you move the mouse, unplug the charger, or lock the screen), the timer stops and resets.

### Manual Mode

You can manually control the timer by clicking the menu bar item:

- **Left-click**: Adds 1 hour to the timer (max 24 hours) and switches to manual mode
- **Right-click**: Opens the menu

In manual mode:

- The timer shows a **▶** prefix (e.g., `▶ 02:30`)
- The timer keeps running even if you move the mouse, unplug, or lock the screen
- Click **Cancel** in the menu to stop the timer and return to automatic mode

### Menu Items

Right-click the menu bar item to access:

- **Cancel** - Stop the timer and return to automatic mode (only visible in manual mode)
- **About ScreenOn** - Opens the GitHub repository
- **Exit** - Quit the application

### Visual Indicators

- `00:00` - Timer not running (automatic mode, waiting for idle)
- `02:00` - Timer running, 2 hours remaining (colon blinks every second)
- `▶ 01:30` - Manual mode, 1 hour 30 minutes remaining

## Requirements

- macOS 11.0 or later (Apple Silicon)
- JDK 17+ (for building)

## Gradle Tasks

### Build

```bash
./gradlew build
```

Compiles the project and runs all tests.

### Run Tests

```bash
./gradlew nativeTest
```

Runs the unit tests.

### Run the App (Development)

```bash
./gradlew runDebugExecutableNative
```

Builds and runs the app directly. Useful during development.

### Package as .app

```bash
./gradlew packageApp
```

Creates a signed macOS application bundle at `build/ScreenOn.app`.

### Format Code

```bash
./gradlew ktfmtFormat
```

Formats all Kotlin code using ktfmt (Google style).

## Installation

1. Build the app:
   ```bash
   ./gradlew packageApp
   ```

2. Copy to Applications:
   ```bash
   cp -r build/ScreenOn.app /Applications/
   ```

3. Launch the app:
    - Double-click `/Applications/ScreenOn.app`
    - If macOS blocks it: Right-click → Open → Open

## Auto-Launch on Login

To start ScreenOn automatically when you log in:

1. Open **System Settings**
2. Go to **General** → **Login Items**
3. Click **+** and select `ScreenOn.app`

## How It Works

The app uses macOS's built-in `caffeinate` command with these flags:

- `-d` prevent display from sleeping
- `-i` prevent system from idle sleeping
- `-m` prevent disk from idle sleeping
- `-s` prevent system from sleeping (AC power only)
- `-u` declare user active (turns display on)

Safety features:

- `-w <pid>` exits caffeinate if the app crashes
- `-t <seconds>` backup timeout
