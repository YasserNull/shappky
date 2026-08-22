# How Shappky Works

Shappky stands out for its high-speed app termination capabilities and precise identification of active background processes, unlike other task killer applications.

Other apps typically rely on the `Usage Status` permission to read usage data and search for the `is stopped` value. If they do not find it, they assume the app is running in the background and mark it for termination. This approach is flawed—many of these apps are not actually running in the background at all, leading to unnecessary process termination.

Shappky instead utilizes **Shizuku / Root** privileges to inspect active process information directly via the `ps` command.

---

## 1. Process Identification Evolution

### Legacy Approach (Pre-v2.0.0)
Initially, active applications were identified by querying processes with the following command:

```bash
ps -A -o rss,name | grep '\.' | grep -v '[-@]'
```

* `-A`: Shows all running system and user processes.
* `-o rss,name`: Formats output to display Resident Set Size (RAM usage) and process name.
* `grep '\.'`: Filters lines containing a dot to target package names (e.g., `com.dev.example`).
* `grep -v '[-@]'`: Excludes system service lines containing `@` (e.g., `example.service@...`).

> **Limitation:** This method skipped valid process lines that lacked a dot, even if they belonged to an app (for example, child shell processes like `0 sh`).

### Modern Command Architecture (v2.0.0+)
In version 2.0.0, the command was refactored to address compatibility and accuracy issues:

```bash
toybox ps -A -o %cpu,pid,user,rss,name,uid
```

* **Bundled `toybox` binary:** Integrated inside the APK to guarantee identical behavior across different Android devices and ROMs.
* **Extended Parameters:**
  * `user`: Determines the process username.
  * `%cpu`: Measures CPU utilization.
  * `pid`: Extracts the Process ID.
  * `uid`: Extracts the User ID.

---

## 2. Results Parsing Algorithm

Shappky processes the raw output of the `ps` command through a structured execution pipeline:

1. Read process names and verify whether the corresponding package is installed on the system.
2. If the app is not installed, skip the process.
3. If installed, extract the process's running OS username (e.g., `u0_a123`).
4. Associate the extracted username with the installed application package.
5. Scan for other processes sharing the same username.
6. Mark all matching processes as belonging to that application, while ignoring system users and usernames that do not start with `u0_`.

### Technical Trade-Offs & Edge Cases
Matching by process `name` can occasionally misidentify third-party processes spawned by other apps. However, relying purely on `user` (UID) causes issues with system apps running under shared system accounts (e.g., `system`). Thus, Shappky prioritizes primary package name mapping to guarantee system stability.

---

## 3. Application Classification & Protection

Shappky classifies applications into:
* **User Apps:** Third-party user-installed applications.
* **System Apps:** Pre-installed OS and vendor packages.
* **Persistent Apps:** Identified via Android OS system flags (relaunch automatically if killed).

### Foreground Status Detection
Foreground status is verified dynamically rather than treated as a static category:

```bash
dumpsys activity services com.example | grep isForeground=true
```

### Default Protected Applications
To ensure system integrity and uninterrupted user experience, Shappky automatically protects:

1. **Shappky Self-Protection:** Prevents self-termination.
2. **Current Launcher:** Prevents home screen redraws and reloads.
3. **Current Background Service:** Ensures core app functionality remains intact.
4. **Current Input Method (IME):** Prevents keyboard crashes.
5. **Active Widgets:** Scans currently active widget IDs via:
   ```bash
   dumpsys appwidget
   ```
   *(under the `AppWidgetIds` section)*
6. **Android System Services:** Prevents OS instability.
7. **Google Play Services:** Prevents crashes on Google-supported systems.
8. **Persistent Applications:** Skipped since killing them is redundant (they immediately restart).

---

## 4. Multi-Stage Termination Mechanics

Shappky executes a progressive 4-stage kill process when closing background applications:

### Stage 1: Soft Kill
```bash
am kill com.example
```
Graceful background termination. If an application has active home widgets or is open in the foreground, a soft kill stops background tasks without breaking widget state or interrupting active user interaction.

### Stage 2: Force Stop (If Active PID Persists)
Shappky verifies if the process is still running:
```bash
pidof com.example
```
If a PID is returned following a soft kill, Shappky issues a force-stop command:
```bash
am force-stop com.example
```
This forcibly terminates the application regardless of its active state.

### Stage 3: Forced Signal Process Kill
If orphaned background processes persist after a force-stop, Shappky sends a direct termination signal:
```bash
pkill -9 pid
```

### Stage 4: Global Flush ("Kill All")
When the user initiates a bulk cleanup action ("Select All -> Kill"), Shappky terminates the specified targeted apps and then issues a safe global background kill signal across the operating system:
```bash
am kill-all
```
