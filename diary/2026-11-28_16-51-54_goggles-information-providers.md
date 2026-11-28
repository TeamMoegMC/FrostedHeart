# 护目镜覆盖层信息采集职责拆分

- 作者：Codex
- 创建时间：2026-11-28T16:51:54.524237+00:00
- 背景：第一轮只抽出绘制样式，renderOverlay 仍有 203 行混合采集逻辑，未充分完成重构。
- 实现：renderOverlay 缩为 18 行；collectInformation 串联短步骤。新增 GoggleOverlayContext 统一目标/装备快照，CreateOverlayInformation 负责 Create/IE 兼容信息，PlantOverlayInformation 拥有请求节流和读数缓存，HeatNetworkOverlayInformation 拥有热网缓存、请求及分段文本。每个新类有独立职责，避免把大方法整体迁移。
- 扩展：新增上下文信息回调；原有回调、收包方法、热网接口保留兼容。可见性条件、请求顺序/周期、段落间距、零帧插值及默认样式沿用。
- 文档：更新 docs/climate/goggles-overlay.md 的职责和验证说明。
- 验证：离线 compileJava 成功（25 秒，29 条编译警告）；最终植物方法拆分后，通过两次控制桥 runClient 重编译及正常运行退出。git diff --check 通过。
- 实景 A/B：基线 83caafd3d；普通场景热管/真实小麦温度 × GUI 缩放 1/2/3 × 五个淡入采样，30/30 组非空截图 RGB 差异为 0。ModernUI 3.12.0.1 相同矩阵及六组小数鼠标坐标，36/36 组非空截图差异为 0，实际缩放为 2/3（请求 1 被该配置设为 2）。
- 热网适配器：缺失、无效、包含正速率/零速率及非匹配端点、空网络四种状态，完整 Component JSON 与旧实现一致。
- 证据：build/overlay-validation/provider-normal-pixels.txt、provider-modern-pixels.txt；run/agent/heat-text-provider-validation.txt；run/agent/screenshots/ 中 provider-normal/provider-modern 截图及并排图。
- 验证边界：未为多方块、装配错误、列车等其他分支单独搭建游戏内场景；不能将上述截图数量解释为所有兼容路径均已实测。
- 运行设置：客户端已正常退出；恢复测试前 GUI 缩放 0。临时测试源和夹具保留在忽略的 build/overlay-validation，不进入生产源码。
