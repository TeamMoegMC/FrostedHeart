# 护目镜信息覆盖层

状态：接口、离线强制编译及下述客户端视觉回归已验证。最后核对：2026-11-28。
范围：`TemperatureGoogleRenderer`、`TemperatureTooltipStyle`、`CUIOverlay`。

`TemperatureGoogleRenderer.OVERLAY` 使用非交互的 `CUIOverlay` 和无背景渐变的 `PrimaryLayer`，在图层绘制阶段采集和显示信息。
`ClimateClientEvents.suppressCreateGoggleOverlay` 在 Forge 的 `RenderGuiOverlayEvent.Pre` 中取消 Create 原生 `GoggleOverlayRenderer.OVERLAY`（注册 ID 为 `create:goggle_info`），避免与本覆盖层重复显示；其他 Create HUD 不受该取消条件影响。

内置信息采集及网络请求保持原有顺序；`registerInformationProvider` 可在内置信息之后追加文本，回调接收代理后的方块位置和可修改的文本列表。注册与样式切换须在客户端线程进行，提供器应自行判断装备与目标条件。

`setOverlayStyle` 替换后续帧的绘制策略；默认 `TemperatureTooltipStyle` 保留原有文本测量、边界限制、Create 配置颜色、24 帧淡入及 ModernUI 鼠标定位兼容逻辑。样式接收文本、帧插值和窗口尺寸，可自行实现布局。

验证（2026-11-28）：`./gradlew compileJava --offline --rerun-tasks --console=plain` 成功，退出码 0，4 个任务实际执行，29 条编译警告。默认绘制方法体与修改前一致（仅限定 hoverTicks 所属类）。

通过原有控制桥进入 `overlay-visual-regression` 世界，佩戴护目镜，分别查看热管及手持土壤温度计查看真实小麦温度。原版来自基线 `83caafd3d`，临时夹具将双方放在同一帧状态下独立绘制到等尺寸缓冲；植物缓存来自相同的真实服务端读数。比较最终 RGB 像素，并检查截图非空。

- 普通场景：实际 GUI 缩放 1/2/3，每种目标采样 hoverTicks 输入 0/5/11/23/24（方法内部仍执行原有自增，partialTicks=0），30/30 组非空截图差异像素为 0。
- ModernUI 3.12.0.1：通过临时本地 Maven 配置交由 ForgeGradle 映射到开发环境，避免直接加载发行 JAR 的 Mixin 名称不匹配。相同矩阵 30 组，加两类目标各 3 组小数鼠标坐标，共 36/36 组非空截图差异像素为 0。开启 exactPositioning，鼠标横坐标 120.125/120.375/120.625 下，每种目标边界均保持一致。该 ModernUI 配置将请求缩放 1 设为实际 2，因此本轮实际覆盖缩放 2/3。
- CUI 的 `UIElement.updateRenderInfo` 忽略零插值；该覆盖层单独保存当前帧的准确插值，避免淡入沿用旧值。没有修改通用 CUIOverlay 绘图状态。

报告：`build/overlay-validation/normal-zero-fixed-pixels.txt`、`build/overlay-validation/modern-zero-fixed-pixels.txt`；截图：`run/agent/screenshots/*zero-fixed*`。这些运行产物不进入生产源码。当前结论限于上述目标、默认样式和测试配置；不代表所有模组组合或其他自定义样式均已测试。

上述 66 组截图比较完成于禁用原生 Create 覆盖层之前，用于验证温度覆盖层重构。禁用原生层的后续改动仅做离线编译及代码核对，尚未重新启动客户端截图。
