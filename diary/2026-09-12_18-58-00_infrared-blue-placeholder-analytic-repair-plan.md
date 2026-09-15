# 红外蓝底与解析场恢复计划

- Time: `2026-09-12 18:58:00 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed`
- Scope: `修订红外修复计划并复查原架构匹配；未实施生产代码`

## Completed

- 整体替换[执行计划](../plans/2026-09-08_21-58-58_thermal-unknown-block-ventilation-75.md#block-surface-infrared)当前正文：删除9³自然背景采样/同步/纹理，恢复蓝色缺值占位和既有服务端解析场显示合成，保留材料表面取样、0.43混合与单纹理。
- 查明恢复场显示必须同时修正：材料epoch不能追踪场变化、materialReadable不能禁用整张最终显示纹理、材料presence不能直接擦掉解析场、场缩小/删除必须重写旧范围，以及double材料值须在合成后量化。
- 明确能量塔真实公式`max(base, naturalAir + getTempMod())`和塔底球形中心；有冷材料也执行保底，不能只在缺材料时补场，更不能把增温或蓝色占位当绝对温度。
- 完整规定field-only full/delta基线、不可读转换、generation/epoch/两个mask的LAST提交，以及delta显式INVALID。复用旧field候选范围与Sample，不恢复旧Air/dormant显示。
- 更新旧红外plan的入口摘要，保持superseded；本轮主plan的先前方案与实施记录后缀逐字保持。

## Decisions

- 不新增客户端field解释器、第二纹理、服务器observer、revision/hash或场温度缓存。恢复旧前后field Page并集刷新，复用按需邻区判断和同biome Brick分层自然计算。
- 全窗口自然背景被删除，但场公式需要的自然基准仍在命中范围按需计算；不能宣称“所有自然温度查询归零”。有场poll的工作随范围增长，计划明确记录此成本。
- 最终纹理含材料基础值及玩法场显示修正，统一称displayTemperature；解析修正不写H/C/T，也不称真实材料升温。
- Documentation impact: 本轮仅改plans和新增日记。living docs仍准确描述当前尚未修复的生产行为；实施时再同步更新。

## Validation

- 核对当前capture、shader、renderer、两个packet、codec、GeneratorData发布中心/模式、GameplayFields独立生命周期、Sample排序/requiresNatural/requiresBase及自然温度入口。
- 用git show只读对照HEAD旧解析合成、前后field范围刷新和自然查询优化，未复制工程或重编旧版本。
- 检查plan入口、历史区分与diff空白；本轮未修改生产/测试源码，未编译或重复运行GameTest。

## Remaining

- 按本计划实施；将原“解析场不影响IR/729背景”测试换成真实generator内外、撤销、叠加、无runtime及事务夹具，运行完整Forge GameTest。
- 实机画面和目标场范围的性能验收。旧57项结果与无场warm-key耗时不是本修复验收证据。
