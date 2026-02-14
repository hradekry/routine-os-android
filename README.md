# Routine OS Android

A native Android version of Routine OS with robust alarm and notification support that works even when the device is asleep.

## Features

- **Calendar View** - Monthly calendar with event management
- **Daily Tasks** - Task tracking with completion, skip, and recurring tasks
- **The Coach** - AI coaching system with motivational messages and progress tracking
- **Native Alarms** - Exact alarms that work in Doze mode and when device is asleep
- **Persistent Notifications** - Foreground service ensures alarms are never missed
- **Boot Receiver** - Automatically restores alarms after device restart
- **Data Persistence** - SharedPreferences with JSON serialization
- **Export/Import** - Backup and restore functionality

## Key Android Features

### Alarm System
- Uses `AlarmManager.setExactAndAllowWhileIdle()` for reliable alarms
- Foreground service with notification channel for high priority
- Handles Android 12+ exact alarm permissions
- Supports Android 13+ notification permissions
- Boot receiver restores alarms after device restart

### Architecture
- MVVM with AndroidViewModel
- Repository pattern for data access
- Coroutines for async operations
- ViewBinding for type-safe view references
- Material Design 3 components

## Requirements

- Android 5.0 (API 26) minimum
- Android 12+ for exact alarm permission
- Android 13+ for notification permission

## Build & Run

```bash
# Open in Android Studio
# Or build via command line
./gradlew assembleDebug
./gradlew installDebug
```

## Permissions

The app requests these permissions:
- `POST_NOTIFICATIONS` (Android 13+) - Show notifications
- `SCHEDULE_EXACT_ALARM` (Android 12+) - Set exact alarms
- `RECEIVE_BOOT_COMPLETED` - Restore alarms after boot
- `FOREGROUND_SERVICE` - Keep alarm service running

## Project Structure

```
app/src/main/
├── java/com/routineos/
│   ├── data/
│   │   ├── models/          # Event, Task, AppSettings
│   │   └── Repository.kt    # Data persistence
│   ├── services/
│   │   ├── AlarmService.kt  # Foreground service
│   │   └── AlarmScheduler.kt # Alarm management
│   ├── receivers/
│   │   ├── AlarmReceiver.kt # Handles alarm broadcasts
│   │   └── BootReceiver.kt  # Restores alarms on boot
│   ├── ui/
│   │   ├── calendar/        # Calendar fragment
│   │   ├── tasks/           # Tasks fragment
│   │   └── coach/           # Coach fragment
│   ├── viewmodels/
│   │   └── MainViewModel.kt # Shared view model
│   ├── MainActivity.kt
│   └── RoutineOSApp.kt
├── res/
│   ├── layout/              # XML layouts
│   ├── drawable/            # Icons and shapes
│   ├── values/              # Strings, colors, themes
│   └── menu/                # Bottom navigation
└── AndroidManifest.xml
```

## Key Components

### AlarmService
- Foreground service that persists across app lifecycle
- Shows persistent notification
- Plays alarm sounds and vibrations
- Handles alarm stop requests

### AlarmScheduler
- Manages exact alarm scheduling
- Handles permission requests
- Schedules recurring alarms
- Reschedules after boot

### Repository
- SharedPreferences-based storage
- JSON serialization with Gson
- Export/import functionality
- Coroutine-based async operations

## Data Models

```kotlin
data class Event(
    val id: String,
    val title: String,
    val description: String,
    val time: String?,
    val type: String, // "onetime" or "recurring"
    val date: String,
    val alarmEnabled: Boolean,
    val alarmTimestamp: Long?,
    val alarmNotified: Boolean
)

data class Task(
    val id: String,
    val title: String,
    val description: String,
    val completed: Boolean,
    val date: String,
    val type: String, // "onetime" or "recurring"
    val skippedDates: List<String>,
    val trackable: Boolean,
    val trackTarget: Float?,
    val trackUnit: String?,
    val trackIncrement: Float?,
    val trackProgress: Map<String, Float>
)
```

## Alarm Flow

1. User creates event/task with alarm
2. `AlarmScheduler.scheduleAlarm()` sets exact alarm
3. `AlarmReceiver` receives broadcast when alarm triggers
4. `AlarmService` starts and shows notification + plays sound
5. User can stop alarm via notification action
6. For recurring events, next alarm is automatically scheduled

## Power Management

- Uses `setExactAndAllowWhileIdle()` to bypass Doze mode
- Foreground service prevents app from being killed
- Boot receiver ensures persistence across restarts
- Minimal battery impact with efficient scheduling

## Future Enhancements

- [ ] Full calendar grid implementation
- [ ] Trackable task progress UI
- [ ] Voice input for Coach commands
- [ ] Widget support
- [ ] Cloud sync
- [ ] More sophisticated Coach AI
- [ ] Dark/Light theme toggle
- [ ] Custom alarm sounds
- [ ] Task categories and filtering

## License

MIT
