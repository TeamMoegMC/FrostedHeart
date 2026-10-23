# 薄冰开裂机制：跨对话交接

- Time: `2026-10-16 12:00:10 +08:00`
- Authors: `OpenAI Codex；调查与交接`
- Status: `completed`
- Scope: `Frosted Heart 现有 thin_ice / layered_thin_ice 的踩踏开裂原型；服务 TheWinterRescue 整合包`
- Related: [仓库规则](../AGENTS.md)、[气候文档入口](../docs/climate/README.md)、[温度体系](../docs/climate/world-climate-and-temperature.md)

## 接手目标与授权边界

用户将“薄冰开裂机制”交给另一个对话实现。本对话只调查和编写交接，没有实现、运行构建或修改游戏数据。接手对话应以用户在新对话中的指令为实施授权。

建议先完成现有两种薄冰的可玩原型，再处理冰洞装饰与世界生成。本任务不包含完整冰洞复刻、平台升级、新生物或战斗系统。

执行前读取 `AGENTS.md`、`docs/README.md`、气候系统相关文档、`diary/README.md` 与最新相关开发记录。`design/` 严格只读。代码和数据是当前行为的最终依据；本计划是意图，不是实现事实。此前 `.Codex/memory/project-structure.md` 和 `architecture.md` 未找到，接手时可重新检查。

## 用户已确认的项目范围

1. 丰富 TWR 已存在的冰洞，视觉接近最新原版实验内容，行为适配 FH 温度系统。
2. 只需要类似环境，不包含 Frostbite、冰球、冻结战斗/药水体系。
3. 只服务 TheWinterRescue 整合包。
4. 世界生成只影响新地形，不改造旧区块。
5. 冰洞是需要保暖装备进入的危险区域。
6. 把原版新加入的“会碎的碎冰”功能给到现有薄冰，而非另起一个独立碎冰方块。

注意第 4 项约束的是地形生成：修改现有方块 ID 的行为，会影响旧世界已放置的同种薄冰。此前已经向用户明确说明这一点；没有承诺旧薄冰保留旧行为。

## 原版依据与可靠性边界

前序调查截至 2026-10-16：冰洞和 Brittle Ice 属于 Java 26.4 快照内容，不应称为当时最新稳定版已有功能。薄冰的直接参照是 2026-10-13 发布的 Snapshot 4：

- [官方 Snapshot 4 更新记录](https://feedback.minecraft.net/hc/en-us/articles/49579562483853-Minecraft-Java-Edition-26-4-Snapshot-4)
- [官方 Snapshot 4 文章](https://www.minecraft.net/es-es/article/minecraft-26-4-snapshot-4)
- [官方 Snapshot 3：冰洞环境](https://feedback.minecraft.net/hc/en-us/articles/49412490179853-Minecraft-Java-Edition-26-4-Snapshot-3)

当时官方可确认：玩家或生物踩上后开始开裂，最终破碎；自然生成于冰洞裂隙；可精准采集；水接触蓝冰会形成它；Frostbite 有踩踏豁免。官方还列出四张碎冰纹理和开裂音效。

上述材料没有确定 FH 应采用的开裂时长、恢复规则、潜行豁免、重量计算或破碎后流体处理。下文这些内容是适配方案，不是已验证的原版细节。此次交接未再次联网确认后续快照；实现时如需追随最新原版，应重新核实，避免自动扩大本任务。

## 已核实的现状（交接时重读源码确认）

平台见 [gradle.properties](../gradle.properties)：Minecraft `1.20.1`、Forge `47.3.0`，建议维持当前平台。

| 位置 | 当前行为及意义 |
| --- | --- |
| [ThinIceBlock.java](../src/main/java/com/teammoeg/frostedheart/content/climate/block/ThinIceBlock.java) | 继承 `IceBlock`，`randomTick` 空实现；只有相邻薄冰面剔除逻辑，无开裂状态或踩踏回调。 |
| [LayeredThinIceBlock.java](../src/main/java/com/teammoeg/frostedheart/content/climate/block/LayeredThinIceBlock.java) | `LAYERS=1..8`，默认 1；视觉高度为每层 2/16 格，碰撞高度使用 `LAYERS-1`。因此单层没有碰撞；不能假定普通踩踏回调一定触发。可堆叠放置，`canSurvive` 恒为 true，`randomTick` 空实现。 |
| [FHBlocks.java](../src/main/java/com/teammoeg/frostedheart/bootstrap/common/FHBlocks.java) | 注册 `frostedheart:thin_ice`（`THIN_ICE_BLOCK`）和 `frostedheart:layered_thin_ice`（`LAYERED_THIN_ICE`）。已有模型生成、随机外观和掉落设置。 |
| [FHSoundEvents.java](../src/main/java/com/teammoeg/frostedheart/bootstrap/reference/FHSoundEvents.java) | 已有 `ICE_CRACKING` / `ice_cracking`，优先试听复用。 |
| [IceBlockMixin_Melt.java](../src/main/java/com/teammoeg/frostedheart/mixin/minecraft/temperature/IceBlockMixin_Melt.java) | 原版冰随机融化由 FH 接管；非精准挖掘的水生成与 FH 温度有关。继承关系和注入效果须一起检查。 |

其他搜索锚点：`FHBlockStateGen.simpleCubeAllRandom`、`FHBlockStateGen.layeredRandom`、`FHLootGen.buildSnowLootTable`、`FHClientEventsMod`、`ICE_CHIP`、`ice_cracking`。现有五种薄冰外观是随机纹理，不能直接视为五个裂纹阶段。

完整薄冰已有精准采集掉本体、其他情况掉冰屑的配置；分层薄冰使用分层掉落生成器。不要把“机械踩碎”和“玩家挖掘”无意绑定成同一掉落/生水路径。

## 原型建议规则

用户已认可优先做薄冰原型；以下具体参数没有逐项获得独立确认。可作为原型默认方案，实施说明中应标明选择。

| 决定 | 推荐方案及理由 | 替代项/待验证点 |
| --- | --- | --- |
| 两种薄冰的关系 | 共用开裂状态推进逻辑，厚度与裂纹分开表示，保留现有 ID 和层数。 | 避免复制两份易产生差异的逻辑；不必为此建立大型框架。 |
| 离开后的行为 | 一旦触发，继续开裂直至破碎；预留明确撤退时间。 | 离开后暂停或逐渐恢复也是可选设计，尚未确定原版行为。 |
| 厚度 | 层数越少，破裂越快；完整薄冰与满层薄冰关系保持一致。 | 具体时长未定，用少量可调参数和试玩确定；不引入未要求的重量系统。 |
| 单层碰撞 | 明确解决单层无碰撞的问题，再选择踩踏检测。 | 可调整碰撞与外观一致，或保留现状并提供适用的接触触发；需考虑已有放置和通行体验。 |
| 预警 | 分阶段裂纹与现有音效，让玩家能看懂风险并撤退。 | 原型可先采用简单阶段；最终模型需覆盖全部层数和随机外观。 |
| 破碎与融化 | 机械破碎、温度融化分别处理；悬空薄冰踩碎留下空气。 | 水源、邻近流体与维度规则需验证，不能凭空生成水。 |
| 保暖装备 | 装备抵御洞内低温，薄冰承担行走风险。 | 暂不添加保暖装备防踩裂特权。 |
| 蓝冰与水生成薄冰 | 放到后续温度/相变适配阶段。 | 不直接复制原版无条件转换，避免绕过 FH 热学；不是首个原型的必要工作。 |

进一步需要实施者作出并记录的常规选择：玩家/生物触发范围、潜行/跳跃是否区别处理、机械破碎是否掉落、破裂是否传播、堆叠加层时如何处理已有裂纹。没有用户要求的连锁坍塌、职业装备豁免等机制，先不扩大范围。

## 建议实施顺序

1. 重读上述类、注册、资源生成器、热学入口和相关文档；全仓搜索两种薄冰 ID，查清现有放置/生成用途。
2. 确定碰撞、触发、阶段推进与破碎结果；在服务端推进状态，客户端提供一致的裂纹和声音。
3. 覆盖完整薄冰与 1..8 层薄冰；保留堆叠、精准采集、面剔除、透明渲染等现有能力。检查新增状态对旧存档默认值和模型数量的影响。
4. 验证区块卸载/重新加载后的计时行为，避免重入重复计时、反复触发音效或永不破碎；选择当前平台支持的简洁方案并记录结果。
5. 做针对性编译/验证和游戏内体验检查，修复问题后更新相关 living docs，并按 `diary/README.md` 记录实际实现、验证与剩余工作；将本计划状态及 Outcome 更新为真实结果。

首次原型无需改动伴随整合包。若确需改 KubeJS、数据包或配方，伴随仓库在 `D:\games\minecraft\Project TWR\TheWinterRescue`；先重新查找其 AGENTS.md（前序调查未找到），两个仓库分别检查并报告变更。

## 验收重点

- 生存玩家踩踏两种薄冰能触发；移动离开后遵循选定规则，裂纹/声音有清晰预警。
- 分层 1、2、7、8 层与完整薄冰的碰撞、厚度差异、相邻渲染正常。
- 持续站立、快速跨越、多个实体同时踩踏不会产生计时或声音异常；专用服务端与客户端表现一致。
- 悬空与水上两类场景的破碎结果合理，温度融化和挖掘仍走各自规则。
- 旧存档已有薄冰可加载，已有层数保持；卸载重载后的裂纹行为明确。
- 精准采集、普通挖掘、堆叠放置、已有随机纹理与透明渲染按最终设计工作。

选择有意义的状态推进/交互测试，不要求为简单实现机械补测试。构建命令应遵从当前仓库环境；不要复制项目另编一份或引入路径敏感哈希校验。

## 与后续冰洞工作的边界

后续工作另行开展：冰晶和冰锥、洞穴冰/方解石材质与裂隙、新区块生成、按目标保暖装备档位校准洞温。目前冰洞 ID 为 `the_winter_rescue:ice_caves`，FH 温度配方修正为 `-5.0`，这只是温度加项，不是恒定洞温 -5°C。

用户仍未指定哪一档现有保暖装备应能常规探索冰洞。该问题影响后续温度平衡，不必阻塞薄冰原型。不要在本任务中顺带重做世界生成或温度体系。

## Outcome

交接创建时仅完成文档。2026-10-18 已完成两种薄冰原型、阶段资源与自动验证，最终规则与验证记录见下方实施结果。

## 2026-10-18 实施更新

用户在关联讨论中已确定四状态、概率与冷却、冲击高度、小数概率、分层承受次数和堆叠比例。热裂与修复按相变热量预算实现，二者均为对应完整相变的 1/4；替代原交接建议中的固定延迟和离开后继续开裂。原型行为及稀疏裂纹工作进度已完成实现和自动验证，人工试玩尚未进行。当前说明见 [thin-ice.md](../docs/climate/thin-ice.md)，验证见 [开发日记](../diary/2026-10-18_14-30-27_thin-ice-mechanics-and-phase-work.md)。交接创建时的 Outcome 仅描述当时状态，不代表现在尚无改动。

## 实施结果

两种薄冰的概率踩踏、冷却、起跳/落地多级冲击、六面邻水破碎、厚度承受次数、比例堆叠及温度裂纹工作预算已实现。数据生成通过；107 项 frostedheart 回归通过；随后完成单层落地兼容与测试夹具修正，最终 5 项薄冰专项全部通过。测试组已加入默认 namespace 列表。没有冰锥、通用碎裂框架或整合包修改。客户端试玩与性能实测尚未完成，讨论中的反馈仅为自动场景验证。

最终行为见 [实现文档](../docs/climate/thin-ice.md)，验证与剩余工作见 [开发日记](../diary/2026-10-18_14-30-27_thin-ice-mechanics-and-phase-work.md)。
