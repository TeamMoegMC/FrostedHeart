# 跑步踩裂薄冰加速

- Time: `2026-10-21 17:44:40 +08:00`
- Author: `OpenAI Codex；实现代理`
- Status: `completed`
- Scope: `ThinIceBlock.contactInterval 跑步参数`

## Completed

- 按用户要求，将跑步开裂参数由 6 调为 4：无冷却时每 tick 开裂概率为 1/4，成功后的冷却为 4 ticks（20 TPS 时 0.2 秒）。
- 文档影响：同步[薄冰说明](../docs/climate/thin-ice.md)参数表，向[讨论](../discussion/2026-10-17_23-27-52_ice-caves-and-thin-ice.md)追加本次确认。

## Decisions

- 跑步继续共用同一参数控制抽样概率与成功冷却；潜行、站立、走路及冲击沿用原参数。

## Validation

- 核对 `contactInterval` 返回值同时供 `ordinaryContact` 的 `nextInt(interval)` 与 `startCooldown(..., interval)` 使用。
- `git diff --check` 通过。此次仅调整常量，未重跑构建或 GameTest。

## Remaining

- 新跑步节奏待实际试玩反馈。
