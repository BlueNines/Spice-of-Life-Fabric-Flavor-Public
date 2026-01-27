Language/语言: 
<a href="#CN">中文</a>
<a href="#EN">English</a>
<H1 id="CN">概述</H1>
一个简单的fabric1.20.1 _生活调味料：胡萝卜版_ 部分功能移植mod

本mod旨在通过增加血量上限奖励玩家食用新的食物，以加强玩家对整合包食物系统的动力，并平衡整合包的游戏难度。
## 配置文件
该指南为最新版本维护，请尽量使用新版的MOD
所有配置项以health计算，2health=1heart（红心）

<details>
<summary>maxHealthy</summary>

```
"maxHealthy": 120,
```
配置最大血量上限
版本：0.1.1+
</details>
<details>
<summary>defaultHealthy(废弃)</summary>

```
"defaultHealthy": 120,
```
初始血量上限
版本：0.1.1+
</details>
<details>
<summary>healthGain</summary>

```
"healthGain": 2,
```
每次奖励血量数
版本：0.1.1+
</details>
<details>
<summary>resetOnDeath</summary>
  
```
resetOnDeath: false,
```
死亡后是否重置生命值上限与所有食物记录。
版本：2.0+
</details>
<details>
<summary>developerMode</summary>

```
"developerMode": false,
```
开发者模式，输出log记录玩家变更记录，建议不持续开启。
版本：2.1.0+
</details>
<details>
<summary>healthToMaxOnIncrease</summary>

```
"healthToMaxOnIncrease": false
```
是否在增加生命上限时恢复满血。
版本：2.1.0+
</details>
<details>
<summary>Increasefrequency</summary>
食用X种食物而增加N点血量，该配置项作为X（与前面的healthyGain设置共存）
版本：2.3.0+
  
```
"Increasefrequency": 4,
```

</details>

<details>
<summary>frequencyGain</summary>
食用X种食物后增加N点血量，该配置项作为N（与前面的healthyGain设置共存）
版本：2.3.0+
 
```
"frequencyGain": 4,
```

</details>


<details>
<summary>healthIncreaseOnIncrease</summary>
每次血量上限增益时回复的血量，若超过上限则回复至上限

```
"healthIncreaseOnIncrease": 2,
```

</details>

<details>
<summary>Expression</summary>
字符串类型，增益计算函数，与旧版本设置并存，详细说明见下文（2.5.0+）
  
```
"Expression": 0,//(String)
```

</details>

### **函数使用文档：**

**你可以在函数中使用这些变量：**

- `uniqueFoods`食用过的食物数
- `currentHealth`当前血量（不包含本mod添加的血量，包含别的mod添加的血量和基础血量）

**你可以使用这些运算：**
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
- 你可以使用`e`和`pi`代表`e`和`π`


## 命令

- ``/sol2f clearhealthy``重置当前玩家的生命值上限与所有食物摄入记录，需管理员权限。（版本：2.0+）
- ``/sol2f getlist <player>``获取<player>的已食用列表。 （版本：2.7.0+）


## GUI
- **食物簿GUI** 在2.4.0+（beta2+）可用
![GUI_foodbook](https://cdn.modrinth.com/data/cached_images/4f93d1c6f1f49cb83cca3954847041212fe58945.png  )
- **概述GUI** 在2.4.0+（beta3+）可用
![GUI_overview](https://cdn.modrinth.com/data/cached_images/c505c1e662cf59069f2e590b0e76631e83aaa22a.png  )
- **按键绑定** 打开GUI的快捷键在minecraft的按键绑定中配置
![GUI_keybinding](https://cdn.modrinth.com/data/cached_images/4c0430cadb76f90f51a83419395ebc981ac96220.png  )

通过合成"食物簿"打开食物簿:
![food book](https://cdn.modrinth.com/data/cached_images/eb67b04a5cd10951c497e5d90ac9703d7a2415f7.png)

## Q＆A
**Q** 是否允许将此mod添加到整合包？<br>
**A** 是的！该mod为平衡整合包设计，但是请不要使用整合包盈利或修改mod信息。

**Q** 是否允许将此mod上传至别的平台？<br>
**A** 这是被允许的。但是不允许付费下载、限制下载（包括但不限于下载前看广告、下载需要积分或代币、下载需要登陆或用户分级和等级等任何不能直接且无限制的下载通道），请标注modrinth页面链接且不要修改mod信息。除非您有高带宽的CDN，否则建议不要搬运mod文件提供下载服务。

##  依赖项
- <a herf="https://modrinth.com/mod/modmenu  ">Mod Menu</a> (v2.2.0+ 可选)配置页面入口
- <a herf="https://modrinth.com/project/9s6osm5g  ">Cloth Config API</a> (v2.2.0+ 必装)

---
Language/语言: 
<a href="#CN">中文</a>
<a href="#EN">English</a>

<H1 id="EN">Engilsh</H1>
  
**Translation is completed by artificial intelligence, AI-generated content may be inaccurate. For accurate information, please refer to the original Chinese text.**

## Overview  
A Fabric mod that partially ports features from *Life Seasoning: Carrot Edition*.  

This mod aims to incentivize players to try new foods by rewarding them with increased maximum health, thereby strengthening engagement with the modpack’s food system and balancing gameplay difficulty.

## Configuration File  
This guide is maintained for the latest version. Please use the newest version of the mod whenever possible.  
All health-related values are expressed in **health**, where **2 health = 1 heart (❤)**.

<details>
<summary>maxHealthy</summary>

```
"maxHealthy": 120,
```
Sets the maximum allowable health limit.  
Version: 0.1.1+
</details>

<details>
<summary>defaultHealthy (deprecated)</summary>

```
"defaultHealthy": 120,
```
Initial maximum health.  
Version: 0.1.1+
</details>

<details>
<summary>healthGain</summary>

```
"healthGain": 2,
```
Health units awarded per qualifying food consumption.  
Version: 0.1.1+
</details>

<details>
<summary>resetOnDeath</summary>

```
resetOnDeath: false,
```
Whether to reset both maximum health and all food records upon player death.  
Version: 2.0+
</details>

<details>
<summary>developerMode</summary>

```
"developerMode": false,
```
Enables developer mode, which logs player health and food record changes. Not recommended for continuous use.  
Version: 2.1.0+
</details>

<details>
<summary>healthToMaxOnIncrease</summary>

```
"healthToMaxOnIncrease": false
```
Whether to fully heal the player when their maximum health increases.  
Version: 2.1.0+
</details>

<details>
<summary>Increasefrequency</summary>
Controls how many unique foods must be eaten before granting extra health, working alongside `healthyGain`. This value represents **X** in “gain N health after eating X foods.”  
Version: 2.3.0+

```
"Increasefrequency": 4,
```
</details>

<details>
<summary>frequencyGain</summary>
The amount of health (**N**) granted after eating **X** unique foods (as defined by `Increasefrequency`). Works together with `healthyGain`.  
Version: 2.3.0+

```
"frequencyGain": 4,
```
</details>

<details>
<summary>healthIncreaseOnIncrease</summary>
Amount of health restored when maximum health is increased. If the result exceeds the new maximum, health is capped at the new limit.

```
"healthIncreaseOnIncrease": 2,
```
</details>

<details>
<summary>Expression</summary>
A string-type custom gain calculation function. Coexists with legacy settings. See documentation below (2.5.0+).

```
"Expression": "0", // (String)
```
</details>

### **Function Usage Documentation**

**Available variables in the expression:**

- `uniqueFoods`: number of unique foods consumed  
- `currentHealth`: current health value (excluding health added by this mod, but including base health and health from other mods)

**Supported operations:**

- Basic arithmetic: `+`, `-`, `*`, `/`; parentheses for precedence (e.g., `(1 + 2) * 3`)  
- Power: `pow(base, exponent)`  
- Rounding:  
  - `floor(num)` (round down)  
  - `ceil(num)` (round up)  
  - `round(num)` (round to nearest integer)  
- Min/Max:  
  - `min(num1, num2)`  
  - `max(num1, num2)`  
- Logarithm: `log(base, argument)`  
- Constants: `e` and `pi` represent Euler’s number and π, respectively

## Commands

- ``/sol2f clearhealthy``Resets the current player’s maximum health and all food consumption records. Requires operator permissions.  (Version: 2.0+)
- ``/sol2f getlist <player>``get eaten list of <player>.  (Version: 2.7.0+)


## GUI
- **Food Journal GUI** available from 2.4.0+ (beta2+)  
![GUI_foodbook](https://cdn.modrinth.com/data/cached_images/4f93d1c6f1f49cb83cca3954847041212fe58945.png)  
- **Overview GUI** available from 2.4.0+ (beta3+)  
![GUI_overview](https://cdn.modrinth.com/data/cached_images/c505c1e662cf59069f2e590b0e76631e83aaa22a.png)  
- **Key Bindings**: Shortcut keys to open GUIs are configurable in Minecraft’s controls menu  
![GUI_keybinding](https://cdn.modrinth.com/data/cached_images/4c0430cadb76f90f51a83419395ebc981ac96220.png)

## Q&A
**Q:** Can I include this mod in a modpack?  
**A:** Yes! This mod is specifically designed for modpack balance. However, you may **not** profit from the modpack or alter the mod’s information.

**Q:** Can I re-upload this mod to other platforms?  
**A:** Yes, redistribution is allowed, **provided that**:  
- Downloads are **free and unrestricted** (no ads, login, tokens, points, level requirements, etc.)  
- The original Modrinth page link is clearly attributed  
- The mod’s contents and metadata are **not modified**  
- Unless you operate a high-bandwidth CDN, please **do not host the mod file yourself** for download

## Dependencies
- <a href="https://modrinth.com/mod/modmenu">Mod Menu</a> (v2.2.0+, optional) – for config screen access  
- <a href="https://modrinth.com/project/9s6osm5g">Cloth Config API</a> (v2.2.0+, required) – for configuration handling
