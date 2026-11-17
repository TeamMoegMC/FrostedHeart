# 余温回收、卸载与红外生命周期设计

- Time: `2026-11-05 16:55:04 +08:00`
- Author: `Codex; OpenAI GPT-6 Astra; primary architecture agent`
- Status: `completed`（架构设计及源码核对；运行时实施尚未开始）
- Scope: 空气/材料普通余温退休、Page/Brick 交接、Chunk 保存卸载恢复、有限维护、红外删除和散热校准

## Completed

- 完成[可实施架构](../plans/2026-11-05_16-44-09_thermal-residual-retirement-architecture.md)，状态为 ready；明确当前代码事实与拟实施行为。
- 核对当前 1°C/0.5°C 活动阈值、空间空气 0.0625°C 捕获过滤、旧 residual 分支 0.25°C 过滤、材料无平衡回收和 1800 秒休眠半衰期的不同作用。
- 追踪冷退役/缩小、普通 SAVE、真实 UNLOAD、取消、重新加载、未消费 J、相变、旧读者和红外 revision/INVALID 的数据归属。
- 确认 `MinecraftPageManager.retire()` 未处理 `captureDormantPage()` 的失败返回，而 shrink 已有重试。将修正列入设计，没有据此宣称已复现丢失。

## Decisions

- 复用 0.5°C 退出尺度；普通冷退役直接消除尾部，已卸载 Chunk 不注册运行任务。
- 对已加载且仍有休眠状态的少数 Chunk 使用一个服务器主线程有限轮转表；每 Chunk 最多一项，不建立到期堆、逐材料定时器或新的保存协议。
- 查询保持只读，清理按 Section 一次发布、一次 revision 更新；最后复查放弃了“查询先独立隐去、以后才压缩”的草案，避免天气变化时旧记录重新出现，也省去永久逻辑删除位图。
- 保留原 sampleTick、H/branch 和现有待消费 J 责任，重复读取/保存不重新起算冷却。红外删除复用已有增量编码，保留当前蓝色缺失占位合同。
- 30 游戏秒半衰期与约 1–3 分钟普通篝火余温目标为首轮校准候选。必须对照在线和卸载返回曲线，不能把降低存档半衰期直接当作在线散热已修好。
- Documentation impact: 本次只新增计划和完成记录，现行 docs 继续描述当前实现；生产实施后再同步拥有各行为的文档。`design/` 只读。

## Validation

- 只读核对 `WorkerPageStore`、`RegionalQuerySnapshot`、`MinecraftPageManager`、`MinecraftThermalInput`、`RegionalCheckpoint`、`DormantChunkThermalState`、`AirFieldCheckpoint`、`MaterialSectionState`、`PendingHeatCheckpoint`、`MinecraftPhaseController`、`InfraredCapture` 和相关网络/渲染入口。
- 核对现有 Forge 生命周期、粗档冷退役、保存重启和工作区篝火红外复现夹具的作用；没有运行或改写其他工作的测试。
- 用 PowerShell 计算候选 30 秒半衰期下 2/10/20°C 温差到 0.5°C 的时间，分别为 60/129.66/159.66 秒；核对混合采样时间、正负温度和三个自然基准下的 Section 显热上界均保守。这是公式检查，不是玩法或性能实测。
- 检查新文档相对链接、空白和设计状态。没有生产源码、配置、数据包或构建产物变更，未进行编译/游戏运行。

## Remaining

- 按计划实施并完成生命周期、相变、红外客户端、独立进程重启及真实负载验证。
- 通过实际散热曲线确定最终半衰期/在线参数；依据主线程尾延迟和存活数据确定维护预算与支持容量。尚无实测提速结论。
