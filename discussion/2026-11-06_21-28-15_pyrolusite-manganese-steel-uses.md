# 软锰矿、锰与锰钢用途

- Time: `2026-11-06 21:28:15 +08:00`
- Author: 用户（用途选择）；OpenAI Codex（整理与源码核对）
- Related: [冰洞第二版方案](../plans/2026-10-22_01-04-46_ice-caves-landscape-revision.md)

## 用户暂定的用途

1. **高耐久工具／武器部件材料**：锰钢相比普通钢提高耐久，挖掘效率、攻击性能不提高。具体倍率、部件范围及修理成本未定；之前提出的耐久 +30%～50% 仅为候选数值。
2. **破碎机升级部件**：用途方向暂定，目标机器、升级方式和收益未定。不能仅因矿物耐磨就假设现有机器具备磨损或升级槽。
3. **一次性电池原料**：二氧化锰路线保留，但暂不添加电池。用户希望等以后出现耗电较低、便携性要求较高的设备后再做；本次不据此确定电池电量或 FE 换算。

没有把此前讨论的可选炼钢增益、通用脱氧剂配方确认为本轮用途。软锰矿到粉碎矿、洗净矿、粉、锰铁／锰钢的加工链仍需另行设计。本记录不授权实现、提交或部署。

## 本轮核对：整合包已有破碎设备

- TWR `config/fhresearches/crusher.json` 的研究效果解锁 `immersiveengineering:multiblocks/crusher`，父研究为 `heavy_engineering`；`kubejs/server_scripts/src/recipes_types/immersiveengineering/crusher.js` 与 `recipes_packaged/crush.js` 添加 IE 破碎机配方。
- TWR `config/fhresearches/crushing_wheel.json` 解锁 `create:crushing_wheel`，`kubejs/server_scripts/src/recipes_types/create/mechanical_crafting.js` 有粉碎轮制作配方。
- 因此当前仓库数据已包含 **IE 多方块破碎机与 Create 粉碎轮**；本轮未启动游戏验证实际研究解锁和设备交互。
- 在 FH 源码及 TWR 脚本、配置中未检索到匹配的破碎机升级对接。未完整审查依赖模组实现，不能据此断言不存在任何扩展接口。下一步选定目标机器后核实，升级部件尚不是可直接追加配方就能启用的已知功能。
