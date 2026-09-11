# 白幕 V1 到 V2 性能工程路径

- Time: `2026-08-24 05:24:58 +08:00`
- Author: `Codex; OpenAI GPT-5; primary architecture planning`、`Codex subagent; OpenAI gpt-5.6-sol ultra; independent performance reviewer`
- Status: `completed`
- Scope: 白幕 V1/V2 实现计划、当前天气渲染文档的计划入口

## Completed

- 在 [`V1 服务端低开销空间渲染计划`](../plans/2026-08-24_04-25-01_white-curtain-server-efficient-spatial-rendering.md) 中冻结 `ClientWeatherState`、`ClientWeatherFrame` 和可替换 render backend 的 V2 handoff contract。
- 新增 [`V2 电影级渲染计划`](../plans/2026-08-24_05-08-24_white-curtain-v2-cinematic-rendering.md)，覆盖低分辨率 scissored raymarch、独立 temporal resolve、depth-aware composite、资源生命周期、玩家档位选择、性能预算、故障恢复和验证矩阵。
- 在 [`docs/climate/weather-rendering.md`](../docs/climate/weather-rendering.md) 增加明确标注为未实现的 V1/V2 计划入口；现役说明没有改写为未来架构。

## Decisions

- V2 对服务端 CPU、内存、网络和 gameplay 权威状态保持相对 V1 零增量，只替换客户端表现 backend。
- V1/V2 的 CPU/GPU 门按目标 FPS 的帧预算比例收紧；60 Hz 的 V2 `2.0/2.5 ms` 目标/硬门不能用于 144/240 Hz。
- V2 Quality 封顶 `0.52 Mpixel`、`12 steps`、`60 updates/second` 和 `24 MiB`，高端 GPU 不自动换取更多 raymarch 工作。
- 玩家明确选择固定 V1/V2 档位；可选 Auto 只在 V2 内调档，性能超预算不自动切换 backend。只有无法正确渲染的功能故障才恢复到 V1。
- 红外全屏 pass 开启期间暂停 cinematic atmosphere，退出后重置 history，首版不叠加两个后处理 owner。

## Validation

- 阅读并核对 `WhiteCurtainInfo`、`InfraredViewRenderer`、`IrisRenderingPipelineAccess`、两套 `FHShaders` 和 `FHClientEvents` 的现役入口。
- 三份 Markdown 的 code fence 数量均为偶数，无 trailing whitespace；新增相对链接均可解析。
- `git diff --check` 对本次 tracked 文档修改通过。未运行 Java 测试，因为本次没有修改源码或运行时资源。

## Remaining

- V1 和 V2 均未实现。先执行 V1 Phase 0 基线和 handoff gate，再按独立 V2 计划验证 render stage、depth source、GPU/CPU/显存预算和兼容矩阵。
