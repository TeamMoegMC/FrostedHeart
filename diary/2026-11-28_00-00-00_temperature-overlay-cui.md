# 护目镜覆盖层 CUI 重构

- 作者：Codex
- 日期：2026-11-28 UTC
- 基线：先快进拉取至 83caafd3d。
- 实现：复用 CUIOverlay/PrimaryLayer，拆出 TemperatureTooltipStyle；新增信息追加和样式替换接口。新样式类仅负责原有绘制逻辑，避免继续混合采集和呈现。
- 文档：新增 docs/climate/goggles-overlay.md 描述接口与验证边界。
- 验证：compileJava --offline 成功（29 条已有编译警告）；git diff --check 通过；默认绘制方法体与 HEAD 原实现比较一致。
- 未完成验证：未启动客户端，不能确认 CUI 管线引入后像素输出完全不变；需补客户端视觉回归。

## 客户端验证阶段记录

- 已通过控制桥创建 overlay-visual-regression 测试世界，佩戴护目镜；热管及手持土壤温度计查看小麦的实景截图均已生成。
- 普通场景：两类目标 × GUI 缩放 1/2/3 × 完整显示的两档采样，共 12 组 RGB 像素差异为零；部分淡入采样仍有差异，不能宣称全部通过。
- 定位：UIElement.updateRenderInfo 忽略零 partialTick，CUI 图层读取到旧插值。TemperatureGoogleRenderer 已改为保留当前帧的准确插值；此前试加的 CUI 绘图准备钩子已撤回，CUIOverlay 无修改。
- 修正后 compileJava 进程退出码 143，未获得成功结果；待客户端启动重编译及复测。
- ModernUI 3.12.0.1 已下载，已发起带桥客户端启动，尚无兼容性通过结论。
- 证据：run/agent/screenshots/normal-fixed-*-comparison.png；build/overlay-validation/normal-checkpoint-pixels.txt。临时夹具及原版对照均位于忽略的 build/overlay-validation/ 下，不进入生产源码。

## 最终复测结果（2026-11-28 15:54 UTC）

- 上述待验证事项已按以下范围完成：强制离线 compileJava 成功，退出码 0，耗时 27 秒，4 个任务实际执行。
- 普通场景：热管/真实小麦温度 × 实际缩放 1/2/3 × 五个淡入采样，30/30 组非空截图 RGB 差异像素为 0。
- ModernUI 3.12.0.1：发行 JAR 直接启动因 SRG/MCP Mixin 名称不匹配失败，改用临时本地 Maven + fg.deobf 后启动及退出成功。相同矩阵 30 组与额外鼠标坐标 6 组，36/36 组非空截图 RGB 差异像素为 0。该配置请求缩放 1 的实际值为 2，实际覆盖缩放 2/3。
- exactPositioning=true，横坐标 120.125/120.375/120.625 时，两种目标各自覆盖层边界均稳定。
- 最终修正仅在 TemperatureGoogleRenderer 保存准确帧插值；CUIOverlay 已撤回试验修改。
- 文档已更新 docs/climate/goggles-overlay.md，说明方法、证据及实际覆盖范围。
- 最终报告：build/overlay-validation/normal-zero-fixed-pixels.txt 与 modern-zero-fixed-pixels.txt；实景和并排截图在 run/agent/screenshots/，前缀包含 zero-fixed。
- 测试客户端已正常退出；测试前 guiScale=0，将恢复该设置。临时 ModernUI 使用显式 init 脚本加载，不影响普通启动的依赖配置。
