# 参考现有 CUIOverlay 的温度覆盖层结构

- 作者：Codex
- 时间：2026-11-28T17:03:06.596595+00:00
- 参考：KeyHintOverlay、TipOverlay、PopupOverlay、DialogueOverlay 的具名 PrimaryLayer、子元素及生命周期用法。
- 调整：新增 TemperatureOverlay.INSTANCE，持久 ReadoutElement 在 addChildUIElements 中挂载；onBeforeRender 更新数据和视口，子元素 render 仅绘制。撤去 TemperatureGoogleRenderer 中匿名 PrimaryLayer.drawBackground 串联整个流程的用法。
- 兼容：保留原有 renderOverlay 直接绘制入口；默认样式及已拆分的信息提供器沿用，零帧插值由读数组件准确保存。
- 结构取舍：读数组件提供完整窗口视口，供不同样式自行布局，保留默认样式的 Create 配置位置和边界限制。每帧更新内容，不重建组件树。
- 验证：离线 compileJava 成功，退出码 0，24 秒，29 条编译警告；git diff --check 通过。普通场景 30/30 和 ModernUI 36/36 组非空截图 RGB 差异为 0；热网文本四种状态 Component JSON 继续一致。两次 runClient 正常退出。
- 范围：热管及真实小麦温度，五个淡入采样。普通实际缩放 1/2/3；ModernUI 实际缩放 2/3（其配置将请求 1 设为 2），另测小数鼠标坐标。未对所有其他兼容分支单独搭建场景。
- 文档：更新 docs/climate/goggles-overlay.md，区分 provider-* 与 cui-* 两轮报告。
- 证据：build/overlay-validation/cui-normal-pixels.txt、cui-modern-pixels.txt；run/agent/screenshots/ 中 cui-normal/cui-modern 截图。
- 测试结束：恢复运行目录 GUI 缩放 0。临时夹具及截图保留在忽略目录，不进入生产源码。
