# 传动机械分阶段研究

- Time: `2026-11-23 00:48:14 +08:00`
- Author: Codex（OpenAI，研究内容实现与验证）
- Status: completed
- Scope: The Winter Rescue 的传动机械知识数据、配方 ID 和双语提示，Frosted Heart 的知识文档。

## Completed

- 冶铁炉设计接续到两项实物检视和一关零件排样。随后在实验台消耗 10 块青铜板和 1 根传动轴。实验运行 200 游戏刻，无产物与返还，手动完成后授予四项设计。
- 旧研究四条配方效果分别迁入四项设计，旧完成资格继续可用。更新[传动机械说明](../docs/knowledge/transmission-machinery.md)、知识索引和旧目录处置表。

## Decisions

- 复用两根铁棒制 8 根轴的现有配方作为排样完毕后的临时权限。进入试装阶段后仍可制作，项目完成后由设计接续。
- 正常发现使用冶铁炉成果。迁移想法为已经投入旧研究的队伍保留独立入口。

## Validation

- [28 项 Forge 回归测试](../build/transmission-gametest.log)通过，覆盖阶段推进、拼图、旧档桥接和实验台。
- [2 项传动项目定向测试](../build/transmission-content-gametest.log)通过。加载同级整合包的项目定义及从配方脚本提取的测试配方，验证拼图定稿、观察不扣料、只临时放开铁棒制轴、实验台材料不足时不扣料、开工扣料、完成无返还、永久授权以及旧档接续。双语文案、六条配方脚本注册和四个旧效果 ID 通过单独校验。

## Remaining

- 正式整合包客户端、生存获取、任务书界面和机器切割未实玩。Create 机械锯继续按其 `RecipeFinder` 路径执行。
