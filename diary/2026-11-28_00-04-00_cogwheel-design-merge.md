# 合并传动机械的齿轮设计

- Time: `2026-11-28 00:04:00 +08:00`
- Author: Codex（OpenAI，研究内容实现与验证）
- Status: completed
- Scope: 《冬季救援》的传动机械成果、齿轮工作台配方、下游开题和旧档映射。

## Completed

- 将青铜大小齿轮设计合并为「齿轮设计」，统一授权青铜、铸铁和钢各两条工作台配方。更新机械风箱、流体运输及联络的成果名称。
- 更新[传动机械](../docs/knowledge/transmission-machinery.md)、[机械风箱](../docs/knowledge/mechanical-bellows.md)、[流体运输](../docs/knowledge/fluid-transport.md)和[旧档映射](../docs/research/legacy-content-disposition.md)。

## Decisions

- 保留原小齿轮成果 ID，旧研究的两个齿轮效果都映射到该成果。Create 序列组装仍通过独立机器配方执行。

## Validation

- Gradle 预检通过 80 项旧目录定义。执行 KubeJS 工作台配方脚本并核对六条齿轮配方、三个项目成果、两个旧效果、四处下游引用及中英文文案。

## Remaining

- 正式整合包客户端内的配方权限和旧档加载尚未实玩。
