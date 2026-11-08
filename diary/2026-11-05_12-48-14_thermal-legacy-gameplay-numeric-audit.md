# 新热模拟与旧玩法数值核对

- Time: `2026-11-05 12:48:14 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed` for the source audit; gameplay restoration remains outstanding
- Scope: `docs/climate/`、旧基准 `0c9fe6209` 与当前 `61eddf706` 温度源码/生成配方；伴生整合包只读

## Completed

- 用户确认旧版指新热模拟引入前的实际玩法，而非新热模拟内部的早期调度/数值实现。
- 逆向检查 HUD、空气/方块/作物入口、能量塔、物理热源、身体/衣物/装备、接触、伤害、食物和旧身体 NBT；结论为当前没有一比一还原。
- 核对维度/群系/作物/旧方块热源/饮品五组生成配方无差异，区分配方保留与执行链路保留。确认旧普通热源配方未被当前供热运行时读取。
- 确认能量塔方块合成从旧 `applyHeat` 改为 `FLOOR_FROM_NATURAL`，`N=-40,H=30,P=N` 时旧为 20°C、当前为 −10°C。
- 对照旧体感 HUD 与当前空气 HUD、旧天气/昼夜及身体积分、新功率装备、相变和旧人物体温重置等差异；记录源码公式求值的范围。
- 新增[现行数值对照](../docs/climate/thermal-legacy-gameplay-parity.md)，在气候入口、世界公式和玩家文档加入链接。没有修改生产代码、配置、整合包、城镇、旧粒子模拟器或提交状态。

## Decisions

- 不通过随意调功率掩盖确定性的消费公式差异；没有在审计期间替换平衡公式或恢复旧后端。
- 区分旧源码实际执行与旧注释意图，包括负雨雪配置被减去、极冷概率和旧身体没有读取 `tempSpeed`。
- 明确旧玩法一致性需要独立证据，新模拟内部精度/连续性和生命周期测试不能替代。

## Validation

- `git show` / `git diff` 直接比较旧源码与当前源码；无历史工程复制或编译。
- 搜索配置及 `BlockTempData` 的全部生产消费者，核对实际入口及未接入的旧热源。
- PowerShell 对核对过的能量塔与玩家第一步公式求值；属于源码公式例子，不是实际世界、长期体温或性能实测。
- 伴生包读取 `defaultconfigs/frostedheart-server.toml`、`block_temp.js` 和 Git 对照；没有写入。
- 仅文档变更；未运行 JUnit、编译或宽泛 GameTest。

## Remaining

- 旧玩法还原未实施；需先统一消费者公式与热源接线，并明确新物理传播/蓄热与严格旧玩法一致性的边界，再统一重点验收。
- 本次不提供长期体温、取暖范围、相变时间或最大 0.1°C 旧玩法误差的资格。
