# 无新增 Mixin 的共享矿床实现与验收

- Time: `2026-10-24T04:29:02+08:00`
- Author: `OpenAI Codex / GPT-6，实施、源码核对与验收`
- Status: `completed`（实现与本文明确范围的服务器验收；完整整合包客户端表现未测）
- Scope: 四个 deposit 主类、FHFeatures/FHBlocks 接入、TheWinterRescue 的数据与生成工具

## Completed

- 注册一个普通 `frostedheart:deposit` Feature 和一个无 BE、无随机 tick 的 tuff 围岩块。主体、裂隙、两处必有洞厅、可选侧厅、岸台、台地池和矿化复用一份种子布局。
- 单元粗筛、一次原生候选群系查询、按 ID 查 Profile、最多五次基础高度探针；配置自有 64 槽原子缓存，用弱引用识别 generator/RandomState，无世界对象或已生成标志。
- 当前区块写入、20x20 几何 halo、两格封闭层；内侧皮层不拆背后的围岩；普通 setBlock 保留世界生成/FH 现有写入路径。
- 移除旧的未注册 hydrothermal 草稿。没有新增 Mixin、BiomeSource、ChunkGenerator、Jigsaw、供热或持续回填水逻辑。
- 实现 Python 标准库导出与 --check/--registry，主配置唯一来源是 placement/profiles。原有 normal 的 1,374 条记录与气候工具没有被本功能重生成。
- 导出器补齐旧 all_twr 未列出的群系，静态覆盖实际 normal 的全部 45 种可能群系。

## Decisions

- 为守住矿量，保留原有背景矿脉次数，晶洞平均次数仅从 1.4 降为 1.3；大幅降低次数会使速度改善混入资源削减，已撤回该尝试。
- 去掉热液 biome 的独立湖/delta/magma/岩浆泉入口，减少闪长岩、方解石和藤蔓重复扫描，新增主体承担主要地貌。
- Shape/Range 等嵌套 Codec 可被独立调用；通用检查移到独立私有 Codecs 嵌套类，避免先访问 Shape.CODEC 时触发外层初始化循环。
- GameTestInfo 执行后按 Runnable 身份移除回调；重复计时采用新的包装回调。修正后完成真正的 600 秒观察，并以 5 秒回归再次覆盖重排路径。
- 长测试使用编译输出快照，避免同目录其他构建删除正在加载的类；没有复制源码后另行编译，也没有路径相关校验。

## Validation

- Java 17 / Forge 47.3.0：`jar`、`reobfJar` 与独立命名空间 GameTest 通过。JAR 包含 Feature/Codecs/围岩资源，不包含 GameTest 夹具。
- 两份真实 Forge 记录交叉核对：长时流体 600.418 秒、加速 GameTest 1,596,000 tick，水体 24,946 → 24,946；最终 Codec 修复后回归与其几何、矿化及初始水量指纹相同。
- 8 个真实种子、256 个候选（175 采用/81 拒绝）；32 份完整布局；4 线程、缓存冲突/淘汰/反向顺序、种子及 generator/RandomState 身份隔离。
- 完整相交片段写入，空腔/宿主两种场景；矿带顺序一致；第二份 clay 材料配置复用 Writer；保留 chest/BE、bedrock、lining、lava、oak_planks。
- 岸台 1,001 柱，满足 3x3 水平净空的 627 个中心组成单一连通区域。64 槽缓存及布局分配上界 57,152 bytes（共享材料配置另计）。
- 同种子、同坐标轮换请求：热液每组 240 FULL 区块，最终均值 48.48 → 41.96 ms（-13.45%），P95 111.33 → 96.24 ms（-13.56%）。普通组每组 128 区块，均值/P95 在约 2% 波动范围内，未见 >3% 回退。
- 相同 61,440 格 X/Z 面积、Y [-64,40) 的六种矿物计数变化 -4.18%..+6.32%，均在 ±10% 门槛内。
- 额外原生 Minecraft 检查使用实际 normal.json 与包内 NoiseRouter：8 种子、32,768 单元中，290 个中心选中热液群系，260 处通过地形高度检查。它不充当 Forge 性能基准。
- 导出 --check、真实 Forge 方块注册表引用检查、45 群系覆盖、JSON/Python 语法与改动的 diff --check 通过。
- 原始记录、统一 14 项门槛、柱数据及图示：[整合包验收目录](../../TheWinterRescue/Scripts/worldgen/deposits/results/REPORT.md)。

## Remaining

- 性能数据使用原版噪声设置的固定群系对照。完整包自定义 router 的完整生成耗时、实际装饰成功频率、客户端透明水体/粒子成本和任意第三方结构交叉未验收。
- 方块实体/保护标签的保留不等于完整石质结构避让；已有区块不回填，修改 salt/尺寸只影响未来生成。
- Living docs 已新增 [world-generation.md](../docs/world-generation.md) 并更新 docs/README；真实气候与性能的不同验证范围均已注明。
