# Hyper-Heartrate-Bukkit

<!-- markdownlint-disable MD033 -->

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Paper](https://img.shields.io/badge/Paper-26.2-blueviolet)](https://papermc.io)
[![Java](https://img.shields.io/badge/Java-25-orange)](https://adoptium.net)
[![PlaceholderAPI](https://img.shields.io/badge/PAPI-2.11.7-yellow)](https://www.spigotmc.org/resources/placeholderapi.6245/)
[![NickPlus](https://img.shields.io/badge/NickPlus-%E5%85%BC%E5%AE%B9-brightgreen)](https://github.com/Shallow-Y/NickPlus)

Bukkit 服务端插件 — 作为 [Hyper Heartrate](https://github.com/ChaYeWuu/Hyper-Heartrate) Fabric 模组的服务端中转，接收并转发玩家心率数据，支持 PlaceholderAPI 变量，兼容 NickPlus 匿名身份伪装。

## 概述

装有 Hyper Heartrate Fabric 模组的客户端通过 **Bukkit Plugin Messaging** 通道将心率数据发送到服务端，本插件接收后广播给附近安装了模组的其他玩家，实现联机心率同步。

### 数据流

```
[客户端 A Fabric 模组]          [服务端本插件]          [客户端 B Fabric 模组]
       │                              │                         │
       │──C2S──心率数据──────────────→│                         │
       │ (hyper-heartrate:hr_c2s)     │──S2C──UUID+心率───────→│
       │                              │ (hyper-heartrate:hr_s2c)│
```

## 环境要求

- [Paper 26.2](https://papermc.io) 服务端（或兼容 Paper API 26.2 的派生端）
- Java 25+
- 客户端需安装 [Hyper Heartrate](https://github.com/ChaYeWuu/Hyper-Heartrate) Fabric 模组

## 安装

1. 从 [Releases](../../releases) 下载最新版 `Hyper-Heartrate-Bukkit-*.jar`
2. 放入服务端的 `plugins/` 目录
3. 重启服务端

## 协议

通过 Bukkit Plugin Messaging 通道传输数据，与 Fabric 模组的 `MultiplayerNetworking` 协议完全对齐：

| 通道 | 方向 | 数据格式 | 说明 |
|------|------|----------|------|
| `hyper-heartrate:hr_c2s` | 客户端→服务端 | `VarInt(heartRate)` | 客户端发送自身心率 |
| `hyper-heartrate:hr_s2c` | 服务端→客户端 | `UUID(16字节) + VarInt(heartRate)` | 广播给附近玩家 |

### 广播规则

- 仅向 **64 格内**的其他玩家发送
- 仅向注册了 `hyper-heartrate:hr_s2c` 通道的客户端发送（即安装了模组的玩家）
- 不阻塞未安装模组的玩家正常游戏

## 构建

```bash
git clone https://github.com/ChaYeWuu/Hyper-Heartrate-Bukkit.git
cd Hyper-Heartrate-Bukkit
./gradlew build
```

产物位于 `build/libs/Hyper-Heartrate-Bukkit-*.jar`

## 开发

本插件不含任何 GUI/HUD 等客户端功能，仅负责服务端的数据中转。所有客户端显示逻辑由 [Hyper Heartrate](https://github.com/ChaYeWuu/Hyper-Heartrate) Fabric 模组处理。

### 结构

```
src/main/java/com/chayewuu/hyperheartratebukkit/
├── HeartRateBukkitPlugin.java          # 插件主类，注册通道与事件
├── HeartRatePlaceholderExpansion.java  # PlaceholderAPI 变量扩展
├── util/
│   └── VarIntUtil.java                 # VarInt/UUID 编解码（与 Minecraft 协议兼容）
├── network/
│   ├── RemoteHeartRateStore.java       # 远程心率存储（10秒过期）
│   └── HeartRateMessageListener.java   # 消息监听与中转
└── integration/
    └── NickPlusBridge.java             # NickPlus Fake UUID 兼容桥接
```

## NickPlus 兼容

本插件自动兼容 [NickPlus](https://github.com/Shallow-Y/NickPlus) 匿名身份伪装插件。

当玩家使用 `/nick` 匿名后，NickPlus 会为其生成一个 Fake UUID，客户端看到的是伪装身份。本插件在 S2C 广播时自动检测并替换为 Fake UUID，确保：

- **其他玩家**：TAB 列表和 nametag 上的心率正确显示（收到 Fake UUID 匹配）
- **自己**：匿名玩家自己的 TAB 列表也能看到自己的心率（S2C 也发给自己）
- **存储**：`RemoteHeartRateStore` 始终使用真实 UUID，Placeholder 查询不受影响

### 依赖关系

```
NickPlus ──→ PlaceholderAPI ──→ Hyper-Heartrate-Bukkit
  (提供 %nickplus_fakeuuid%)    (通过 PAPI 桥接读取)
```

无需额外配置，安装 PlaceholderAPI 和 NickPlus 后自动生效。

## PlaceholderAPI 变量

本插件提供以下 PlaceholderAPI 变量（需服务端安装 [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/)）：

| 变量 | 说明 | 示例 |
|------|------|------|
| `%hyperheartrate_heartrate%` | 当前玩家心率 | `72` |
| `%hyperheartrate_heartrate_<玩家名>%` | 指定玩家心率 | `85` |
| `%hyperheartrate_heartrate_color%` | 带颜色代码的心率 | `§a72` |
| `%hyperheartrate_heartrate_color_<玩家名>%` | 指定玩家的带色心率 | `§e100` |
| `%hyperheartrate_status%` | 当前玩家心率状态 | `§a有数据` |
| `%hyperheartrate_status_<玩家名>%` | 指定玩家心率状态 | `§7无数据` |

### 颜色规则

- **< 60 BPM** → `§b` 蓝色（心动过缓）
- **60–100 BPM** → `§a` 绿色（正常）
- **100–140 BPM** → `§e` 黄色（偏高）
- **> 140 BPM** → `§c` 红色（过高）

## 许可

MIT License — 详见 [LICENSE](LICENSE)

## 相关项目

- [Hyper Heartrate](https://github.com/ChaYeWuu/Hyper-Heartrate) — Fabric 客户端模组，用于采集和显示心率数据