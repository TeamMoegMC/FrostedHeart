# 独立栽培橡树原型

- Time: `2026-11-18 03:00:00 UTC`
- Author: `OpenAI Codex`
- Status: 实现完成，离线编译、专用 Forge GameTest 4/4、assemble 与成品资源检查通过；客户端未验收。
- Scope: 仅 FrostedHeart；未改原版橡树、整合包或世界生成，未提交或部署。

## 决策与实现

新增 `frostedheart:cultivated_oak_sapling`，使用 BushBlock + 无 ticker 的方块实体。随机刻调用现有 `WorldTemperature.block`；五种大小显示 age=0..4，空手右键显示进度、目标产量/高度和等待原因。自然方块栏及 give 可获取，未添加配方；树木使用原版橡木、树叶及其掉落。

独立 `data/frostedheart/cultivated_trees/oak.json` 内嵌从本地 1.20.1 原版资源复制的橡树 TreeConfiguration，另外定义进度、线性产量高度换算、温度与高度限制。仅支持固定 radius/offset 的直干 blob 树冠且无装饰器；模组执行算法、数据包提供参数，通过 reload 加载。

按实际取得的生长进度积累温度与增产肥料贡献，加速和保温沿用已有肥料等级属性。每次有效推进扣一单位施肥泥土储量，耗尽变普通泥土。成熟固定目标高度和随机种子，避免末刻补肥追溯收益。生成前采用保守的加载区块/世界高度/障碍检查；受阻保留成熟树苗，重试不扣肥、不降高。调用固定高度的原版树生成算法，并保留已结算的根部土壤；返回失败恢复树苗 NBT。

## 验证

从父目录执行，所有显式项目写入路径均以 FrostedHeart/ 开头。

- `bash FrostedHeart/gradlew -p FrostedHeart --offline compileJava`：通过，31s。
- `bash FrostedHeart/gradlew -p FrostedHeart --offline runGameTestServer -PgameTestNamespaces=frostedheart_cultivated_oak -PgameTestDirectory=build/cultivated-oak-gametest`：通过，3m8s，`All 4 required tests passed`。日志 `build/cultivated-oak-gametest.log`。
- 覆盖：内置数据包解析、ceil/高度上限、五阶段推进、目标与进度 NBT 恢复、冷停长不扣肥、保温扩展范围、加速不增产、全程增产、温度边界减产、受阻等待且不扣肥、成熟后补肥不改目标、清除障碍后实际生成 6/12 块橡木并保留肥料土壤。
- `bash FrostedHeart/gradlew -p FrostedHeart --offline assemble`：通过，21s；日志 `build/cultivated-oak-assemble.log`。
- 成品 `build/libs/frostedheart-1.20.1-0.7.17.jar`：检查新增主类、配置、模型、掉落与中英文合并资源齐全，不包含 CultivatedOakGameTests。
- 新增 11 个 JSON 解析及 `git diff --check` 通过。

测试服务器存在原有缺失依赖配方、研究目录与认证联网超时日志，但本次配置正常加载，四项测试全部通过。未修改这些无关问题。未执行客户端外观/交互验收、自然随机气候全周期统计、数据包热重载场景、区块边界场景或独立进程存档重启；NBT 测试不能等同于重启验收。

## 文档影响

新增 `docs/climate/cultivated-oak.md`，记录 ID、配置、公式、默认参数、肥料扣费、空间检查保守性、存档和验证边界；更新气候 README 与 GameTest README 入口。未修改 design/。
