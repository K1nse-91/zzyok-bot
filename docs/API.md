# ZZYOK BOT API 使用说明

## 1. 两个 Base

| 服务 | 默认地址 | 用途 |
|---|---|---|
| CloudStore 云仓库 | `https://zzyok.cn` | 登录、钱包、仓库、订阅、修装备 |
| Bot Platform | `https://bot.zzyok.cn` | BOT 管理、任务、权限、日志 |

地址可在 App 的「设置 → 服务器」里修改。

## 2. 会话

登录成功后服务端下发 Cookie，App 自动保存并在后续请求中携带。

```http
POST /api/login
Content-Type: application/json

{"username":"...","password":"..."}
```

验证码登录：

```text
POST /api/login/challenge
GET  /api/login/challenge
POST /api/login/challenge/cancel
```

## 3. 钱包 / 钱的同步

```http
GET /api/zzko
```

返回当前账号的 zzko 余额。App 在以下时机自动同步：

- 打开 BOT App 首页
- 登录成功
- 充值成功
- 订阅购买后
- 手动点「接口说明 → 同步余额」

`GET /api/me` 也会返回账号和钱包信息。

订阅购买：

```http
POST /api/subscriptions/purchase
Content-Type: application/json

{"ownerUuid":"...","planId":"..."}
```

## 4. 云仓库

```http
GET  /api/inventory?owner=<uuid>
GET  /api/admin/inventory          # 管理员
POST /api/action/deposit
POST /api/action/withdraw
POST /api/action/repair
```

## 5. BOT 平台

```http
GET  /api/bots
POST /api/bots/start
POST /api/bots/stop
POST /api/bots/restart
GET  /api/bots/logs?botId=<id>
GET  /api/smart/tasks?botId=<id>
GET  /api/smart/permissions?botId=<id>
POST /api/smart/tasks
POST /api/smart/permissions
```

所有接口返回 JSON，错误信息看 `errorMsg`。

## 6. 第三方 App 接入

第三方 App 通过 ZZYOK Phone App SDK 接入，不需要直接调用云仓库接口。

```java
public final class MyBotAddon implements ZzyokAddon {
    public String id() { return "mybot"; }
    public String label() { return "MyBOT"; }
    public String icon() { return "minecraft:command_block"; }
    public Screen createScreen(Minecraft mc) { return new MyBotScreen(); }
}
```

`fabric.mod.json`：

```json
{
  "entrypoints": {
    "zzyokphone-addon": ["com.example.MyBotAddon"]
  },
  "depends": { "zzyokphone": ">=1.1.4" }
}
```

打包放到 `mods`，手机桌面会自动出现 App 图标。