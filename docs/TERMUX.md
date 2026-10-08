# OmniRoute on Termux (Android)

This is a practical installation, configuration, verification, and troubleshooting guide for OmniRoute on Android using Termux. Termux is an Android environment, not a conventional glibc Linux distribution; compatibility must be verified for each OmniRoute release and feature. Do not assume that a successful npm install proves the server works.

## Support and safety notes

- Use a current Termux installation from a maintained distribution source.
- Back up important Termux files before major package upgrades.
- Keep several gigabytes of free space available before installing: npm may download a large dependency tree and cache tarballs.
- Never publish provider API keys, cookies, access tokens, or unredacted logs.
- Do not use `sudo` or create a fake `/usr/bin/env` symlink on Android to work around a shebang error.
- Review the current release's README, package metadata, and CLI help; commands and options can change.

## 1. Prepare Termux

Update Termux and install common build/runtime prerequisites:

```sh
pkg update && pkg upgrade
pkg install nodejs-lts git python make clang
```

Check storage and runtime details:

```sh
df -h "$HOME"
node --version
npm --version
node -p 'JSON.stringify({platform: process.platform, arch: process.arch, execPath: process.execPath}, null, 2)'
command -v node
command -v npm
npm config get prefix
npm root --global
```

Use a Node.js version accepted by the installed OmniRoute release. Check the package's `engines` field instead of assuming the newest Node version is supported. If your shell loads a compatibility wrapper, compare it with the native Termux runtime and use a consistent Node/npm pair for installation and execution. A `NODE_OPTIONS` preload can also affect Node commands; inspect it if even `node --version` fails.

## 2. Install OmniRoute

First check the current package metadata and its supported Node range:

```sh
npm view omniroute version engines os cpu
```

Install the published package globally:

```sh
npm install --global omniroute
npm list --global --depth=0
command -v omniroute
```

If npm is still running, do not start a second install in parallel. Check its process, most recent npm log, and free storage. If storage is critically low, stop and diagnose before retrying rather than repeatedly downloading the full dependency tree.

To inspect the latest npm log:

```sh
ls -t "$HOME"/.npm/_logs/* 2>/dev/null | head -1
```

Read the log path printed above with `tail -n 80 /path/to/npm-debug-log`. Preserve the first error as well as the final summary when asking for help.

## 3. Verify the CLI before configuration

Run:

```sh
omniroute --help
omniroute --version
```

Use only commands shown by the installed version's help/README for initialization, configuration, and server startup. If the command does not exist, check the global npm prefix and whether the install completed:

```sh
npm root --global
npm list --global omniroute
ls -l "$(command -v omniroute)" 2>/dev/null
```

### Troubleshooting: `/usr/bin/env: bad interpreter`

Termux usually keeps `env` at `$PREFIX/bin/env`; standard Linux npm shims may start with `#!/usr/bin/env node`. If the installed executable fails with `bad interpreter`, inspect it:

```sh
command -v omniroute
head -n 1 "$(command -v omniroute)"
command -v env
printf 'PREFIX=%s\n' "$PREFIX"
```

If the package entry point exists, invoke it directly with the intended Node executable to isolate the shebang issue. Replace the path if your global npm root differs:

```sh
node "$(npm root --global)/omniroute/bin/omniroute.mjs" --help
```

This diagnostic bypasses the shim only. It does not fix incompatible native modules, missing packages, unsupported Android APIs, or a Node runtime mismatch. Do not create a system-wide fake `/usr/bin/env` path.

### Troubleshooting: Node wrapper or `NODE_OPTIONS`

If a Node command fails with a message mentioning `NODE_OPTIONS` or a preload script, inspect the current shell environment:

```printf 'NODE_OPTIONS=%s\n' "$NODE_OPTIONS"
type -a node npm
node -p 'process.execPath'
```

For diagnosis only, compare with a clean environment and the native Termux Node binary (usually `$PREFIX/bin/node`):

```sh
env -u NODE_OPTIONS "$PREFIX/bin/node" --version
env -u NODE_OPTIONS "$PREFIX/bin/node" -p 'JSON.stringify({platform:process.platform, arch:process.arch, execPath:process.execPath})'
```

If npm and Node are provided by a compatibility layer, use that layer consistently or switch to the native Termux toolchain for a clean test. Do not delete compatibility files without understanding which other tools depend on them.

### Troubleshooting: native dependencies and platform mismatch

Termux reports Android's platform/ABI, which is not identical to GNU/Linux on the same CPU architecture. Packages that ship glibc-only binaries, desktop/Electron components, or platform-specific native addons may fail even on ARM64. Do not copy `node_modules` from desktop Linux, macOS, or Windows. Capture the exact failing package and error; check whether the upstream project supports Android/Termux before trying workarounds.

## 4. Configure providers and secrets

Use the configuration or onboarding method documented by the installed OmniRoute version. Verify the supported provider names, environment variable names, and config path from that version's help and example files.

- Keep credentials in the supported local configuration or environment mechanism.
- Restrict file permissions for files containing secrets (for example, `chmod 600` on a private config file where applicable).
- Do not commit local `.env` files, token files, or provider credentials.
- Do not paste secrets into chat, bug reports, screenshots, or terminal transcripts.
- If a credential was exposed, revoke and rotate it.

Do not assume a specific config command or environment variable: consult the version-matched documentation first.

## 5. Start and verify the service

Follow the installed CLI's documented startup command. In a separate Termux session, verify the process and the documented health/API endpoint:

```sh
ps -A | grep -i omniroute
```

If the project documents a health endpoint, query that exact local URL using the tool it specifies (for example, `curl -i http://127.0.0.1:<documented-port>/<documented-health-path>`). Do not guess the port or endpoint. Confirm all of the following before considering setup complete:

1. CLI help and version commands run successfully.
2. The process remains running after startup.
3. The documented health endpoint responds as expected.
4. A safe test request succeeds with a configured provider.
5. Logs show no missing dependency, authentication, or bind/listen error.

Keep the service bound to localhost unless remote access is intentionally configured and secured. Do not expose an unauthenticated API to the public internet.

## 6. Android background operation

Android may suspend or kill background processes. If long-running use is needed, investigate Termux's supported wake-lock and background-process guidance for your Android version. Battery optimization, vendor task management, and device restarts can affect uptime. Test persistence after screen-off and after reopening Termux; do not claim that a process is a permanent Android service unless you have verified that behavior.

## 7. Collect diagnostics for a bug report

Run these commands and redact secrets before sharing the output:

```sh
termux-info
node --version
npm --version
node -p 'JSON.stringify({platform: process.platform, arch: process.arch, execPath: process.execPath})'
npm list --global --depth=0
npm root --global
printf 'PREFIX=%s\nNODE_OPTIONS=%s\n' "$PREFIX" "$NODE_OPTIONS"
df -h "$HOME"
```

Include the OmniRoute version, Android version/device architecture, exact command, full error and relevant npm log lines. Redact access tokens, API keys, cookies, private hostnames, and personal data.

## 8. Uninstall or recover

If an installation is broken, inspect its state before removing anything:

```sh
npm list --global omniroute
npm root --global
```

If you decide to uninstall, use the same npm installation/prefix that installed it:

```sh
npm uninstall --global omniroute
```

Only remove npm cache data if you need the space and understand that a later installation may need to download it again. Avoid deleting the entire Termux prefix or broad directories as a troubleshooting shortcut.

## Compatibility caveat

This guide describes a careful installation and verification workflow, not a guarantee that every OmniRoute feature is supported on Android. Validate the CLI, server, providers, networking, and any native/desktop-dependent feature independently against the exact release being used.
