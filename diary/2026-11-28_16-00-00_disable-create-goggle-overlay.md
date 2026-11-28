# 禁用 Create 原生护目镜 HUD

- 作者：Codex
- 日期：2026-11-28 UTC
- 需求：禁用机械动力原生 goggle 渲染，保留温度 CUI 覆盖层。
- 实现：复用 ClimateClientEvents，以 HIGHEST 优先级取消 RenderGuiOverlayEvent.Pre 中 overlay 实例等于 GoggleOverlayRenderer.OVERLAY 的绘制。核对本地 Create 0.5.1.j-55 注册代码，原生 ID 为 create:goggle_info。
- 清理：移除 TemperatureGoogleRenderer 中已完成的禁用原生层 TODO。
- 文档：更新 docs/climate/goggles-overlay.md，区分此前重构截图验证与此次禁用原生层的验证范围。
- 验证：`./gradlew compileJava --offline --console=plain` 成功，退出码 0，耗时 26 秒，29 条编译警告；`git diff --check` 通过。未启动客户端截图。
