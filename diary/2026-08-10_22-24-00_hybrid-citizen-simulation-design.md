# 城镇居民混合模拟架构设计文档

- Time: `2026-08-10 22:24:00 +08:00`
- Author: `Kimi-K3; AI assistant`
- Status: `completed`
- Scope: `docs/hybrid-simulation-architecture.md`（新文档，无代码改动）

## Completed

- 应用户要求，为“冬季救援”城镇居民系统输出完整的混合模拟（服务端纯数据 + 客户端纯渲染）架构方案，落地于 `docs/hybrid-simulation-architecture.md`。
- 内容覆盖：SoA 数据层（定点数、交换删除）、分帧调度与活跃度 LOD、整数状态机行为、路网图+流场+分离力三层寻路（异步线程池）、byte 量化朝向、AOI+脏标记 5Hz 快照同步、客户端双快照插值、Flywheel 实例化渲染与近距假实体切换、交互 RPC、SavedData 持久化、性能预算表与 P1–P5 实施路线图。
- 方案绑定本仓库事实：MC 1.20.1 / Forge 47.3.0 / Java 17；复用 chorda 的 CBaseNetwork、world 级 SavedData 封装；利用已有 Flywheel 0.6.11（Create 依赖）做实例化渲染；建议包位置 `frostedheart/content/town/citizen/`。

## Decisions

- 选混合模拟而非轻量自定义实体：用户目标规模为数千至上万居民，实体体系在该规模下不可行。
- 寻路选流场（Flow Field）共享 + 异步 BFS，而非逐单位 A*：同目标单位共享一次搜索，主线程零阻塞。
- 同步发“位置+速度+状态”5Hz 快照而非逐帧位置：客户端外推+插值，带宽可控。
- 渲染按距离分三路：近距假实体（原版管线、可交互）、中距 Flywheel 实例化、远距 Billboard。

## Validation

- 纯设计文档，未编译验证；其中性能数字为估算值（文档内已标注假设），落地后需用 spark/JFR 回归。

## Remaining

- 按文档 P1 阶段开始实现：CitizenSim SoA + CitizenSimManager(SavedData) + 分帧 tick + 状态机。
