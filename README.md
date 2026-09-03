# 🎓 Student360

> **Your All-in-One Academic Powerhouse — Modern, Intuitive, and 100% Offline-First.**

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84.svg?style=flat&logo=android)](https://www.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4.svg?style=flat&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Material 3](https://img.shields.io/badge/Design-Material%203-7B1FA2.svg?style=flat)](https://m3.material.io)
[![Room Database](https://img.shields.io/badge/Storage-Room%20DB-FFA000.svg?style=flat)](https://developer.android.com/training/data-storage/room)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

---

## 🌟 Overview

**Student360** is a comprehensive, offline-first Android application crafted with **Kotlin** and **Jetpack Compose**. Designed for students navigating demanding academic lives, Student360 seamlessly unifies attendance management, smart timetable scheduling, study sessions with Pomodoro tracking, exam readiness analytics, and AI-driven study assistance into a cohesive Material 3 experience.

---

## ✨ Key Features

### 📊 1. Intelligent Attendance Management
- **Target Tracking**: Set customizable attendance target thresholds (e.g., 75%, 80%, 85%).
- **Bunk & Recovery Calculator**: Instantly know how many classes you can afford to miss or how many consecutive classes you must attend to restore your target percentage.
- **Detailed History**: Log past attendance records by date and status (Present, Absent, Cancelled).
- **Proactive Alerts**: Receive notifications if your attendance in any subject drops below the safety margin.

### 📅 2. Dynamic Timetable & Scheduling
- **Weekly & Daily Timetables**: Visual timetable breakdown with customizable subject colors, room locations, and timings.
- **Auto-Day Detection**: Automatically displays the current day's lecture sequence.
- **Smart Lecture Reminders**: Pre-class notifications with customizable warning windows.

### 🎯 3. "My Day" & Task Planner
- **Unified Daily Agenda**: Consolidate today's lectures, pending assignments, and priority tasks in one unified view.
- **Task & Assignment Tracking**: Organize deliverables by due date, urgency tags, and completion states.

### ⏱️ 4. Focus Study Timer & Pomodoro
- **Deep Focus Modes**: Built-in Pomodoro, 50/10/50, and customized interval timers.
- **Subject-Linked Sessions**: Associate study intervals with specific subjects to record precise study metrics.
- **Streak & Consistency Counter**: Track study habits over time to maintain consistency.

### 📝 5. Exam Readiness & Syllabus Tracker
- **Weightage & Topic Checklists**: Break down syllabi by topics and track readiness levels.
- **Countdown Timers**: Live day count to upcoming midterms, practicals, and finals.
- **Exam Engine**: Synthesizes preparation progress with subject confidence metrics.

### 🤖 6. AI Study Assistant & Planner
- **Study Recommendations**: Contextual study tips based on your upcoming exams and current syllabus completion.
- **Adaptive Scheduling**: Suggests subjects requiring immediate revision.

### 🔒 7. Complete Offline Privacy & Backup
- **No Cloud Required**: All data stays private on your device with local SQLite / Room DB.
- **Backup & Restore**: Easily export and import your database in JSON/Room formats to switch devices or preserve data.

---

## 🏗️ Architecture & Tech Stack

Student360 adheres to modern Android architecture principles (**Clean Architecture + MVVM + UDF**):

- **UI Layer**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with [Material 3](https://m3.material.io) theming, animations, unified date/time pickers, and dynamic dark/light theme support.
- **State Management**: AndroidX `ViewModel`, Kotlin `StateFlow`, and `Coroutines`.
- **Data Layer**: [Room Persistence Library](https://developer.android.com/training/data-storage/room) with TypeConverters, Reactive Flow queries, and SQLite caching.
- **Background Tasks & Alerts**: Android `AlarmManager` and `NotificationManager` with notification channels.
- **JSON Serialization**: `Gson` / `Kotlinx Serialization` for seamless backups and data exchange.

```
app/src/main/java/com/student360/app/
├── data/
│   ├── local/
│   │   ├── dao/           # Room DAOs (Subject, Timetable, Exam, Study, Task, Alert, etc.)
│   │   ├── entity/        # Room Database Entities
│   │   └── Converters.kt  # Type Converters
│   └── repository/        # Centralized Repository Layer
├── service/               # Notification Schedulers, ExamEngine, BackupRestoreManager
├── ui/
│   ├── screens/           # Compose Screens & ViewModels (Home, Attendance, Schedule, etc.)
│   └── theme/             # Material 3 Color Schemes, Typography, Shapes
├── MainActivity.kt        # Single Activity Container
└── Student360App.kt       # Application Entry & Notification Channel Setup
```

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio**: Ladybug / Koala / Hedgehog or newer (JDK 17+)
- **Android SDK**: Min SDK 26, Target SDK 34/35

### Installation & Build

1. **Clone the Repository:**
   ```bash
   git clone https://github.com/pawshevedarth-commits/Student-360.git
   cd Student-360
   ```

2. **Open in Android Studio:**
   - Launch Android Studio and choose **Open Project**.
   - Select the `Student-360` root directory and let Gradle sync.

3. **Build via Command Line:**
   ```bash
   # On Windows (PowerShell)
   .\gradlew.bat assembleDebug

   # On macOS/Linux
   ./gradlew assembleDebug
   ```

4. **Install onto Device/Emulator:**
   ```bash
   .\gradlew.bat installDebug
   ```

---

## 📱 Screenshots

> *Coming soon — capture and share screenshots of the dashboard, attendance tracker, and study timers!*

---

## 🤝 Contributing

Contributions, issues, and feature requests are welcome!
Feel free to check out the [issues page](https://github.com/pawshevedarth-commits/Student-360/issues).

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
