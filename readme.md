# 概述

一个简单的fabric1.20.1 _[生活调味料：胡萝卜版](https://www.curseforge.com/minecraft/mc-mods/spice-of-life-carrot-edition)_ 部分功能移植mod。本mod旨在通过增加血量上限奖励玩家食用新的食物，以加强玩家对整合包食物系统的动力，并平衡整合包的游戏难度。

## 配置文件

- 所有配置项按照血量计算，2血量=1心

<details>
<summary>maxHealthy</summary>

```
"maxHealthy": 120,
```

配置最大血量上限
版本：0.1.1+

</details>
<details>
<summary>defaultHealthy</summary>

```
"defaultHealthy": 120,
```

初始血量上限
版本：0.1.1+

</details>
<details>
<summary>healthyGain</summary>

```
"healthyGain": 2,
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
<summary>healToMaxOnIncrease</summary>

```
"healToMaxOnIncrease": false
```

是否在增加生命上限时恢复满血。
版本：2.1.0+

</details>

## 命令项

<details>
<summary>cleanhealthy</summary>

```
/sol2f clearhealthy
```

重置当前玩家的生命值上限与所有食物摄入记录，需管理员权限。
版本：2.0+

</details>

## Q＆A

**Q** 是否允许将此mod添加到整合包？
**A** 是的！该mod为平衡整合包设计，但是请不要使用整合包盈利或修改mod信息。

**Q** 是否允许将此mod上传至别的平台？
**A** 这是被允许的。但是不允许付费下载、限制下载（包括下载前看广告、下载需要积分类代币），请标注modrinth页面链接且不要修改mod信息。

## 依赖项

- [Mod Menu](https://modrinth.com/mod/modmenu) (v2.2.0+ 可选) 配置页面入口
- [Cloth Config API](https://modrinth.com/project/9s6osm5g) (v2.2.0+ 必装)

## 提示

- 2.0~2.2.0版本存在逻辑bug和重大错误隐患
