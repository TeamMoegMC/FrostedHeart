# 独立栽培橡树原型

- 状态：原型实现；服务端 GameTest 4/4 通过，真实客户端放置、空手右键、随机刻成熟、障碍重试及生成渲染检查通过。
- 最后核对：2026-11-18。
- 范围：Minecraft 1.20.1 / Forge；仅 `frostedheart:cultivated_oak_sapling`，不替换原版橡树苗、自然树形或世界生成。
- 代码锚点：`CultivatedOakBlock`、`CultivatedOakBlockEntity.advance`、`CultivatedOakProfile`、`CultivatedOakGameTests`。

## 试用

创造模式自然方块栏或 `/give @s frostedheart:cultivated_oak_sapling` 获取树苗，种在泥土或施肥泥土上。确保光照至少 9，周围留足空间。空手右键查看阶段、累计进度、成熟目标与等待原因。阶段 `age=0..4` 使用同一张原版橡树苗贴图逐渐放大。

自然随机刻推进，没有每 tick 的方块实体 ticker，也不使用原版骨粉接口。默认理想温度为 20°C，正常生长范围为 5～35°C；实际温度读取 `WorldTemperature.block`，包括现有气候与局部热环境。可以临时提高 `randomTickSpeed` 加快观察，但这也会影响世界中的其他随机刻方块。

成熟前目标木头和高度显示为 0，表示尚未定稿。成熟后下次随机刻尝试生成；环境之后变冷不会撤销已经完成的生长。树木使用原版橡木和橡树叶，树叶仍按原版掉落橡树苗；本原型未添加合成或独立木材。

## 数据包

覆盖 `data/frostedheart/cultivated_trees/oak.json` 后执行 `/reload`。新增目录由服务端资源重载监听器解析，不是原版 configured feature 注册表，也不是通用脚本语言。移除或无法解析配置时树苗保留并等待修正，错误写入服务端日志。

`tree` 内嵌原版橡树 `TreeConfiguration`，通过原版 codec 解析；生成时仅替换为固定高度的 `StraightTrunkPlacer`。首版支持直干、blob 树冠、无装饰器，树冠 radius 和 offset 必须为固定整数；radius 为 0～8、offset 为 0～8。生成使用 ignore vines，保留根部土壤，不支持 force dirt 或装饰器扩展。

| 字段 | 默认值与含义 |
|---|---|
| `growth.progress_per_stage` | 4，每一阶段所需进度，共四次阶段转换 |
| `growth.min_light` | 9，成熟前的最低有效光照 |
| `yield.base_logs` | 6，理想环境无肥料时的目标木头数 |
| `yield.logs_per_height` | 1，线性换算斜率 |
| `yield.fixed_logs` | 0，线性换算固定木头项 |
| `yield.min_height` / `max_height` | 4 / 12，树干高度上下限；支持范围 4～24 |
| `temperature.min` / `ideal` / `max` | 5 / 20 / 35，单位 °C |
| `temperature.minimum_yield_factor` | 0.5，温度在生长边界时的产量系数 |
| `temperature.preserved_degrees_per_grade` | 5，每单位保温等级向下扩展的温度范围，单位 °C |

换算公式为 `height = clamp(ceil((targetLogs - fixed_logs) / logs_per_height), min_height, max_height)`。对于默认直干橡树，每格树干一块橡木，因此高度即木头数量；修改线性系数只会修改估算关系，不会使原版生成器自动增加分枝或兑现任意木头预算。

## 生长与肥料

温度适宜度 q 在最低/最高温度为 0，在理想温度为 1，两侧线性插值；超出生长范围则停止。每次有效随机刻增长 `0.25 + 0.75q` 进度，产量系数为 `minimum_yield_factor + (1-minimum_yield_factor)q`。

增产肥料将当次产量系数乘以现有 `FertilizerGrade.productivity`；加速肥料将当次进度除以 `0.5 × FertilizerGrade.growSpeed`；保温肥料将最低温度降低 `preserved_degrees_per_grade × FertilizerGrade.preserve`。只识别根部 `FERTILIZED_DIRT`，不增加直接对树苗施肥入口；可先施肥泥土，再种树，或对根部泥土补肥。

每次确实取得生长进度消耗一单位土壤 `STORAGE`，耗尽后变为普通泥土。三类肥料均采用这一原型扣费方式，与已有原版树苗概率扣费规则独立。中途补肥只影响之后取得的进度。累计 `yieldCredit += actualProgress × yieldFactor`；成熟目标为 `base_logs × yieldCredit / progress`，并固定目标高度和树形随机种子。因此临成熟补肥不能追溯增加全部产量。

默认全程理想环境下，无肥料需 16 次有效随机刻，目标高度 6；基础加速肥料需 8 次；全程基础增产肥料且持续补足储量则高度 12。默认随机刻速度下，无肥料平均约 18 分钟成熟，这不是固定倒计时。

## 空间、保存与限制

生成前保守检查树冠半径加一范围内、从根部到目标高度加树冠偏移再加二的包围盒；检查世界高度、区块加载和可替换方块。不会为了检查加载周围区块。这个检查比原版实际树形更保守，可能要求移除本不会触及的邻近障碍。

空间不足保留 `age=4` 和目标，后续随机刻重试，不扣肥、不降低高度。检查通过后使用固定高度和固定种子调用原版 `Feature.TREE.place`；返回失败时恢复树苗及生长 NBT。正常成功生成后保留根部土壤，避免原版树干生成器覆盖剩余肥料。未引入通用树木放置事务。

方块实体保存 `Progress`、`YieldCredit`、`TargetLogs`、`TargetHeight` 和 `TreeSeed`。打掉树苗只掉新的树苗物品，不携带原株生长进度。重载配置会影响尚未完成的生长规则，以及待生成树木的树冠配置；成熟目标高度不重新抽取。

## 验证入口

从工作区父目录执行：`bash FrostedHeart/gradlew -p FrostedHeart --offline runGameTestServer -PgameTestNamespaces=frostedheart_cultivated_oak -PgameTestDirectory=build/cultivated-oak-gametest`。

覆盖配置解码与线性换算、五阶段状态、冷停长和保温、加速与增产、边界温度减产、NBT 恢复、障碍重试不扣肥、成熟后补肥不追溯，以及实际生成 6/12 块树干。测试使用显式温度调用同一生长逻辑，不替代自然气候全周期统计、客户端渲染或独立进程存档重启验收。

2026-11-18 客户端补充验证：使用真实树苗物品右键泥土放置，空手右键收到阶段状态；临时世界内以 heat_adjust 提供 20°C 环境、randomTickSpeed=500 加快自然随机刻，成熟并受阻时目标固定为 6，移除障碍后实际生成六格橡木树干，客户端持续正常渲染。截图与命令记录位于 `run/agent/screenshots/`、`build/cultivated-oak-client-check.json`，详见客户端验证日记。这些临时参数不修改生产配置。
