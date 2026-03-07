
---
### Switch Language:

<a href="#EN">English</a>

---

<H1 id="CN">概述</H1>

本mod旨在通过增加血量上限奖励玩家食用新的食物，以加强玩家对整合包食物系统的动力，并平衡整合包的游戏难度。

# mod ~~*食用*~~ 指南

该指南的config和GUI说明部分为最新版本（3.2.0）维护，其他版本呢详情请参考游戏内配置页面提示。

## 关于配置

所有配置项以health计算，2health=1heart（红心）

### 服务端配置

这些配置存放于 ``/config/spice-of-life-fabric-flavor.json``。
这些配置在服务端生效，本地游戏版本的配置影响本地游戏存档。

#### 生命增益系统
<details><summary>maxHealth | 最大血量</summary>

- 类型: int
- 默认值: 5000
- 取值范围: 2~5000
- 说明: 设置本 Mod 能提供的最大额外生命值(玩家最终血量 = 原版血量 + 其他 Mod 增益 + 本 Mod 增益。此选项仅限制“本 Mod 增益”的上限,防止数值无限膨胀)

</details>

<details><summary>healthGain | 生命值增益</summary>

- 类型: int
- 默认值: 2
- 取值范围: 0~20
- 说明: 每发现一个食物的生命值增益(设置此为0则等于禁用)

</details>

<details><summary>resetOnDeath | 死亡重置</summary>

- 类型: boolean
- 默认值: false
- 说明: 启用后,玩家死亡时血量将重置为默认值,且删除所有已发现记录

</details>

<details><summary>healthToMaxOnIncrease | 增益时恢复血量</summary>

- 类型: int
- 默认值: 0
- 取值范围: 任意
- 说明: 每次生命值增益时恢复血量数值,若大于上限则恢复到上限

</details>

<details><summary>healthIncreaseOnIncrease | 增益时恢复满血</summary>

- 类型: boolean
- 默认值: false
- 说明: 每次生命值增益时恢复血量至上限

</details>

<details><summary>Expression | 生命值增益计算函数</summary>

- 类型: string
- 默认值: 0
- 说明: 写法详见下文<a href="#ExpU">函数编写指南</a><br>
可用变量: `uniqueFoods`食用过的食物数


</details>

<details><summary>blacklist | 黑名单</summary>

- 类型: List
- 默认值:

```json
[
  "minecraft:rotten_flesh",
  "minecraft:spider_eye"
],
```

- 说明：列表中的食物不再会被记录(在黑名单修改后,玩家已发现的食物不受影响,未发现的食物不再计入统计)
  **相关机制和操作说明**:
  客户端的tooltip会显示所有已发现食物(不管是否存在于黑名单,机制应用于保存黑名单前已发现某一食物),未发现且处于黑名单内的食物不显示.修改后使用 /sol2f sync AllFoodList 同步数据以确保tooltip显示正常<br>
  客户端的GUI在修改配置后需要使用sync命令同步数据.包括: 在修改了血量增益计算相关配置后,血量概述在执行sync后会重新计算并显示在客户端;在重设黑名单后,执行同步以同步客户端GUI的食物概述.<br>
  客户端的食物概述显示的是所有已发现食物/非黑名单食物,而不是所有已发现食物/所有已发现食物+非黑名单食物的并集

</details>

> 在修改血量配置后,食用未发现食物,或重生(未开启死亡重置配置)以重新计算血量增益值 <br>
> 机制: 在每次食用新食物、死亡重生时会按照最新配置计算血量增益值

#### 饱食度衰减系统
<details><summary>EnableNaturalHunger | 启用自然饥饿</summary>

- 类型: boolean
- 默认值: false
- 说明: 启用后,玩家会随时间损失饥饿值

</details>

<details><summary>NaturalHungerPeriod | 自然饥饿周期</summary>

- 类型: int
- 默认值: 1200
- 说明: 配置值代表tick(游戏刻 [↗ 游戏刻-Minecraft Wiki](https://zh.minecraft.wiki/w/%E5%88%BB?variant=zh-cn#%E6%B8%B8%E6%88%8F%E5%88%BB)),每达到一定配置的游戏刻,按照<a href="#NaturalHungerExpression">函数</a>计算衰减

</details>

<details id="NaturalHungerExpression"><summary>NaturalHungerExpression | 自然饥饿衰减计算函数</summary>

- 类型: string
- 默认值: "1"
- 说明: 用于计算每次自然衰减值的函数(将玩家的饱食度减少此函数的结果,而非设置为此函数的结果),写法详见下文<a href="#ExpU">函数编写指南</a><br>
  **可用变量 (Variables):**
  - **动作**: `isSprinting` (疾跑), `isSwimming` (游泳), `isSneaking` (潜行), `isCrawling` (爬行)
  - **环境**: `isTouchingWater` (接触水), `isBeingRainedOn` (淋雨), `isWet` (潮湿(接触水或淋雨或接触气泡柱)), `BrightnessAtEyes` (玩家眼镜坐标的**渲染亮度**(0.0-1.0) [↗ 渲染亮度|Minecraft Wiki](https://zh.minecraft.wiki/w/%E4%BA%AE%E5%BA%A6#%E6%B8%B2%E6%9F%93%E4%BA%AE%E5%BA%A6))
  - **状态**: `FoodLevel` (当前饥饿值), `Saturation` (饱食度), `Exhaustion` (消耗度) [↗ 饥饿机制详解-Minecraft Wiki](https://zh.minecraft.wiki/w/%E9%A5%A5%E9%A5%BF#%E6%9C%BA%E5%88%B6)<br>
  *注：所有布尔变量在公式中为 `1.0` (真) 或 `0.0` (假)*<br>
  **配方示例:**
  1.  **基础固定消耗**: 每周期扣 0.5 点。
    ```json
    "NaturalHungerExpression": "0.5"
    ```
  2.  **环境动态消耗**:
    疾跑或游泳：扣 4 点,淋雨或站在水中：扣 2 点,其他情况：扣 1 点
    ```json
    "NaturalHungerExpression": "if(or(isSprinting, isSwimming), 4, if(or(isBeingRainedOn, isTouchingWater), 2, 1))"
    ```
  3.  **黑暗生存挑战**: 在黑暗处 (`BrightnessAtEyes` < 0.1) 消耗加倍。
    ```json
    "NaturalHungerExpression": "if(lt(BrightnessAtEyes, 0.1), 2, 1)"
    ```

</details>

<details><summary>EnableSleepHunger | 启用睡眠饥饿</summary>

- 类型: boolean
- 默认值: false
- 说明: 启用后,玩家睡醒时会根据<a href="#SleepHungerExpression">函数</a>扣除一定饥饿值

</details>

<details id="SleepHungerExpression"><summary>SleepHungerExpression | 睡眠饥饿衰减计算函数</summary>

- 类型: string
- 默认值: "SleepDuration / 3000"
- 说明: 用于计算每次睡醒时衰减的饱食度的值(将玩家的饱食度减少此函数的结果,而非设置为此函数的结果),指南详见下文<a href="#ExpU">函数编写指南</a><br>
  **可用变量:**
  - **睡眠**: `SleepDuration` (睡眠时长(tick),按照游戏内时间而非睡眠经历的绝对tick [↗ "24小时制"下的Minecraft-Minecraft Wiki](https://zh.minecraft.wiki/w/%E6%98%BC%E5%A4%9C%E6%9B%B4%E6%9B%BF#24%E5%B0%8F%E6%97%B6%E5%88%B6%E4%B8%8B%E7%9A%84Minecraft%E6%97%A5))
  - **状态**: `FoodLevel` (当前饥饿值), `Saturation` (饱食度), `Exhaustion` (消耗度) [↗ 饥饿机制详解-Minecraft Wiki](https://zh.minecraft.wiki/w/%E9%A5%A5%E9%A5%BF#%E6%9C%BA%E5%88%B6)<br>

</details>

<details><summary>MinFoodLevel | 饥饿衰减下限</summary>

- 类型: int
- 默认值: 1
- 说明: 本mod的饥饿衰减(包括自然饥饿衰减和睡眠饥饿衰减)所能扣除到的最小值,若小于该值则不再扣除,若扣除后小于该值则扣除至该值,否则按照函数默认扣除

</details>

<h4 id="ExpU">函数使用指南:</h4>

> <details><summary>函数编写指南</summary>
> 
> - 你可以使用`e`和`pi`代表`e`和`π`
> 
> **运算**
> 
> - 普通四则运算（加+减-乘*除/），用括号表示的优先级(例如 `( 1 + 2 ) * 3`)
> - 幂运算 `pow(底数, 指数)`
> - 约数：
>   - 向下取整数:`floor(num)`
>   - 四舍五入数:`round(num)`
>   - 向上取整数:`ceil(num)`
> - 取大小值
>   - 取大值:`max(num1, num2)`
>   - 取小值:`min(num1, num2)`
> - 对数:`log(底数, 真数)`
> 
> **逻辑**
> 
> - 判断:`if(condition, A, B)` ,如果 condition > 0 返回 A ,否则返回 B
> - 逻辑与:`and(A, B)` ,两者都大于 0 返回 1 ,否则返回 0
> - 逻辑或:`or(A, B)` ,A或B任意一个超过 0 返回 1 ,否则返回 0
> - 逻辑非:`not(A)` ,大于 0 返回 0 ,否则返回 1
> 
> **比较逻辑**
> 
> - 大于:`gt(A, B)` ,如果 A > B ,返回 1 ,否则返回 0
> - 小于:`lt(A, B)` ,如果 A < B ,返回 1 ,否则返回 0
> - 等于:`eq(A, B)` ,如果 A = B ,返回 1 ,否则返回 0
> - 不等于:`neq(A, B)` ,如果A ≠ B ,返回 1 ,否则返回 0 
> </details>

### 客户端配置

这些配置存放于 ``/config/spice-of-life-fabric-flavor-client.json``。
这些配置仅影响本地客户端显示，服务端的该配置无效。

<details>
<summary>ShowConsumedTooltips | 显示已发现食物tooltip</summary>

- 类型: boolean
- 默认值: true
- 说明: 控制客户端的tooltip显示。

</details>

<details>
<summary>ShowUnconsumedTooltips | 显示未发现食物tooltip</summary>

- 类型: boolean
- 默认值: true
- 说明: 控制客户端的tooltip显示。

</details>

## 关于命令

<details>
<summary>CleanHealth | 重置玩家数据</summary>

```
/sol2f clearhealthy
```

重置当前玩家的生命值上限与所有食物摄入记录，需管理员权限。（版本：2.0+）

</details>
<details>
<summary>GetPlayerData | 获取玩已发现列表</summary>

```
/sol2f getlist <player>
```

获取玩家已发现食物列表。

</details>
<details>
<summary>Sync | 同步数据</summary>

```
/sol2f sync
```

同步所有数据。

- ``/sol2f sync PlayerData``同步玩家数据。主要用于GUI显示和tooltip显示控制。
- ``/sol2f sunc AllFoodList``同步食物数据：最新的非黑名单食物数据，玩家最大血量上限数据。主要用于GUI显示和tooltip显示控制。

</details>

## 关于食物簿

在修改配置后请使用命令同步数据以确保GUI显示的数据为最新数据。

<details>
<summary>食物簿页面</summary>

![GUI_foodbook](https://cdn.modrinth.com/data/cached_images/4f93d1c6f1f49cb83cca3954847041212fe58945.png)

- 使用鼠标滚轮翻页。

</details>

<details>
<summary>概述页面</summary>

![GUI_overview](https://cdn.modrinth.com/data/cached_images/c505c1e662cf59069f2e590b0e76631e83aaa22a.png)

- 血量概述：当前获得的mod增益 / 最大可获得的mod增益。

</details>

<details>
<summary>按键绑定
</summary>

打开GUI的快捷键在minecraft的按键绑定中配置
![GUI_keybinding](https://cdn.modrinth.com/data/cached_images/4c0430cadb76f90f51a83419395ebc981ac96220.png)

</details>

## 物品

<details>
<summary>食物簿</summary>

通过合成"食物簿"打开食物簿:
![food book](https://cdn.modrinth.com/data/cached_images/eb67b04a5cd10951c497e5d90ac9703d7a2415f7.png)

</details>

## 常见问题和许可声明

### 总则

本mod（Spice of Life: Fabric Flavor）默认状态下由作者保留所有权力。
任何用户拥有自由的获取和使用mod的权利，任何机构或个人对mod的权利由以下细则约束。

**关于在整合包和对外分发的Minecraft游戏（以下简称游戏）版本或基于游戏作品的二次创作（以下统称为整合包）、以及在公开的服务器或在线游戏（以下统称在线游戏）中引用本mod**

本mod被允许在任何公开或私人分发的整合包、在线游戏中被引用。

- 禁止使用整合包盈利或损害用户权利；
- 不允许利用本mod进行任何直接或间接的商业化行为，包括但不限于：整合包或在线游戏客户端下载前需要打赏赞助、整合包或在线游戏客户端限制下载；
- 不允许对未打赏或赞助或付费的用户限制玩法或用户权限；
- 对于对外分发的整合包：必须提供**直接**且**无限制**的下载通道。

**关于分发本mod和二次创作分发**

分发是被允许的。二次创作中，mod的元数据和代码收到保护。

- 分发任何原始版本或二次创作不允许付费下载、限制下载；
- 任何分发原始版本和分发二次创作必须提供**直接**且**无限制**的下载通道，请标注modrinth页面链接。

**关于二次创作（即基于本mod的任何版本进行任何形式的修改）**

任何修改基于不修改mod元数据。

- 如有需要，mod的本地化文件允许修改，但不允许在中文和英文的翻译中进行署名（例如“由xxx翻译”“由xxx优化”），其他语言的翻译允许社区贡献和署名。
- 二次创作不允许修改mod元数据，你可以为mod增加额外本地化或修改GUI材质，为mod创作的额外材质包由其作者对所有内容负责，材质包作者对其材质包关于本mod的修改部分进行任何创作和本mod无关。

*限制的定义：包括但不限于下载前看广告、下载需要积分或代币、下载需要登陆或用户分级和等级等。

## 依赖

- [Mod Menu](https://modrinth.com/mod/modmenu) (v2.2.0+ 可选)配置页面入口
- [Cloth Config](https://modrinth.com/project/9s6osm5g) API (v2.2.0+ 必装)
- Fabric API (必装)

---

### 切换语言：

<a href="#CN">中文</a>

---

<H1 id="EN">Overview</H1>

This mod aims to incentivize players to try new foods by rewarding them with increased maximum health, thereby strengthening engagement with the **mod pack**'s food system and balancing gameplay difficulty.

# Mod Usage Guide

The configuration and GUI sections of this guide are maintained for the latest version (**3.2.0**). For details on older versions, please refer to the in-game configuration prompts.

## About Configuration

All health-related values are expressed in **health points**, where **2 health = 1 heart (❤)**.

### Server-Side Configuration

These settings are stored in ``/config/spice-of-life-fabric-flavor.json``.
These configurations take effect on the server. In a single-player local game, they affect the local save file.

#### Health Gain System

<details><summary>maxHealthy | Maximum Health Bonus Cap</summary>

- **Type**: int
- **Default**: 5000
- **Range**: 2~5000
- **Description**: Sets the upper limit for health bonuses granted *specifically* by this mod.
  - Formula: `Player Max Health = Base Health + Other Mod Bonuses + This Mod's Bonus`.
  - This option strictly caps the "This Mod's Bonus" term to prevent numerical inflation.

</details>

<details><summary>healthyGain | Health Gain per New Food</summary>

- **Type**: int
- **Default**: 2
- **Range**: 0~20
- **Description**: The amount of health points awarded for each unique food consumed. Setting this to 0 disables the base gain.

</details>

<details><summary>resetOnDeath | Reset on Death</summary>

- **Type**: boolean
- **Default**: false
- **Description**: If enabled, the player's health bonus resets to default, and all food consumption records are deleted upon death.

</details>

<details><summary>healthToMaxOnIncrease | Full Heal on Health Increase</summary>

- **Type**: boolean
- **Default**: false
- **Description**: If enabled, restores the player's current health to the new maximum whenever a health increase occurs.

</details>

<details><summary>healthIncreaseOnIncrease | Heal Amount on Health Increase</summary>

- **Type**: int
- **Default**: 0
- **Range**: Any positive integer
- **Description**: Restores a specific amount of health whenever a health increase occurs. If the heal amount exceeds the new max health, it caps at the max.

</details>

<details><summary>Increasefrequency | Special Reward Frequency</summary>

- **Type**: int
- **Default**: 0
- **Range**: 0~20 (0 disables this feature)
- **Description**: Defines the frequency threshold (**X**) for special rewards. The player gains an extra bonus only after consuming every **X** unique foods.

</details>

<details><summary>frequencyGain | Special Reward Amount</summary>

- **Type**: int
- **Default**: 2
- **Range**: 1~30
- **Description**: Defines the health amount (**N**) awarded when the frequency threshold (`Increasefrequency`) is reached.

</details>

<details><summary>Expression | Custom Health Calculation Expression</summary>

- **Type**: string
- **Default**: "0"
- **Description**: A custom formula for calculating health gain. See the <a href="#ExpU">Function Usage Guide</a> below for syntax.
- **Available Variable**: `uniqueFoods` (Number of unique foods consumed).
- **Note**: If set to "0", this feature is disabled. The result is added to the base gain.

</details>

<details><summary>blacklist | Food Blacklist</summary>

- **Type**: List
- **Default**:
```json
[
  "minecraft:rotten_flesh",
  "minecraft:spider_eye"
]
```
- **Description**: Foods listed here will **not** be recorded or counted towards stats.
- **Synchronization Mechanism ("Game Sync" Philosophy)**:
  - **Existing Data**: Foods already consumed *before* adding them to the blacklist remain recorded (we do not break the fourth wall by retroactively changing history).
  - **New Data**: Undiscovered foods on the blacklist will no longer count towards statistics.
  - **Client Tooltip**: Tooltips show all *discovered* foods. Undiscovered foods on the blacklist will not show tooltips.
  - **Action Required**: After modifying the blacklist, you **must** run `/sol2f sync AllFoodList` to synchronize data. This ensures the client GUI and tooltips reflect the latest non-blacklisted list correctly.
  - **GUI Display**: The overview shows `Discovered Foods / Non-Blacklisted Foods`.

</details>

> **Note on Health Recalculation**: After modifying health configurations, the new values are applied when the player eats a *new* undiscovered food or respawns (if "Reset on Death" is disabled). This adheres to the "Game Sync" concept: time does not pass in the mod logic if the game world hasn't progressed.

#### Hunger Decay System

<details><summary>EnableNaturalHunger | Enable Natural Hunger</summary>

- **Type**: boolean
- **Default**: false
- **Description**: If enabled, players lose hunger value over time based on the configured period and function.

</details>

<details><summary>NaturalHungerPeriod | Natural Hunger Period</summary>

- **Type**: int
- **Default**: 1200
- **Description**: The interval in **game ticks** (20 ticks = 1 second). Every time this period is reached, hunger decays according to the <a href="#NaturalHungerExpression">NaturalHungerExpression</a>.

</details>

<details id="NaturalHungerExpression"><summary>NaturalHungerExpression | Natural Hunger Decay Function</summary>

- **Type**: string
- **Default**: "1"
- **Description**: The function used to calculate hunger loss per period. The result is *subtracted* from the player's food level.
- **Syntax**: See <a href="#ExpU">Function Usage Guide</a>.
- **Available Variables**:
  - **Actions**: `isSprinting`, `isSwimming`, `isSneaking`, `isCrawling`
  - **Environment**: `isTouchingWater`, `isBeingRainedOn`, `isWet` (Water/Rain/Bubble Column), `BrightnessAtEyes` (Render brightness at eye level, 0.0-1.0)
  - **Status**: `FoodLevel`, `Saturation`, `Exhaustion`
  - *Note: Boolean variables act as `1.0` (true) or `0.0` (false) in formulas.*
- **Examples**:
  1.  **Base Fixed Cost**: Lose 0.5 hunger per period.
      ```json
      "NaturalHungerExpression": "0.5"
      ```
  2.  **Dynamic Environmental Cost**:
      Sprinting/Swimming: -4, Raining/In Water: -2, Others: -1.
      ```json
      "NaturalHungerExpression": "if(or(isSprinting, isSwimming), 4, if(or(isBeingRainedOn, isTouchingWater), 2, 1))"
      ```
  3.  **Darkness Survival Challenge**: Double consumption in darkness (`BrightnessAtEyes` < 0.1).
      ```json
      "NaturalHungerExpression": "if(lt(BrightnessAtEyes, 0.1), 2, 1)"
      ```

</details>

<details><summary>EnableSleepHunger | Enable Sleep Hunger</summary>

- **Type**: boolean
- **Default**: false
- **Description**: If enabled, players lose hunger upon waking up based on the <a href="#SleepHungerExpression">SleepHungerExpression</a>.

</details>

<details id="SleepHungerExpression"><summary>SleepHungerExpression | Sleep Hunger Decay Function</summary>

- **Type**: string
- **Default**: "SleepDuration / 3000"
- **Description**: Calculates hunger loss upon waking. The result is *subtracted* from the food level.
- **Syntax**: See <a href="#ExpU">Function Usage Guide</a>.
- **Available Variables**:
  - **Sleep**: `SleepDuration` (Duration in ticks, based on in-game time passed, not absolute real-time ticks).
  - **Status**: `FoodLevel`, `Saturation`, `Exhaustion`.

</details>

<details><summary>MinFoodLevel | Hunger Decay Floor</summary>

- **Type**: int
- **Default**: 1
- **Description**: The minimum food level allowed after decay (from both Natural and Sleep hunger). If decay would drop the player below this value, it stops at this value.

</details>

#### <span id="ExpU">*Function Usage Guide*</span>

> <details><summary>Click to expand Function Syntax</summary>
>
> - You can use `e` and `pi` to represent Euler's number and π.
>
> **Arithmetic Operations**
>
> - Basic: Addition (+), Subtraction (-), Multiplication (*), Division (/). Use parentheses `()` for precedence (e.g., `(1 + 2) * 3`).
> - Power: `pow(base, exponent)`
> - Rounding:
>   - Floor: `floor(num)`
>   - Round Nearest: `round(num)`
>   - Ceiling: `ceil(num)`
> - Min/Max:
>   - Max: `max(num1, num2)`
>   - Min: `min(num1, num2)`
> - Logarithm: `log(base, argument)`
>
> **Logic**
>
> - Conditional: `if(condition, A, B)` -> Returns A if condition > 0, else B.
> - AND: `and(A, B)` -> Returns 1 if both > 0, else 0.
> - OR: `or(A, B)` -> Returns 1 if either > 0, else 0.
> - NOT: `not(A)` -> Returns 0 if A > 0, else 1.
>
> **Comparisons**
>
> - Greater Than: `gt(A, B)` -> Returns 1 if A > B, else 0.
> - Less Than: `lt(A, B)` -> Returns 1 if A < B, else 0.
> - Equal: `eq(A, B)` -> Returns 1 if A = B, else 0.
> - Not Equal: `neq(A, B)` -> Returns 1 if A ≠ B, else 0.
> </details>

### Client-Side Configuration

These settings are stored in ``/config/spice-of-life-fabric-flavor-client.json``.
These configurations only affect the local client display. Server-side settings do not override these visual preferences.

<details>
<summary>ShowConsumedTooltips | Show Consumed Food Tooltip</summary>

```json
"ShowConsumedTooltips": true,
```
Controls whether the tooltip indicator for "Consumed" foods is displayed on the client.

</details>

<details>
<summary>ShowUnconsumedTooltips | Show Unconsumed Food Tooltip</summary>

```json
"ShowUnconsumedTooltips": true,
```
Controls whether the tooltip indicator for "Not yet consumed" foods is displayed on the client.

</details>

## About Commands

<details>
<summary>CleanHealth | Reset Player Data</summary>

```bash
/sol2f clearhealthy
```
Resets the current player's maximum health bonus and all food consumption records. Requires operator permissions. (Version: 2.0+)

</details>

<details>
<summary>GetPlayerData | Get Consumed List</summary>

```bash
/sol2f getlist <player>
```
Retrieves the list of consumed foods for the specified `<player>`.

</details>

<details>
<summary>Sync | Synchronize Data</summary>

```bash
/sol2f sync
```
Synchronizes all data between server and client. **Crucial after config changes.**

- ``/sol2f sync PlayerData``: Synchronizes player-specific eaten food lists. Primarily updates GUI displays and tooltip states.
- ``/sol2f sync AllFoodList``: Synchronizes global food data (latest non-blacklisted list and theoretical max health cap). Used to update GUI overviews and ensure tooltips behave correctly after blacklist changes.

</details>

## About the Food Book

在修改配置后请使用命令同步数据以确保GUI显示的数据为最新数据。
After modifying configurations, please use the sync commands to ensure the GUI displays the latest data.

<details>
<summary>Food Book Page</summary>

![GUI_foodbook](https://cdn.modrinth.com/data/cached_images/4f93d1c6f1f49cb83cca3954847041212fe58945.png)

- Displays a grid of all discovered foods.
- Use the mouse scroll wheel or the slider on the right to flip pages.

</details>

<details>
<summary>Overview Page</summary>

![GUI_overview](https://cdn.modrinth.com/data/cached_images/c505c1e662cf59069f2e590b0e76631e83aaa22a.png)

- **Health Summary**: Displays `Current Mod Bonus / Maximum Possible Mod Bonus`.
- Click the book icon in the corner to switch between Overview and Food Book.

</details>

<details>
<summary>Key Bindings</summary>

The shortcut key to open the GUI can be configured in Minecraft's **Controls** menu under the "Spice of Life Fabric Flavor" category.
![GUI_keybinding](https://cdn.modrinth.com/data/cached_images/4c0430cadb76f90f51a83419395ebc981ac96220.png)

</details>

## Items

<details>
<summary>Food Book</summary>

Craft the **"Food Book"** to open the food journal interface:
![food book](https://cdn.modrinth.com/data/cached_images/eb67b04a5cd10951c497e5d90ac9703d7a2415f7.png)

</details>

## Common Questions & Licensing

### General Principles

This mod (**Spice of Life: Fabric Flavor**) reserves all rights by default.
Users have the freedom to obtain and use the mod. Rights for institutions or individuals are constrained by the following rules.

**Regarding Use in Mod Packs and Public Servers**

This mod is allowed to be included in any publicly or privately distributed mod packs and online games.

- **No Profit from Mod**: Prohibited from profiting directly from the mod or infringing user rights.
- **No Paywalls**: No direct or indirect commercialization is allowed. This includes requiring donations/sponsorships to download the mod pack or restricting downloads.
- **No Gameplay Restrictions**: Cannot restrict gameplay features or permissions for users who have not donated/paid.
- **Direct Access**: For distributed mod packs, a **direct** and **unrestricted** download channel must be provided.

**Regarding Distribution and Derivative Works**

Distribution is allowed. Metadata and code in derivative works are protected.

- **No Paid Downloads**: Distributing original or modified versions cannot require payment or restrict downloads.
- **Attribution**: Any distribution must provide a **direct** and **unrestricted** download channel and include a link to the Modrinth page.

**Regarding Derivative Works (Modifications)**

Any modification must not alter the mod's core metadata.

- **Localization**: Localization files may be modified. However, attribution (e.g., "Translated by XXX") is **not allowed** in the Chinese and English translation files. Attribution is allowed for other community-contributed languages.
- **Assets**: You may add extra localization or modify GUI textures. Extra texture packs created for this mod are the sole responsibility of their authors; such modifications are considered separate from the core mod.

*Definition of Restrictions: Includes but is not limited to watching ads before download, requiring points/tokens, login requirements, or user tier restrictions.*

## Dependencies

- [Mod Menu](https://modrinth.com/mod/modmenu) (v2.2.0+, Optional) – Entry point for the configuration screen.
- [Cloth Config API](https://modrinth.com/project/9s6osm5g) (v2.2.0+, Required) – Required for configuration handling.
- Fabric API (Required)
