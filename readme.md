# Spice of Life: Fabric Flavor

[English](#english)

这是一个面向 Fabric 的食物探索模组。玩家首次食用新的标准食物时会获得可配置的最大生命值增益，并可通过食物簿查看已发现食物。

当前 `1.20.1` 分支版本：`3.4.0+mc1.20.1`。

## 版本与依赖

- Minecraft `1.20.1`
- Java `17`
- Fabric Loader `0.17.3` 或更高的 1.20.1 兼容版本
- Fabric API `0.92.6+1.20.1`
- Cloth Config `11.1.136`，服务端和客户端必装
- Mod Menu `7.2.2`，仅客户端配置页面入口需要

服务端和客户端必须安装相同主版本的 Sol2F。专用服务器不需要 Mod Menu。

## 主要功能

- 首次食用新食物增加最大生命值；
- 支持原版食物和使用 1.20.1 标准 `FoodComponent` 注册的普通 Fabric Mod 食物；
- 食物黑名单、白名单和自定义生命值表达式；
- 可选的自然饥饿和睡眠饥饿系统；
- 食物簿、概览页面、鼠标滚轮、Tooltip 和 H 快捷键；
- 异步 MySQL 多子服玩家食物进度同步；
- 数据库故障时本地玩家 NBT 回退；
- generation 清空机制，防止旧异步写入复活已清空数据。

## 安装

将以下文件放入每个 Fabric 1.20.1 服务端的 `mods` 目录：

```text
sol2f-3.4.0+mc1.20.1.jar
fabric-api-0.92.6+1.20.1.jar
cloth-config-fabric-11.1.136.jar
```

客户端安装相同文件。需要从 Mod Menu 打开配置页面时，再安装：

```text
modmenu-7.2.2.jar
```

## 服务端功能配置

配置文件：

```text
config/spice-of-life-fabric-flavor.json
```

### 生命增益 `health`

| 配置项 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `maxHealth` | int | `5000` | 本模组可提供的最大额外生命值，不限制原版基础值或其他 Mod 增益。 |
| `healthGain` | int | `2` | 每发现一种有效食物增加的生命值。`2 health = 1 颗红心`。 |
| `resetOnDeath` | boolean | `false` | 开启后，玩家死亡时异步清空当前食物进度。MySQL 模式使用 generation 防复活。 |
| `healthToMaxOnIncrease` | boolean | `false` | 首次发现食物并增加上限后，是否恢复到当前满血。登录、重生和数据库加载不会触发。 |
| `healthIncreaseOnIncrease` | int | `0` | 首次发现食物后恢复的生命值数量，`0` 表示不额外治疗。 |
| `Expression` | string | `"0"` | 额外生命值表达式，可使用变量 `uniqueFoods`。 |
| `BlackList` | string list | 腐肉、蜘蛛眼 | 不记录且不参与本服生命计算的食物 ID。 |
| `WhiteList` | string list | 空 | 非空时只记录和计算列表中的食物 ID。 |

最终增益计算：

```text
本模组增益 = min(
  有效已发现食物数 × healthGain + Expression(uniqueFoods),
  maxHealth
)
```

各子服模组或黑白名单不同时，MySQL 会保留同步组见过的全局食物 ID，但本服生命值只统计本服存在且通过当前黑白名单的食物。

### 饥饿系统 `hunger`

| 配置项 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `EnableNaturalHunger` | boolean | `false` | 是否启用周期自然饥饿。 |
| `NaturalHungerPeriod` | int | `12000` | 自然饥饿触发周期，单位 tick；`20 tick = 1 秒`。小于等于 0 时不执行。 |
| `NaturalHungerExpression` | string | `"1"` | 每次周期减少的饥饿值表达式。 |
| `EnableSleepHunger` | boolean | `false` | 是否根据睡眠时长减少饥饿值。 |
| `SleepHungerExpression` | string | `"SleepDuration / 3000"` | 睡眠饥饿表达式，`SleepDuration` 单位为 tick。 |
| `MinFoodLevel` | int | `1` | 自然/睡眠饥饿允许降低到的最低饥饿值。 |

### 高级配置 `dev`

| 配置项 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `DeveloperMode` | boolean | `false` | 输出额外诊断信息。正式大型模组服不建议长期开启。 |

## 表达式

生命值表达式可使用 `uniqueFoods`，睡眠表达式可使用 `SleepDuration`。内置常量包括 `e` 和 `pi`。

支持的自定义函数：

```text
floor(x)
ceil(x)
round(x)
min(a, b)
max(a, b)
pow(a, b)
log(base, value)
if(condition, trueValue, falseValue)
or(a, b)
and(a, b)
not(x)
lt(a, b)
gt(a, b)
eq(a, b)
neq(a, b)
```

逻辑和比较函数返回 `1` 或 `0`。

## 客户端配置

配置文件：

```text
config/spice-of-life-fabric-flavor-client.json
```

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `GUISetting.ShowConsumedTooltips` | `true` | 为已发现食物显示 Tooltip。 |
| `GUISetting.ShowUnconsumedTooltips` | `true` | 为本服可发现但尚未发现的食物显示 Tooltip。 |

默认按键 `H` 打开食物簿，可在 Minecraft 控制设置中重新绑定。食物簿支持鼠标滚轮和右侧滚动条。

## MySQL 多子服同步

数据库配置文件：

```text
config/spice-of-life-fabric-flavor-database.json
```

默认关闭。启用后：

- 使用 UUID 作为玩家身份；
- `syncGroup` 隔离不同服务器网络；
- 每个子服使用唯一 `serverId`；
- 登录立即异步加载，并在默认 5 秒后进行一次补偿查询；
- JDBC、建表、查询、写入、重试和清空都在专用有界线程池执行；
- 数据库线程不会访问玩家实体、世界或 NBT；
- Tick、Tooltip、食物簿和普通读取命令不会查询数据库；
- 数据库故障时继续使用本地 NBT，恢复后自动重连；
- `/sol2f clearhealthy` 与死亡清空使用 generation，旧写入不会复活。

完整 SQL、权限、全部配置范围、代理 UUID、安全边界和部署顺序：

- [MySQL 多子服部署指南](docs/MySQL多子服部署指南.md)
- [1.20.1 降级与 MySQL 多服同步分析](docs/1.20.1-Fabric降级与MySQL多服同步分析.md)

## 命令

### 数据库状态

```text
/sol2f status
```

需要管理员权限。显示 `ready/fallback/disabled`、同步组、子服 ID、在线会话数和数据库队列长度，不会显示密码。

### 清空当前玩家

```text
/sol2f clearhealthy
```

需要管理员权限并由玩家身份执行。MySQL 模式会先异步递增 generation，成功后再清空客户端和生命增益；失败不会伪装成成功。

### 查询食物列表

```text
/sol2f getlist
/sol2f getlist <player>
```

控制台执行时必须指定在线玩家名。

### 重新同步客户端显示

```text
/sol2f sync
/sol2f sync PlayerData
/sol2f sync AllFoodList
```

这些命令只发送当前服务端内存快照，不会在命令线程查询数据库。

## 食物兼容边界

1.20.1 版本通过 `Item#getFoodComponent()` 判断标准食物。普通 Fabric Mod 使用标准 `FoodComponent` 注册的食物可以直接识别。

以下类型需要单独实测或兼容：

- 不使用标准 `FoodComponent`、只在自定义 `use` 逻辑中模拟进食的物品；
- 同一个 Item 根据 NBT 动态决定是否可食用的非标准实现；
- 绕过原版 `ItemStack.finishUsing` 的自定义消费流程。

牛奶桶、药水等非食物即使调用 `finishUsing` 也不会被记录。

## 构建与测试

使用 Java 17：

```powershell
$env:JAVA_HOME='C:\path\to\jdk-17'
.\gradlew.bat clean build
```

真实 MySQL 集成测试通过以下环境变量启用：

```powershell
$env:SOL2F_TEST_MYSQL_URL='jdbc:mysql://127.0.0.1:3306/sol2f_test?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai'
$env:SOL2F_TEST_MYSQL_USER='sol2f_test'
$env:SOL2F_TEST_MYSQL_PASSWORD='replace-me'
.\gradlew.bat clean test
```

没有提供环境变量时，MySQL 集成测试会跳过，不能把 `NO-SOURCE` 或只编译成功当作数据库业务验收。

## 验收状态

1.20.1 分支已经完成以下本地真实验收：

- Java 17 clean build，真实 MySQL 测试 `16/16` 通过；
- MySQL 5.7.26 建表、幂等写、并发合并和 generation；
- 两个独立 Fabric 1.20.1 专用服务器共享 MySQL；
- 真实 Fabric 客户端 A→B 同步；
- 移走目标服本地 `playerdata` 后，登录到生命增益恢复耗时 `123.8 ms`；
- `clearhealthy`、死亡重置、维度切换、退出重进；
- 原版食物、普通 Mod 标准食物和非食物 `finishUsing`；
- 食物簿、H 快捷键、Tooltip 和 Cloth Config 页面。

当前最终 Jar：`build/libs/sol2f-3.4.0+mc1.20.1.jar`，SHA-256：`5DA7E3EEA9C237B0CF76314A6C733E445965AB5B2A20EE0E93139CC6496B9E8A`。

## 许可证和来源

原作者：`xianfish`

原项目：[xianfish33/Spice-of-Life-Fabric-Flavor](https://github.com/xianfish33/Spice-of-Life-Fabric-Flavor)

仓库许可证标记为 `ARR`。公开 Fork、再分发或商业发布前，请确认原作者授权范围，并保留原作者、来源和许可证信息。

---

<a id="english"></a>

# English

The `1.20.1` branch provides Spice of Life: Fabric Flavor `3.4.0+mc1.20.1` for Minecraft 1.20.1 and Java 17.

## Requirements

- Fabric Loader `0.17.3+`
- Fabric API `0.92.6+1.20.1`
- Cloth Config `11.1.136`
- Mod Menu `7.2.2` is optional and only required as a client config-screen entry point

Install the same Sol2F major version, Fabric API, and Cloth Config on the server and client.

## Features

- Maximum-health rewards for discovering standard vanilla and modded foods
- Food blacklist, whitelist, and expression-based bonuses
- Food Book, overview screen, tooltips, scrolling, and the default `H` key
- Optional natural and sleep hunger systems
- Asynchronous MySQL synchronization for multiple Fabric backend servers
- UUID identity, `syncGroup` isolation, unique `serverId`, bounded queues, and local-NBT fallback
- Generation-based clears that prevent stale asynchronous writes from restoring deleted progress

## MySQL behavior

Only consumed food IDs are synchronized. Inventory, location, dimension, hunger, and full player NBT are not stored in MySQL.

The target backend performs an immediate asynchronous load on login and one delayed reconciliation, five seconds by default. JDBC never runs on the Minecraft server thread. GUI, tooltip, tick, and normal read commands never query the database.

See [MySQL Multi-Server Deployment Guide](docs/MySQL多子服部署指南.md) for SQL permissions, every configuration field, proxy UUID requirements, failure behavior, and rollout steps.

## Commands

```text
/sol2f status
/sol2f clearhealthy
/sol2f getlist <player>
/sol2f sync
/sol2f sync PlayerData
/sol2f sync AllFoodList
```

`/sol2f status` never exposes the database password. `clearhealthy` must run with a player command source.

## Food compatibility

Minecraft 1.20.1 foods are detected through `Item#getFoodComponent()`. Standard Fabric foods work directly. Custom items that bypass both `FoodComponent` and vanilla `ItemStack.finishUsing` require explicit compatibility work. Non-food consumables such as milk buckets are not recorded.

## License

Original author: `xianfish`

Original project: [xianfish33/Spice-of-Life-Fabric-Flavor](https://github.com/xianfish33/Spice-of-Life-Fabric-Flavor)

The repository is marked `ARR`. Confirm redistribution permission before publishing a fork or binary release.
