# OmniRoute on Termux (Android)

This guide covers a native Termux installation on Android. Termux is not a full Linux distribution: paths such as `/usr/bin/env` may not exist, and native Node dependencies can distinguish Android from glibc Linux. Keep these differences in mind when troubleshooting.

## 1. Prepare Termux

Use a current Termux build and update packages:

```sh
pkg update && pkg upgrade
pkg install nodejs-lts git python make clang
```

Check the runtime before installing OmniRoute:

```sh
node --version
npm --version
node -p 'JSON.stringify({platform: process.platform, arch: process.arch})'
```

Use a Node.js version supported by the installed OmniRoute release. Check the `engines` field in the package metadata rather than assuming every Node release is supported.

## 2. Install OmniRoute

Install the published package globally:

```sh
npm install --global omniroute
npm list --global --depth=0
```

If npm reports a native dependency or optional-dependency failure, retain the full npm error output. Do not copy a `node_modules` directory from desktop Linux/macOS: native packages such as esbuild must match the target runtime and architecture.

## 3. Handle a Termux shebang mismatch

Some npm executables use `#!/usr/bin/env node`. A standard Termux environment normally places `env` under `$PREFIX/bin`, not `/usr/bin/env`. If invoking `omniroute` fails with `bad interpreter: No such file or directory`, verify the installed shim and Node path:

```sh
command -v omniroute
head -n 1 "$(command -v omniroute)"
command -v env
command -v node
npm root --global
```

As a diagnostic workaround, invoke the package entry point with Node directly (replace the path if your global npm prefix differs):

```sh
node "$PREFIX/lib/node_modules/omniroute/bin/omniroute.mjs" --help
```

This bypasses the executable's shebang, but it does **not** resolve missing native dependencies or runtime incompatibilities. If the package was installed under a different prefix, locate it with `npm root --global` and use that path.

## 4. Verify before configuring providers

Run the CLI help first:

```sh
omniroute --help
```

If the shim fails, use the direct Node invocation above. Follow the current CLI help for server startup and provider configuration; option names can change between releases. Configure provider credentials through the supported setup flow and do not paste API keys into issue reports or shell history.

## 5. Collect useful diagnostics

When reporting a Termux-specific issue, include:

```sh
termux-info
node --version
npm --version
node -p 'JSON.stringify({platform: process.platform, arch: process.arch})'
npm list --global --depth=0
```

Also include the exact command, complete error message, OmniRoute version, and whether Node is the native Termux runtime or a compatibility/glibc wrapper. Redact tokens, cookies, private URLs, and other secrets.

## Limitations

This guide is a troubleshooting path, not a claim that every OmniRoute feature is supported on Android. Features that rely on native Linux binaries, desktop/Electron integration, transparent proxying, or platform-specific networking may require additional work or may not be available in Termux. Verify each feature independently.
