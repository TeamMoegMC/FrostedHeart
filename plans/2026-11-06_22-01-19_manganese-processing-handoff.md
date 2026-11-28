# 软锰矿处理与锰材料交接

- Time: `2026-11-06 22:01:19 +08:00`
- Authors: 用户（范围与用途）；OpenAI Codex（核对、整理）
- Status: `ready`（可接手；工艺与数值待执行对话确定）
- Scope: FH 材料注册与资源；TWR 软锰矿处理和锰材料配方
- Related: [用途讨论](../discussion/2026-11-06_21-28-15_pyrolusite-manganese-steel-uses.md)

## 目标与边界

用户要求另一个对话先做好“锰本身”：打通现有软锰矿的整套处理，并补齐锰的基础材料形态。交付应是可在整合包中实际加工的链条，不只是注册一批物品。

本轮不实现锰钢、工具／武器部件、破碎机升级或电池，也不改冰洞矿物生成量。未来用途仅用于避免命名和材料语义冲突：锰钢拟提高耐久而不提高效率／攻击性能；机器升级对象未定；一次性电池延后。

## 当前实现（2026-11-06 源码／数据核对）

两仓库路径：

- FH：`D:/games/minecraft/Project TWR/FrostedHeart`
- TWR：`D:/games/minecraft/Project TWR/TheWinterRescue`

| 内容 | 已核对结果与入口 |
|---|---|
| 原矿 | FH `bootstrap/common/FHBlocks.java` 已有 `pyrolusite_ore`、`deepslate_pyrolusite_ore`；保留现有注册和掉落约定 |
| 粗矿 | FH `src/main/java/com/teammoeg/frostedheart/bootstrap/common/FHItems.java` 注册 `frostedheart:raw_pyrolusite`，标签为 `forge:raw_materials/pyrolusite` 和父标签 |
| 处理与金属形态 | 本轮 FH 注册及 TWR `kubejs/`、`config/` 检索未发现锰处理链；不要把已有粗矿当成已具备粉、锭等形态 |
| 常规加工模板 | TWR `kubejs/server_scripts/src/recipes_packaged/ore_processing.js` 管理粉碎矿、部分矿浆、粉和冶炼；没有软锰矿条目 |
| 粉碎配方入口 | TWR `kubejs/server_scripts/src/recipes_packaged/crush.js`、`src/recipes_types/immersiveengineering/crusher.js`（后者同属 `server_scripts/`） |
| 金属冶炼参考 | TWR `kubejs/server_scripts/src/recipes_types/immersiveindustry/crucible.js`；矿物特殊处理也见 `ore_processing.js` 的镍、铅、锌等分支 |
| 设备可用性 | TWR `config/fhresearches/crusher.json` 与 `crushing_wheel.json` 分别有 IE 破碎机和 Create 粉碎轮研究；本轮未启动游戏验证 |
| 注册分工 | FH 已集中注册金属粉、锭、粒等；TWR `kubejs/startup_scripts/src/registries/item.js` 虽可注册物品，但不宜重复建立同一金属的第二套身份 |

通用处理模板不是直接添加一行就能保证正确：它按材料名猜测物品 ID，且支持部分原矿／粉碎矿直接冶炼。软锰矿的矿物名 `pyrolusite` 与金属名 `manganese` 不同，需要显式映射，避免不存在的物品、错误标签或意外绕过加工。

## 推荐交付清单（执行建议，不是用户已确认的具体配方）

优先沿用 FH 现有材料体系，物品／方块、模型贴图、语言和基础标签放 FH；面向整合包的机器配方、成本与进度放 TWR。

| 类别 | 建议首轮内容 | 约束 |
|---|---|---|
| 矿物加工 | 现有粗软锰矿 → 粉碎软锰矿 → 洗净矿／精矿 → 冶炼入口 | 洗选阶段是否独立物品按现有机器和配方习惯确定；明确每一步作用，避免纯凑步骤 |
| 金属材料 | 锰粉、锰锭、锰粒、锰块、锰板 | 补齐可获得配方和必要互转，不能只有 JEI 条目 |
| 条件性形态 | 锰杆等 | 若现有金属体系普遍需要可一并补齐；线、齿轮、熔融液体等不为凑齐而添加 |
| 氧化物 | 若工艺需要，独立命名软锰矿粉或二氧化锰粉 | 不能把含氧矿粉标成金属锰粉；电池级提纯及电池配方不在本轮 |

候选命名：`crushed_raw_pyrolusite`、`manganese_dust`、`manganese_ingot`、`manganese_nugget`、`manganese_block`、`manganese_plate`。正式命名前检查现有约定与依赖注册，不把此表当成必须照抄的最终 ID。矿物与金属标签分别使用 `pyrolusite`、`manganese`，不要把矿粉和金属粉放进同一个材料标签。

## 接手后先确定的工艺决策

1. **金属锰如何得到**：比较现有高炉／坩埚／其它已接入设备能表达的还原路线，优先复用设备。现实工艺与游戏抽象分开说明；不要把“软锰矿直接烧成纯锰”当作已确认方案，也不要因此前提到锰铁就把本轮终点改成锰铁。若路线需要全新机器或大幅扩展化学链，先把简化选项交给用户选择。
2. **洗选与粉末的意义**：推荐洗选保留矿物身份，冶炼后才成为金属；金属锭研磨所得锰粉可用于后续合金。是否另设氧化物粉由实际链条决定。
3. **设备与进度**：确认原料在何阶段可加工，以及已有研究是否覆盖机器；一般的配方接入不必新增研究树。若会明显改变科技阶段，向用户说明。
4. **数量与成本**：参照当前相近矿物确定各步数量、概率、副产物、时间、能耗、还原剂和助熔剂。锰的产率尚未确认；不要照搬其它矿的增产倍数而不计算全链结果。

建议先向用户展示一张简短加工表（输入、机器、输出、成本），把纯金属获取路线等重大分歧确认后实施；命名、资源文件组织、普通互转等常规选择可自行处理。

## 实施顺序与验收

1. 读取适用 `AGENTS.md`、FH `docs/README.md`、相关文档及最新 diary；查看两个仓库状态。当前 FH 有大量其它任务改动，须保留。本轮在 TWR 根目录和相邻目录检索未找到其 `AGENTS.md`，接手时重新检查，不假设规则永远缺失。`design/` 始终只读。
2. 核对现有注册、处理脚本、辅助函数、配方删除规则和机器支持，定下最小可玩的工艺与形态清单。
3. 在 FH 注册材料及资源，在 TWR 接入加工、互转配方；优先复用现有代码，不引入通用材料框架或新机器。
4. 验证 FH 编译／相关数据生成；检查 TWR 脚本加载和配方输出。使用现有开发实例时保护配置与存档，不擅自部署覆盖。
5. 在整合包中核对 JEI 与实际机器：两种矿石掉落到终点可达；无缺失贴图／翻译；洗选不提前变成金属；粉、锭、粒、块和板可获得；可逆互转不净增材料；不存在配方删除或标签歧义造成的断链。粉碎增产与形态互转分别核算。
6. 把已实现行为记录到合适的 `docs/` 文档，必要时补入口；新增 diary，分别报告 FH 与 TWR 改动和验证。将本计划改为 `completed` 并补结果链接；未做客户端实测必须明确标注。

本交接不要求自动提交、推送或部署，也不授权顺带实现未来用途。

## Outcome

待执行对话填写：最终工艺、实际新增形态、文件与文档链接、验证结果、剩余事项。
