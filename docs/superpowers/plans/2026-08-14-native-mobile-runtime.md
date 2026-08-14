# Native Mobile Runtime Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an upstreamable native mobile foundation in which Android owns a dedicated Termux/OmniRoute runtime lifecycle while iOS provides a native control client for an OmniRoute gateway.

**Architecture:** The native apps are control planes; OmniRoute remains authoritative for routing and provider logic. Android manages a dedicated Termux-hosted Node.js runtime and exposes only a loopback, authenticated OmniRoute API to the native UI. iOS initially manages remote/local-network OmniRoute gateways and does not attempt unsupported persistent Node.js execution.

**Tech Stack:** Android Kotlin + Jetpack Compose; Android Keystore; Termux runtime integration; Swift + SwiftUI; iOS Keychain; native HTTP/SSE clients; TypeScript/Node.js OmniRoute APIs; existing project test/lint/build tooling.

## Global Constraints

- Preserve the repository's existing Node.js version requirements: `>=22.22.3 <23` or `>=24.0.0 <27`, with Node 24 LTS recommended.
- Follow the repository's feature-branch and Conventional Commit rules.
- Production changes must include automated tests and preserve the 60% coverage gate.
- Do not expose raw secrets, stacks, or unsanitized provider errors.
- Do not claim Termux alone is a VM/container-grade sandbox; Android isolation is based on app sandbox + dedicated runtime policy.
- Bind the managed local OmniRoute gateway to loopback by default.
- Keep OmniRoute routing/provider logic in the server; mobile code must not fork the routing engine.
- iOS must use supported platform lifecycle/background APIs and must not depend on a persistent background Node.js process.

---

### Task 1: Establish mobile architecture and API contract

**Files:**
- Create: `mobile/README.md`
- Create: `mobile/shared/README.md`
- Create: `docs/architecture/mobile-runtime.md`
- Create: `docs/security/mobile-runtime.md`

**Interfaces:**
- Produces the documented gateway contract consumed by Android and iOS clients.
- Defines `GatewayConnection`, `GatewayCapabilities`, health, provider/model discovery, authentication, streaming, and lifecycle semantics.

- [ ] Document local Android gateway versus remote gateway modes.
- [ ] Document loopback binding and authentication requirements.
- [ ] Document SSE/streaming semantics and error sanitization expectations.
- [ ] Document iOS remote-control constraints.
- [ ] Document the security boundary and explicitly distinguish Android app sandboxing from VM/container isolation.
- [ ] Validate all paths and API names against the existing repository before implementation.

### Task 2: Add shared API/client contract tests

**Files:**
- Create: `mobile/shared/api-contract.md`
- Create: `tests/mobile/api-contract.test.ts`

**Interfaces:**
- Defines request/response fixtures for gateway health, capabilities, providers, models, routing configuration, usage, and authentication failures.

- [ ] Add fixtures for successful and malformed gateway responses.
- [ ] Add tests for stable error envelopes and authentication rejection.
- [ ] Add streaming/SSE event fixtures covering completion, provider error, and disconnect.
- [ ] Run the focused test and confirm failure before implementation.
- [ ] Implement only the minimal contract validation required by the fixtures.
- [ ] Run the focused test and confirm pass.

### Task 3: Android native application foundation

**Files:**
- Create: `mobile/android/` Gradle project and source tree following current Android tooling.
- Create: `mobile/android/app/src/main/java/.../OmniRouteApplication.kt`
- Create: `mobile/android/app/src/main/java/.../MainActivity.kt`
- Create: Compose navigation and gateway UI modules.

**Interfaces:**
- `GatewayRepository` provides gateway state to UI.
- `GatewayConnection` represents endpoint/auth configuration.

- [ ] Establish a minimal Compose application with Gateway, Providers, Routing, Health, and Settings destinations.
- [ ] Add lifecycle-safe state handling with coroutines.
- [ ] Add unit tests for navigation/state reducers.
- [ ] Build the APK/debug variant on a supported Android toolchain.

### Task 4: Secure Android runtime manager

**Files:**
- Create: `mobile/android/.../runtime/TermuxRuntimeManager.kt`
- Create: `mobile/android/.../runtime/OmniRouteProcessManager.kt`
- Create: `mobile/android/.../runtime/RuntimeInstaller.kt`
- Create: `mobile/android/.../runtime/RuntimeIntegrity.kt`
- Create: `mobile/android/.../runtime/RuntimePolicy.kt`
- Create: `mobile/android/.../security/SecretStore.kt`
- Create: `mobile/android/.../security/LocalApiAuthenticator.kt`
- Create: `mobile/android/.../runtime/*Test.kt`

**Interfaces:**
- `TermuxRuntimeManager.install(): Result<RuntimeInfo>`
- `TermuxRuntimeManager.start(): Result<RuntimeHandle>`
- `TermuxRuntimeManager.stop(): Result<Unit>`
- `TermuxRuntimeManager.status(): RuntimeStatus`
- `OmniRouteProcessManager.start(config: GatewayConfig): Result<ProcessHandle>`
- `OmniRouteProcessManager.stop(): Result<Unit>`
- `RuntimePolicy` defines allowed filesystem, environment, command, port, and network behavior.

- [ ] Define an explicit runtime directory owned by the Android/Termux integration rather than reusing arbitrary user paths.
- [ ] Generate/store gateway credentials using Android Keystore-backed storage.
- [ ] Verify the runtime/package integrity before execution.
- [ ] Start OmniRoute with an explicit environment, loopback binding, controlled port, and dedicated data directory.
- [ ] Reject unsafe command construction; pass runtime values as structured arguments/environment values.
- [ ] Capture stdout/stderr into bounded logs without leaking secrets.
- [ ] Detect process exit and expose deterministic status.
- [ ] Add tests for path validation, argument construction, secret redaction, lifecycle transitions, and crash recovery.

### Task 5: Android OmniRoute gateway client

**Files:**
- Create: `mobile/android/.../gateway/OmniRouteClient.kt`
- Create: `mobile/android/.../gateway/SseClient.kt`
- Create: `mobile/android/.../gateway/GatewayRepository.kt`
- Create: corresponding unit tests.

**Interfaces:**
- `OmniRouteClient.health()`
- `OmniRouteClient.capabilities()`
- `OmniRouteClient.providers()`
- `OmniRouteClient.models()`
- `OmniRouteClient.usage()`
- `OmniRouteClient.stream(...)`

- [ ] Connect only to approved gateway endpoints.
- [ ] Implement request timeouts and bounded retries for idempotent control operations.
- [ ] Implement SSE parsing with cancellation and reconnect semantics.
- [ ] Sanitize displayed errors.
- [ ] Add tests for HTTP failure, timeout, malformed JSON, SSE disconnect, and authentication rejection.

### Task 6: Android runtime UX and lifecycle

**Files:**
- Modify: Android Compose runtime/dashboard screens.
- Create: runtime status components and lifecycle tests.

- [ ] Add Install, Start, Stop, Restart, Health, and Logs controls.
- [ ] Show explicit states: unavailable, installing, starting, running, degraded, stopped, failed.
- [ ] Prevent duplicate starts/stops through a serialized lifecycle state machine.
- [ ] Ensure app process death cannot leave the UI claiming the gateway is running without verification.
- [ ] Add Android lifecycle/background handling only where supported by Android policy.
- [ ] Test state transitions and recovery.

### Task 7: iOS native control client foundation

**Files:**
- Create: `mobile/ios/` Xcode project and SwiftUI source tree.
- Create: gateway client, Keychain store, SSE client, and tests.

**Interfaces:**
- Swift equivalents of the gateway contract from Task 1.
- `GatewayClient` for remote/local-network OmniRoute instances.

- [ ] Add SwiftUI navigation for gateway, providers, routing, health, usage, and settings.
- [ ] Store credentials in Keychain.
- [ ] Implement URLSession-based HTTP/SSE streaming.
- [ ] Respect iOS foreground/background execution constraints.
- [ ] Do not implement a persistent local Node.js/Termux runtime on iOS.
- [ ] Add unit tests for decoding, authentication, retries, SSE cancellation, and errors.

### Task 8: Documentation, CI, and contribution readiness

**Files:**
- Modify: `README.md` with a concise mobile section.
- Create: `docs/mobile/android.md`
- Create: `docs/mobile/ios.md`
- Create/modify: CI workflow for mobile checks where supported.
- Create: changelog fragment under `changelog.d/features/` for the user-facing mobile capability.

- [ ] Document Android prerequisites and Termux/runtime limitations.
- [ ] Document iOS gateway configuration.
- [ ] Document security model and threat boundaries.
- [ ] Add reproducible Android build/test commands.
- [ ] Add iOS build/test commands appropriate to macOS/Xcode CI availability.
- [ ] Ensure no secrets or device-specific paths enter the repository.

### Task 9: Verification gate

**Files:**
- No new production files; verification only.

- [ ] Run the repository's unit test suite required for touched server code.
- [ ] Run `npm run test:coverage` and verify the 60% gate remains satisfied.
- [ ] Run `npm run lint` and `npm run check`.
- [ ] Run `npm run build`.
- [ ] Build and test Android modules on an Android-capable environment.
- [ ] Build/test iOS modules on macOS/Xcode when available; otherwise record the exact environment limitation without claiming success.
- [ ] Perform a security review of command execution, paths, credentials, local binding, logs, and runtime lifecycle.
- [ ] Review the complete diff for unrelated changes.
- [ ] Prepare a PR describing the Android runtime boundary, iOS control-client scope, tests, and known platform limitations.

## Commit Strategy

Use focused commits such as:

```text
feat(mobile): add native gateway contract
feat(android): add OmniRoute runtime manager
feat(android): add local gateway client
feat(android): add runtime dashboard
feat(ios): add native gateway client
security(mobile): enforce runtime credential isolation
docs(mobile): document native runtime architecture
```

Never commit secrets, generated build artifacts, local Android/Termux paths, or provider credentials.
