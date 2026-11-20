# 阴燃利用与铜制手炉试制

- Time: `2026-11-20 01:30:59 +08:00`
- Author: Codex（OpenAI，手炉研究实现与验证）
- Status: completed
- Scope: FrostedHeart 低温事件与知识文档，TheWinterRescue 手炉研究定义、T1 提示与配方。

## Completed

- 完成 `create:copper` 后，手部或核心体温达到现有体感分界时可随机想到阴燃利用。联络与启动同时要求铜锭和零下炭类观察。
- 实验台按开题时的黏土知识选择 8 或 4 铜板加 1 黏土，600 tick 成功后交付空手炉。结题解锁固定 ID `the_winter_rescue:research/hand_stove`。
- `legacy_replacement` 保留旧手炉配方效果对应的制造权，承接旧完成及部分进度。煤炭棒和木炭棒退出旧配方锁定。更新 T1 手炉专属说明、[玩家和内容流程](../docs/knowledge/hand-warmer.md)及[旧目录处置](../docs/research/legacy-content-disposition.md)。

## Decisions

- 现场寒冷阈值取低于 0°C。炭类含煤炭、木炭、焦炭，以能进入手炉炭棒燃料链为依据。
- 服务端每 200 游戏刻抽取一次，基础概率为 `1 - 0.999^10`，每项辅助观察加 1 个百分点。核心事件沿用 HUD 的温度分界，服务端读未平滑的核心体温。
- T1 手炉沿用绘图台前置。首批木炭可于木炭坑取得，实际观察点须为零下。实验台是开题后的实物交付节点。

## Validation

- Java 17 编译通过。Forge 47.3.0 定向运行共 31 项 required GameTest 通过，覆盖铜进度和两个低温边界、两档材料消耗、开题后分支固定、空手炉交付、结题配方权限及旧记录承接。日志在 `build/hand-stove-verification/gametest-fixture.log`，临时验证源码复用独立 `handStoveFixture` 源码集。
- 服务端加载正式手炉知识 JSON 和旧定义。配方测试从 TWR 的 KubeJS 注册脚本捕获实际形状、输出和 ID，再用等效配方数据加载到 Forge。旧目录 80 份预检、中英文和研究条件检查、KubeJS 脚本语法及两库差异格式检查通过。

## Remaining

- Prism 部署、整包 KubeJS 启动与客户端生存验收仍待完成。
