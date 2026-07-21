# Spice of Life: Fabric Flavor 1.20.1 MySQL 多子服部署指南

## 1. 适用版本

- Minecraft：`1.20.1`
- Java：`17`
- Fabric Loader：`0.17.3` 或更高的 1.20.1 兼容版本
- Fabric API：`0.92.6+1.20.1`
- 模组版本：`3.5.1+mc1.20.1`
- Cloth Config：`11.1.136`
- Mod Menu：`7.2.2`，仅客户端配置入口需要
- MySQL：已使用 `5.7.26` 完成集成与双服验收

本功能只同步本模组的“已食用食物 ID”和食物自身的数值快照。背包、位置、维度、玩家当前饥饿状态、睡眠状态和整个玩家 NBT 都不会写入 MySQL。

## 2. 一致性模型

玩家进入子服时会立即提交一次异步查询，并在配置的延迟时间后进行一次补偿查询。默认延迟为 5 秒。

- 正常数据库条件下，玩家切服后通常会在登录后的数百毫秒内收敛；
- 目标是登录后 5 秒内同步食物簿、已发现数量和生命增益；
- 这不是跨服事务锁，也不是严格实时消息系统；
- 在线期间不会周期轮询数据库；
- 不需要 Redis，也不需要代理在切服时等待保存；
- 数据库故障、网络超时或执行队列拥堵时，收敛时间可能超过 5 秒，服务器会进入本地 NBT 回退状态并重试。

真实双服验收中，移除目标子服本地 `playerdata` 后，首次检测玩家在线到生命增益恢复的耗时为 `123.8 ms`。

## 3. 安装文件

每个 Fabric 子服的 `mods` 目录至少需要：

- `sol2f-3.5.1+mc1.20.1.jar`
- `fabric-api-0.92.6+1.20.1.jar`
- `cloth-config-fabric-11.1.136.jar`

玩家客户端也必须安装相同 Sol2F 主版本、Fabric API 和 Cloth Config。需要从 Mod Menu 打开配置页时，再安装 `modmenu-7.2.2.jar`。

## 4. 创建数据库与最小权限账号

以下 SQL 需要由数据库管理员执行。请按实际内网网段调整账号允许来源，不要直接允许公网任意地址连接。

```sql
CREATE DATABASE sol2f
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE USER 'sol2f'@'10.%'
  IDENTIFIED BY '请替换为强密码';

GRANT SELECT, INSERT, UPDATE, CREATE, ALTER
  ON sol2f.*
  TO 'sol2f'@'10.%';

FLUSH PRIVILEGES;
```

模组会自动创建以下两张表：

- `sol2f_player_state`：保存同步组、玩家 UUID、当前 generation 和最后玩家名；
- `sol2f_consumed_food`：按“同步组 + UUID + generation + 食物 ID”一食物一行保存，并记录 `hunger_points` 与 `saturation_modifier`。

运行期不需要 `DROP`、`DELETE` 或数据库级管理员权限。`3.5.0` 会自动为旧表增加两个数值列，因此从旧版升级时需要一次 `ALTER` 权限。迁移完成后可移除 `CREATE` 和 `ALTER`，但后续版本若再次调整表结构，需要临时恢复。

## 5. 数据库配置文件

首次启动会生成：

```text
config/spice-of-life-fabric-flavor-database.json
```

示例：

```json
{
  "enabled": true,
  "jdbcUrl": "jdbc:mysql://127.0.0.1:3306/sol2f?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai",
  "username": "sol2f",
  "password": "请替换为强密码",
  "syncGroup": "main-network",
  "serverId": "survival-1",
  "maximumPoolSize": 4,
  "minimumIdle": 1,
  "connectionTimeoutMs": 3000,
  "queryTimeoutSeconds": 5,
  "executorThreads": 2,
  "executorQueueSize": 1024,
  "loginReconcileDelaySeconds": 5,
  "retryDelaySeconds": 5,
  "shutdownWaitSeconds": 5
}
```

### 5.1 配置项说明

| 配置项 | 默认值 | 有效范围 | 说明 |
| --- | --- | --- | --- |
| `enabled` | `false` | `true/false` | 是否启用 MySQL 多服同步。关闭时只使用本地玩家 NBT。 |
| `jdbcUrl` | 本机 `3306/minecraft` | 必须以 `jdbc:mysql://` 开头 | JDBC 连接地址。不要把密码放在 URL 中。 |
| `username` | `sol2f` | 启用时不能为空 | 数据库账号。 |
| `password` | `change-me` | 字符串 | 数据库密码。配置文件是明文，请限制文件系统权限。密码不会写入状态命令或启动日志。 |
| `syncGroup` | `main-network` | `1-64` 位字母、数字、点、下划线或短横线 | 同一玩家网络的所有子服必须一致。不同网络必须使用不同值。 |
| `serverId` | `replace-me` | 与 `syncGroup` 相同的字符规则 | 每个子服必须唯一。启用数据库时不能保留 `replace-me`。 |
| `maximumPoolSize` | `4` | `1-16` | HikariCP 最大连接数。普通子服不建议盲目增大。 |
| `minimumIdle` | `1` | `0-maximumPoolSize` | 最小空闲连接数。大量子服共享数据库时可设为 `0` 或 `1`。 |
| `connectionTimeoutMs` | `3000` | `1000-30000 ms` | 获取数据库连接的最长等待时间，只发生在数据库线程。 |
| `queryTimeoutSeconds` | `5` | `1-30 秒` | 单条 SQL 超时。 |
| `executorThreads` | `2` | `1-8` | 本模组专用数据库线程数。 |
| `executorQueueSize` | `1024` | `64-16384` | 数据库任务有界队列容量。队列满时会明确告警并保留本地 dirty 状态。 |
| `loginReconcileDelaySeconds` | `5` | `1-30 秒` | 登录后的单次补偿查询延迟。 |
| `retryDelaySeconds` | `5` | `1-300 秒` | 查询、写入或初始化失败后的重试延迟。 |
| `shutdownWaitSeconds` | `5` | `1-30 秒` | 停服时等待已提交数据库任务的最长时间。 |

配置超出范围时会被限制到安全范围；ID、URL 或账号等关键配置无效时，MySQL 同步不会启动，并在日志中说明原因。

## 6. 多子服配置示例

三个子服应配置为：

| 子服 | `syncGroup` | `serverId` |
| --- | --- | --- |
| 生存一服 | `main-network` | `survival-1` |
| 生存二服 | `main-network` | `survival-2` |
| 资源服 | `main-network` | `resource-1` |

如果另有完全独立的测试网络，应使用例如 `test-network` 的新 `syncGroup`，避免测试玩家污染正式数据。

## 7. UUID 与代理要求

数据库主身份是玩家 UUID，玩家名只用于审计显示。

- 直接连接且 `online-mode=true`：使用 Mojang 正版 UUID；
- 代理后的离线后端：必须使用与代理匹配的 Fabric 转发方案，让所有子服收到相同、可信的 UUID；
- Velocity 应使用兼容 Fabric 1.20.1 的现代转发实现并配置相同密钥；
- BungeeCord/Waterfall 同样需要 Fabric 后端支持的安全转发实现；
- 后端处于 `online-mode=false` 时必须只监听内网地址，并由防火墙阻止玩家绕过代理直连；
- 不允许有的子服使用正版 UUID、有的子服使用离线名称 UUID，否则同一个玩家会在数据库中变成两个人。

部署前应在所有子服日志或数据库 `player_uuid` 字段中核对同一测试账号的 UUID 完全一致。

## 8. 登录、食用与清空流程

### 8.1 登录

1. 服务端线程读取本地 NBT 作为即时回退快照；
2. 专用数据库线程异步读取当前 generation、食物集合和已经记录的食物数值；
3. 数据库线程只返回不可变数据，不访问玩家实体、世界或 NBT；
4. 结果通过 `MinecraftServer.execute` 回到服务端线程；
5. 校验 `sessionToken` 和 generation 后更新玩家属性、NBT和客户端数据；
6. 默认 5 秒后再执行一次补偿查询。

### 8.2 首次食用

1. 服务端确认物品具有标准 1.20.1 `FoodComponent`；
2. 检查黑白名单和数量上限；
3. 服务端线程立即更新内存、玩家属性和客户端；
4. 仅把新增食物 ID、饥饿值和饱和度系数作为小型幂等写入异步提交；
5. Tick、Tooltip、GUI和普通命令不会直接查询数据库。

### 8.3 清空与死亡重置

`/sol2f clearhealthy` 和 `resetOnDeath=true` 使用 generation 递增，而不是删除全部旧行。

例如玩家当前 generation 为 `2`，清空后变为 `3`。即使 generation `2` 的旧异步写入稍后到达，登录查询也只读取 generation `3`，旧数据不能复活。

`generation` 不是食物数量，也不参与生命值公式。它只是玩家这份食物进度的“代次编号”：清空前的行留在旧代次，新登录只读取当前代次。

### 8.4 食物数值和生命公式

食物的两个原始字段来自 Minecraft 1.20.1 标准 `FoodComponent`：

- `hunger_points`：食物恢复的饥饿值，例如生牛肉为 `3`、熟牛肉为 `8`；
- `saturation_modifier`：原版饱和度系数，例如生牛肉为 `0.3`、熟牛肉为 `0.8`。

理论饱和度点数按 `hunger_points × saturation_modifier × 2` 计算。这里保存的是食物固有数值，不保存玩家本次实际增加了多少；实际增量会受玩家当前饥饿值、饱和度和原版上限裁剪影响，不适合作为永久成长依据。

生命表达式变量：

- `uniqueFoods`：当前有效食物数量；
- `totalHunger`：当前有效食物 `hunger_points` 总和；
- `totalSaturation`：当前有效食物理论饱和度总和。

默认配置就是每 1 点食物饥饿值增加 `0.1` 点生命上限：

```json
"healthGain": 0,
"Expression": "totalHunger * 0.1"
```

模组不会覆盖已经存在的功能配置文件。旧服务器升级到 `3.5.1` 后，需要在每个子服手动修改一次这两个字段，并保持跨服公式一致。

## 9. 命令与诊断

```text
/sol2f status
```

仅管理员可执行，输出示例：

```text
sol2f mysql: ready, group=main-network, server=survival-1, sessions=37, queued=0
```

状态含义：

- `ready`：连接池和业务表已经可用；
- `fallback`：数据库尚不可用或玩家操作正在回退到本地 NBT；
- `disabled`：数据库同步未启用或配置无效；
- `sessions`：当前在线同步会话数；
- `queued`：有界数据库执行器等待任务数。

其他命令：

```text
/sol2f clearhealthy
/sol2f getlist <player>
/sol2f sync
/sol2f sync PlayerData
/sol2f sync AllFoodList
```

`clearhealthy` 必须由玩家身份执行。控制台可使用管理工具切换到玩家身份，但日常操作建议玩家本人以管理员权限执行。

## 10. 数据库故障行为

数据库启动失败时：

- Fabric 服务端仍会继续启动；
- 玩家使用本地 NBT 数据进入游戏；
- 日志显示初始化失败原因，但不会打印密码；
- 后台按 `retryDelaySeconds` 重试；
- 数据库恢复后，在线玩家会重新异步加载并合并 dirty 数据。

写入失败时：

- 当前游戏体验和服务器 tick 不等待 JDBC；
- 未确认食物保留在 `pendingFoods` 和本地 dirty NBT 中；
- 后续重新加载时按相同 generation 补写；
- 队列满会明确记录错误，不会创建无限任务。

停服时：

- 不再接受新同步任务；
- 最多等待 `shutdownWaitSeconds`；
- 超时会报告未完成的排队任务数量；
- HikariCP 随后关闭连接和后台线程。

## 11. 不同子服模组或配置不一致

数据库保留整个同步组见过的食物 ID，但每个子服只使用本服当前存在且通过黑白名单的食物计算生命值。

因此：

- A 服存在某食物、B 服没有时，记录不会丢失；
- 玩家在 B 服不会因为缺失物品获得该食物的生命值；
- 回到 A 服后会重新计入；
- 各子服黑白名单不一致会造成生命值不同，建议正式网络保持一致配置。

## 12. 生产部署顺序

1. 备份各子服和数据库；
2. 确认所有后端收到相同可信 UUID；
3. 创建数据库、账号和最小权限；
4. 先在一个非正式子服安装 1.20.1 Jar 和依赖；
5. 修改数据库配置，确保 `serverId` 唯一；
6. 启动并执行 `/sol2f status`；
7. 用一个测试玩家食用新食物，确认两张表出现对应 UUID；
8. 登录第二个子服，检查 5 秒内食物簿和生命值收敛；
9. 验证 `/sol2f clearhealthy` 后旧 generation 不复活；
10. 再逐台部署到其他子服。

不要让旧版和新版长期同时写同一批玩家数据。回滚前应停服并备份数据库，确认旧版本是否理解新版写入的本地 NBT 字段。

## 13. 许可与再分发

原仓库许可证标记为 `ARR`。公开 Fork、二次分发或商业发布前，应确认原作者许可范围并保留原作者、项目来源和许可证信息。本指南不构成授权。
