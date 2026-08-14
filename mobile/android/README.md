# OmniRoute Native Android

Native Android control plane for OmniRoute with a managed Termux-hosted runtime.

## Architecture

```text
Android app
  ├── Jetpack Compose UI
  ├── Runtime policy
  ├── Keystore-backed secrets
  ├── OmniRoute API client
  └── Termux command bridge
             │
             ▼
      Dedicated Termux runtime
             │
             ▼
         OmniRoute
             │
             ▼
       127.0.0.1:20128
```

## Security boundary

The managed runtime is isolated by policy and dedicated paths, but Termux is **not** a VM/container-grade sandbox. The app must not describe it as one. Stronger hostile-code isolation requires a separate virtualization boundary.

The gateway defaults to loopback. Runtime commands are constrained to the Termux bash entrypoint, shell paths are quoted, and the process is tracked through a runtime-owned PID file.

## Requirements

- Android Studio with Android SDK 35
- JDK 17+
- Termux installed from a trusted source
- Termux support for the `com.termux.RUN_COMMAND` integration
- Network access for the initial OmniRoute package installation

## Build

From `mobile/android`:

```bash
gradle assembleDebug
gradle test
```

A Gradle wrapper will be added when the project is generated with the repository's approved Gradle distribution. Do not commit generated SDK/build artifacts.

## Runtime lifecycle

The first implementation exposes:

- bootstrap/install
- start
- stop
- dedicated runtime home/workspace
- PID tracking
- loopback binding
- runtime path validation

Subsequent milestones add health monitoring, automatic recovery, SSE, provider/model management, and production UI flows.
