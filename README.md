# ZZYOK BOT

Standalone BOT / CloudStore app for **ZZYOK Phone**, built with Minecraft Fabric.

![ZZYOK BOT icon](src/client/resources/assets/zzyokbot/icon.png)

## Features

- Cloud warehouse — deposit, withdraw, repair
- My BOT — start, stop, restart, logs
- Smart tasks and permissions
- Subscription and zzko wallet
- Recharge / redeem
- Server settings
- Account login and session support
- Native 360 × 660 phone UI, matching ZZYOK Phone
- Automatic default-browser fallback for HTTP/TLS-restricted networks

## Requirements

| Item | Version |
|---|---|
| Minecraft | 26.2 |
| Java | 25 |
| Fabric Loader | 0.19.5 |
| Fabric API | 0.161.0+26.2 |
| ZZYOK Phone | >= 1.1.4 |

## Install

1. Install **ZZYOK Phone** first.
2. Put the built `ZZYOKBot-x.y.z.jar` into `mods`.
3. Launch the Fabric 26.2 profile.
4. Open the phone and tap **我的BOT**.

The app icon is a robot icon. It opens the standalone BOT UI; no full phone shell is bundled.

## Build

The repository includes the compile-only SDK jar at `libs/zzyokphone-addon-api.jar`.

```bash
gradle build
```

Output:

```text
build/libs/ZZYOKBot-1.0.3.jar
```

The SDK jar is compile-only and is **not** bundled into the app jar; the ZZYOK Phone host provides those classes at runtime.

## Project layout

```text
src/client/java/cn/zzyok/zzyokphone/
├── bot/       BotAddon, interface docs screen
├── api/       CloudStore / wallet / BOT platform client
├── config/    local config
├── ui/        standalone BOT UI
└── util/      helpers
```

## API

See [docs/API.md](docs/API.md).

The app uses two services:

- CloudStore: `https://zzyok.cn`
- BOT Platform: `https://bot.zzyok.cn`

Wallet balance is synchronized from `GET /api/zzko` on login, opening the app,
recharge, and subscription purchase.

No API keys or credentials are stored in this repository.

## Third-party app integration

The app depends on the ZZYOK Phone addon API:

```json
{
  "entrypoints": {
    "zzyokphone-addon": ["cn.zzyok.zzyokphone.bot.BotAddon"]
  },
  "depends": {
    "zzyokphone": ">=1.1.4"
  }
}
```

Use `libs/zzyokphone-addon-api.jar` to compile. Do not bundle those API
classes into your jar.

## License

MIT. See [LICENSE](LICENSE).