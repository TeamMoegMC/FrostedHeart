# 冰洞薄冰条带与大小晶体生成分工

- Time: `2026-10-22 19:39:47 +08:00`
- Author: `OpenAI Codex; worldgen 子代理`
- Status: `partial`（实现及定向用例完成，集中编译、GameTest 与自然采样由主代理执行）
- Scope: FH `IceCaveSurfaceFeature`、`IceCaveIceBlobFeature`、`IceCaveDecorationFeature`、`IceCaveSpringFeature`、新增 `IceCaveSmallCrystalFeature` 及对应 GameTests

## Completed

- 薄冰条带加入同一稀疏区域掩码与至少四格同材质六邻接分组流程。新增噪声区间 `-0.15 < n < 0.05`；保留浮冰 `0.35 < n < 0.55`、蓝冰 `-0.65 < n < -0.50`。
- 团状冰移除旧 `1/180` 逐格机会，改为每个成功冰体最多一处内部荧石。优先内部大晶体口袋，支撑荧石也必须位于几何内部；无合法口袋时以内部实体冰格替换整块荧石。没有持久计数器，不为荧石拒绝本可生成的团块。
- 原后置口袋、外露镶嵌及温泉提示全部使用 `FHBlocks.LARGE_ICE_CRYSTAL`；普通后置 `1/360` 机会保留。
- 新注册 `frostedheart:ice_cave_small_crystal`，配置为空。每候选最多一块小晶体，六方向各最多搜索 20 格空气，遇实体停止；保留实际群系与可写范围检查。支持天然岩、冰、矿物等实际可附着面，含地面，不检查宽厅、厚冰或温泉邻近。
- 文档影响：当前行为需更新 [ice-caves.md](../docs/worldgen/ice-caves.md)，由主代理统一维护；TWR placed 数据及资源注册由其他代理处理。

## Decisions

- 原版基准来自本地 `E:/DevCache/.gradle/caches/forge_gradle/minecraft_repo/versions/1.20.1/client-extra.jar` 的 `data/minecraft/worldgen/{configured_feature,placed_feature}/glow_lichen.json`：候选 `104..157`，高度 `above_bottom:0..absolute:256`，`in_square`，`OCEAN_FLOOR_WG` 表面下至少 13 格，`biome`，局部搜索范围 20。小晶体复用 placed 候选布置，单独有限搜索，不扫描完整空气体积；原版地衣禁用地面，本次按用户要求启用所有六面。
- 保矿优先：极端团块若所有几何内部格恰为矿石，则不替换矿石、不制造裸露荧石，也不撤销成功冰体。正常及混矿团块逐团保底；测试分母包括全矿岩壁下成功团块，比例要求至少一半。

## Validation

- `git diff --check` 通过。
- GameTests 增补：三条带材质均出现且每组至少四格；小晶体六向天然支撑、温泉邻近、有限搜索及单候选最多一格；团块四种正负区块起点、24 个种子、三种矿型，统计所有成功团块、验证至少 50% 内含荧石、混矿保矿、大小端点及每团最多一处。
- 按任务分工，本子代理没有运行 Gradle、打包或提交；上述运行结果以主代理后续集中验证为准。

## Remaining

- 主代理集中编译与 GameTest，更新当前系统文档和自然采样。实际景观、天然频率及整合包试玩验收独立于合成测试。
