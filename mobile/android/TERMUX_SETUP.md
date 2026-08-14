# Termux setup for OmniRoute Mobile

The native app delegates runtime execution to Termux through the documented `RUN_COMMAND` integration.

## One-time Termux setup

1. Install and launch Termux at least once.
2. In Termux, configure external command execution:

```bash
mkdir -p ~/.termux
printf 'allow-external-apps=true\n' >> ~/.termux/termux.properties
```

3. Restart Termux after changing the property.
4. Android must grant OmniRoute the `com.termux.permission.RUN_COMMAND` permission.
5. Keep the managed runtime under:

```text
~/.omniroute-mobile/runtime
```

The native app does not access this directory directly. Commands execute inside Termux, which owns the filesystem.

## Security

`RUN_COMMAND` is a privileged integration. Only grant the permission to the trusted OmniRoute application. The app restricts commands to the Termux bash entrypoint and uses a dedicated runtime workspace.

Termux itself is not a VM/container security boundary. Do not run untrusted arbitrary code through this integration without an additional isolation layer.

## Runtime

The managed runtime installs Node.js if necessary and installs the pinned OmniRoute package. The gateway is configured for loopback access on port `20128`.
