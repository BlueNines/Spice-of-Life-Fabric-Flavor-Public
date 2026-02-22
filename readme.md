---

### Switch Language:

<a href="#EN">English</a>

---

<H1 id="CN">概述</H1>

fabric的 _生活调味料：胡萝卜版_ 部分功能移植mod

本mod旨在通过增加血量上限奖励玩家食用新的食物，以加强玩家对整合包食物系统的动力，并平衡整合包的游戏难度。

# mod~~食用~~指南

该指南的config和GUI说明部分为最新版本（3.1.0）维护，其他版本呢详情请参考游戏内配置页面提示。

## 关于配置

所有配置项以health计算，2health=1heart（红心）

### 服务端配置
这些配置存放于```/config/spice-of-life-fabric-flavor.json```。
这些配置在服务端生效，本地游戏版本的配置影响本地游戏存档。

<details>
<summary>maxHealthy | 血量增益上限</summary>

```
"maxHealthy": 5000,
```

配置本mod的增益上限，无关玩家默认血量和其他mod的修改，仅影响本mod的增益上限。
玩家血量=初始血量+其他mod的修改（若存在其他mod修改）+本mod增益。该选项控制最后一项。

</details>
<details>
<summary>healthGain | 单次生命值奖励幅度</summary>

```
"healthGain": 2,
```
每次奖励血量数
</details>
<details>
<summary>resetOnDeath | 死亡时重置</summary>

```
resetOnDeath: false,
```

死亡后是否重置生命值上限与所有食物记录。
</details>
<details>
<summary>developerMode | 调试模式</summary>

```
"developerMode": false,
```
开发者模式，输出log记录玩家变更记录，建议不开启。
</details>
<details>
<summary>healthToMaxOnIncrease | 奖励时将生命增益至上限</summary>

```
"healthToMaxOnIncrease": false
```
是否在增加生命上限时恢复满血。

</details>
<details>
<summary>Increasefrequency | 自定义奖励进度频率</summary>
  
```
"Increasefrequency": 4,
```

食用X种食物而增加N点血量，该配置项作为X（区别于与前面的healthyGain，且共存）

</details>

<details>
<summary>frequencyGain | 自定义奖进度幅度</summary>

```
"frequencyGain": 4,
```

食用X种食物后增加N点血量，该配置项作为N（区别于与前面的healthyGain，且共存）

</details>


<details>
<summary>healthIncreaseOnIncrease | 奖励时增益的血量</summary>

```
"healthIncreaseOnIncrease": 2,
```

每次血量上限增益时回复的血量，若超过上限则回复至上限

</details>

<details>
<summary>Expression | 自定义奖励计算表达式</summary>
  
```
"Expression": 0,
```

字符串类型，增益计算函数，与旧版本设置并存。

**函数使用文档：**

- `uniqueFoods`食用过的食物数
- 你可以使用`e`和`pi`代表`e`和`π`

运算：
- 普通四则运算（加+减-乘*除/），用括号表示的优先级（例如( 1 + 2 ) * 3）
- 幂运算 `pow(底数, 指数)`
- 约数：
  - 向下取整数:`floor(num)`
  - 四舍五入数:`round(num)`
  - 向上取整数:`ceil(num)`
- 比较大小
  - 取大值:`max(num1, num2)`
  - 取小值:`min(num1, num2)`
- 对数:`log(底数, 真数)`

</details>

<details>
<summary>Expression | 黑名单食物</summary>
  
```
"blacklist": [
  "minecraft:rotten_flesh",
  "minecraft:spider_eye"
]
```

列表类型。列表中的食物不会被记录，已经食用的物品不会从统计中移除。在修改后，使用/sol2f sync AllFoodList 同步数据以确保tooltip显示正常，已经食用的食物tooltip仍然显示。

每行输入一个物品ID，例如：minecraft:golden_apple

</details>

### 客户端配置
这些配置存放于```/config/spice-of-life-fabric-flavor-client.json```。
这些配置仅影响本地客户端显示，服务端的该配置无效。

<details>
<summary>ShowConsumedTooltips | 显示已发现食物tooltip</summary>
  
```
"ShowConsumedTooltips": true,
```

控制客户端的tooltip显示。

</details>

<details>
<summary>ShowUnconsumedTooltips | 显示未发现食物tooltip</summary>
  
```
"ShowUnconsumedTooltips": true,
```

控制客户端的tooltip显示。

</details>

## 关于命令

- ``/sol2f clearhealthy``重置当前玩家的生命值上限与所有食物摄入记录，需管理员权限。（版本：2.0+）
- ``/sol2f getlist <player>``获取<player>的已食用列表。 （版本：2.7.0+）
- ``/sol2f sync``同步所有数据。（版本3.1.0+）
- ``/sol2f sync PlayerData``同步玩家数据。主要用于GUI显示和tooltip显示控制。（版本3.1.0+）
- ``/sol2f sunc AllFoodList``同步食物数据：最新的非黑名单食物数据，玩家最大血量上限数据。主要用于GUI显示和tooltip显示控制。（版本3.1.0+）


## 关于食物簿

在修改配置后请使用命令同步数据以确保GUI显示的数据为最新数据。


<details>
<summary>食物簿页面</summary>

![GUI_foodbook](https://cdn.modrinth.com/data/cached_images/4f93d1c6f1f49cb83cca3954847041212fe58945.png  )
- 使用鼠标滚轮翻页。

</details>


<details>
<summary>概述页面</summary>

![GUI_overview](https://cdn.modrinth.com/data/cached_images/c505c1e662cf59069f2e590b0e76631e83aaa22a.png  )
- 血量概述：当前获得的mod增益 / 最大可获得的mod增益。

</details>

<details>
<summary>按键绑定
</summary>
  
打开GUI的快捷键在minecraft的按键绑定中配置
![GUI_keybinding](https://cdn.modrinth.com/data/cached_images/4c0430cadb76f90f51a83419395ebc981ac96220.png  )

</details>


## 物品

通过合成"食物簿"打开食物簿:
![food book](https://cdn.modrinth.com/data/cached_images/eb67b04a5cd10951c497e5d90ac9703d7a2415f7.png)

## Q＆A
**Q** 是否允许将此mod添加到整合包？<br>
**A** 是的！该mod为平衡整合包设计，但是请不要使用整合包盈利（不允许下载前需要打赏赞助、不允许对未打赏或赞助或付费的用户限制下载或玩法、限制下载（包括但不限于下载前看广告、下载需要积分或代币、下载需要登陆或用户分级和等级等）请提供**直接**且**无限制**的下载通道）。

**Q** 是否允许将此mod上传至别的平台？<br>
**A** 这是被允许的。但是不允许付费下载、限制下载（包括但不限于下载前看广告、下载需要积分或代币、下载需要登陆或用户分级和等级等）请提供**直接**且**无限制**的下载通道，请标注modrinth页面链接。除非您有高带宽的CDN，否则建议不要搬运mod文件提供下载服务。

**Q** 针对任何形式的mod数据修改，那些操作被允许？<br>
**A** 如有需要，mod的本地化文件允许修改，但不允许在中文和英文的翻译中进行署名（例如“由xxx翻译”“由xxx优化”），其他语言的翻译允许社区贡献和署名。不允许修改mod元数据。

##  依赖
- <a herf="https://modrinth.com/mod/modmenu  ">Mod Menu</a> (v2.2.0+ 可选)配置页面入口
- <a herf="https://modrinth.com/project/9s6osm5g  ">Cloth Config API</a> (v2.2.0+ 必装)
- Fabric API （必装）

---

### 切换语言： 

<a href="#CN">中文</a>

---

<H1 id="EN">English</H1>
  
**Note:** This translation was generated with AI assistance. While efforts were made to ensure accuracy, please refer to the original Chinese text for the most precise technical details.

## Overview  
A Fabric mod that partially ports features from *Spice of Life: Carrot Edition* (Life Seasoning: Carrot Edition).  

This mod aims to incentivize players to try new foods by rewarding them with increased maximum health, thereby strengthening engagement with the **mod pack**'s food system and balancing gameplay difficulty.

# Mod Usage Guide

The configuration and GUI sections of this guide are maintained for the latest version (**3.1.0+**). For details on older versions, please refer to the in-game configuration prompts.

## About Configuration

All health-related values are expressed in **health points**, where **2 health = 1 heart (❤)**.

### Server-Side Configuration
These settings are stored in ```/config/spice-of-life-fabric-flavor.json```.  
These configurations take effect on the server. In a single-player local game, they affect the local save file.

<details>
<summary>maxHealthy | Maximum Health Bonus Cap</summary>

```json
"maxHealthy": 5000,
```

Sets the upper limit for health bonuses granted by this mod. This value is independent of the player's default health or modifications from other mods; it strictly caps the bonus provided by *this* mod.  
**Formula:** Player Max Health = Base Health + Other Mod Modifications (if any) + This Mod's Bonus. This option controls the final term.

</details>

<details>
<summary>healthyGain | Health Gain per New Food</summary>

```json
"healthyGain": 2,
```
The amount of health points awarded for each unique food consumed.
</details>

<details>
<summary>resetOnDeath | Reset on Death</summary>

```json
"resetOnDeath": false,
```

Whether to reset the maximum health bonus and all food consumption records upon player death.
</details>

<details>
<summary>developerMode | Debug Mode</summary>

```json
"developerMode": false,
```
Enables developer mode, which outputs detailed logs regarding player record changes. Recommended to be kept disabled for normal gameplay.
</details>

<details>
<summary>healthToMaxOnIncrease | Full Heal on Health Increase</summary>

```json
"healthToMaxOnIncrease": false
```
Whether to fully restore the player's current health whenever their maximum health increases.

</details>

<details>
<summary>Increasefrequency | Special Reward Frequency</summary>
  
```json
"Increasefrequency": 4,
```

Defines the frequency threshold (**X**) for special rewards. Instead of gaining health for every single new food, the player gains a bonus only after consuming **X** unique foods. This works alongside `healthyGain`.

</details>

<details>
<summary>frequencyGain | Special Reward Amount</summary>

```json
"frequencyGain": 4,
```

Defines the health amount (**N**) awarded when the frequency threshold (`Increasefrequency`) is reached. This works alongside `healthyGain`.

</details>


<details>
<summary>healthIncreaseOnIncrease | Heal Amount on Health Increase</summary>

```json
"healthIncreaseOnIncrease": 2,
```

The amount of health restored to the player whenever their maximum health increases. If the healing amount exceeds the new maximum, health is capped at the new limit.

</details>

<details>
<summary>Expression | Custom Health Calculation Expression</summary>
  
```json
"Expression": "0",
```

A string type field for a custom gain calculation function. This coexists with the standard settings above. If set to "0", this feature is disabled.

**Function Documentation:**

- `uniqueFoods`: The number of unique foods consumed.
- `currentHealth`: The current health value (excluding bonuses from this mod, but including base health and other mod modifications).
- You can use `e` and `pi` to represent Euler's number and π.

**Supported Operations:**
- Basic Arithmetic: Addition (+), Subtraction (-), Multiplication (*), Division (/). Use parentheses `()` for precedence (e.g., `(1 + 2) * 3`).
- Power: `pow(base, exponent)`
- Rounding:
  - Round Down: `floor(num)`
  - Round Nearest: `round(num)`
  - Round Up: `ceil(num)`
- Comparison:
  - Maximum: `max(num1, num2)`
  - Minimum: `min(num1, num2)`
- Logarithm: `log(base, argument)`

</details>

<details>
<summary>blacklist | Food Blacklist</summary>
  
```json
"blacklist": [
  "minecraft:rotten_flesh",
  "minecraft:spider_eye"
]
```

A list type configuration. Foods listed here will **not** be recorded in statistics. Note that foods already consumed before adding them to the blacklist will **not** be removed from existing statistics.  
**Important:** After modifying this list, use the command `/sol2f sync AllFoodList` to synchronize data. This ensures tooltips display correctly. Tooltips for already consumed foods will remain visible until re-evaluated.

Enter one Item ID per line. Example: `minecraft:golden_apple`

</details>

### Client-Side Configuration
These settings are stored in ```/config/spice-of-life-fabric-flavor-client.json```.  
These configurations only affect the local client display. Settings on the server do not override these client-side visual preferences.

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

- ``/sol2f clearhealthy`` Resets the current player's maximum health bonus and all food consumption records. Requires operator permissions. (Version: 2.0+)
- ``/sol2f getlist <player>`` Retrieves the list of consumed foods for the specified `<player>`. (Version: 2.7.0+)
- ``/sol2f sync`` Synchronizes all data (Player Data + Food List). (Version: 3.1.0+)
- ``/sol2f sync PlayerData`` Synchronizes player-specific data. Primarily used to update GUI displays and tooltip states. (Version: 3.1.0+)
- ``/sol2f sync AllFoodList`` Synchronizes food data: updates the list of non-blacklisted foods and the player's maximum health cap data. Primarily used to ensure GUI and tooltip displays are current after config changes. (Version: 3.1.0+)


## About the Food Book

After modifying configurations, please use the sync commands to ensure the GUI displays the latest data.

<details>
<summary>Food Book Page</summary>


Use the mouse scroll wheel to flip pages.
</details>

<details>
<summary>Overview Page</summary>


Health Summary: Displays (Current Mod Bonus / Maximum Possible Mod Bonus).
</details>

<details>
<summary>Key Bindings</summary>

The shortcut key to open the GUI can be configured in Minecraft's Controls menu.

</details>


## Items

Craft the **"Food Book"** to open the food journal interface:
![food book](https://cdn.modrinth.com/data/cached_images/eb67b04a5cd10951c497e5d90ac9703d7a2415f7.png)

## Q&A

**Q:** Am I allowed to include this mod in a **mod pack**?  
**A:** Yes! This mod is specifically designed to balance **mod packs**. However, you are **not** allowed to profit from the **mod pack** using this mod. This strictly includes:
- No requiring donations or sponsorships before downloading.
- No restricting downloads or gameplay features for users who have not donated, sponsored, or paid.
- No restrictive download conditions (including but not limited to: watching ads before download, requiring points/tokens, mandatory login, user tiers, or level requirements).  
You must provide a **direct** and **unrestricted** download channel.

**Q:** Am I allowed to re-upload this mod to other platforms?  
**A:** Yes, redistribution is permitted under the following conditions:
- Downloads must be **free** and **unrestricted**. Paid downloads or restrictive conditions (including but not limited to: watching ads before download, requiring points/tokens, mandatory login, user tiers, or level requirements) are **strictly prohibited**.
- You must provide a **direct** and **unrestricted** download channel.
- You must clearly attribute the original Modrinth page link.
- Unless you operate a high-bandwidth CDN, it is recommended **not** to host the mod file yourself to provide download services; linking to the original source is preferred.

**Q:** Regarding modifications to any form of mod data, what operations are permitted?  
**A:** If necessary, the mod's localization files may be modified. However:
- **No attribution** is allowed within the Chinese (`zh_cn`) or English (`en_us`) translation (e.g., adding lines like "Translated by xxx" or "Optimized by xxx").
- Attribution **is** permitted for translations in other languages contributed by the community.
- Modifying the mod's metadata (Including author, name, modID in file, and so on) is **strictly prohibited**.

## Dependencies
- <a href="https://modrinth.com/mod/modmenu">Mod Menu</a> (v2.2.0+, Optional) – Provides the entry point for the configuration screen.
- <a href="https://modrinth.com/project/9s6osm5g">Cloth Config API</a> (v2.2.0+, Required) – Required for configuration handling.
- Fabric API (Required)
