# 护目镜信息覆盖层

状态：接口、离线强制编译及下述客户端视觉回归已验证。最后核对：2026-11-28。
范围：`TemperatureGoogleRenderer`、目标上下文及信息适配器、`TemperatureTooltipStyle`、`CUIOverlay`。

`TemperatureGoogleRenderer.OVERLAY` 包装具名主层 `TemperatureOverlay.INSTANCE`。结构参考 `KeyHintOverlay`、`TipOverlay` 和 `PopupOverlay`：主层在 `addChildUIElements` 挂载一个持久的 `ReadoutElement`，在 `onBeforeRender` 更新本帧数据及视口，子元素的 `render` 只调用所选样式绘制。不在图层 `drawBackground` 中采集信息或发送请求。该覆盖层不交互、不绘制屏幕背景渐变。
`ClimateClientEvents.suppressCreateGoggleOverlay` 在 Forge 的 `RenderGuiOverlayEvent.Pre` 中取消 Create 原生 `GoggleOverlayRenderer.OVERLAY`（注册 ID 为 `create:goggle_info`），避免与本覆盖层重复显示；其他 Create HUD 不受该取消条件影响。

内置信息采集及网络请求保持原有顺序；`registerInformationProvider` 可在内置信息之后追加文本，回调接收代理后的方块位置和可修改的文本列表。注册与样式切换须在客户端线程进行，提供器应自行判断装备与目标条件。

信息采集职责（2026-11-28 第二轮拆分）：

| 代码锚点 | 职责 |
|---|---|
| `TemperatureOverlay.onBeforeRender` | 按 CUI 生命周期调用采集器，更新持久读数组件的内容和视口 |
| `TemperatureOverlay.ReadoutElement` | 内容为空时隐藏；保留准确的本帧插值（包括零）；在子元素 render 中绘制所选样式 |
| `TemperatureGoogleRenderer.collectFrameInformation` | 可见性判断、淡入计数和信息采集；原 203 行混合入口已分离 |
| `TemperatureGoogleRenderer.renderOverlay` | 保留给绕过 CUI 适配器直接调用的兼容入口 |
| `TemperatureGoogleRenderer.collectInformation` | 按既有顺序串联提供器和扩展回调，保留隐藏条件 |
| `GoggleOverlayContext.at` | 一帧内统一解析代理目标、方块/多方块状态、装备和植物类型 |
| `CreateOverlayInformation` | 分别适配护目镜/悬浮信息、装配错误/列车信息、活塞长度；`Readout` 保存单帧文本和原有可见性标志 |
| `PlantOverlayInformation` | 拥有弱引用世界和目标缓存；`updateTarget` 管理 20 tick 请求节流及目标切换，`readout` 选择服务端植物/树苗数据，`append` 处理段落间距 |
| `HeatNetworkOverlayInformation` | 拥有热网缓存及原有逐帧请求策略，分方法拼装网络摘要和端点速率 |

新增 `registerContextInformationProvider(BiConsumer<GoggleOverlayContext, List<Component>>)`，扩展信息可读取同一份目标及装备快照；方块实体和多方块状态可能为空。旧的 `registerInformationProvider` 适配到同一回调列表，注册顺序和调用阶段不变。现有收包入口、热网 getter/setter 和 `addHeatNetworkInfoToTooltip` 保留兼容转发。

`setOverlayStyle` 替换后续帧的绘制策略；默认 `TemperatureTooltipStyle` 保留原有文本测量、边界限制、Create 配置颜色、24 帧淡入及 ModernUI 鼠标定位兼容逻辑。样式接收文本、帧插值和窗口尺寸，可自行实现布局。

验证（2026-11-28）：`./gradlew compileJava --offline --rerun-tasks --console=plain` 成功，退出码 0，4 个任务实际执行，29 条编译警告。默认绘制方法体与修改前一致（仅限定 hoverTicks 所属类）。

通过原有控制桥进入 `overlay-visual-regression` 世界，佩戴护目镜，分别查看热管及手持土壤温度计查看真实小麦温度。原版来自基线 `83caafd3d`，临时夹具将双方放在同一帧状态下独立绘制到等尺寸缓冲；植物缓存来自相同的真实服务端读数。比较最终 RGB 像素，并检查截图非空。

- 普通场景：实际 GUI 缩放 1/2/3，每种目标采样 hoverTicks 输入 0/5/11/23/24（方法内部仍执行原有自增，partialTicks=0），30/30 组非空截图差异像素为 0。
- ModernUI 3.12.0.1：通过临时本地 Maven 配置交由 ForgeGradle 映射到开发环境，避免直接加载发行 JAR 的 Mixin 名称不匹配。相同矩阵 30 组，加两类目标各 3 组小数鼠标坐标，共 36/36 组非空截图差异像素为 0。开启 exactPositioning，鼠标横坐标 120.125/120.375/120.625 下，每种目标边界均保持一致。该 ModernUI 配置将请求缩放 1 设为实际 2，因此本轮实际覆盖缩放 2/3。
- CUI 的 `UIElement.updateRenderInfo` 忽略零插值；该覆盖层单独保存当前帧的准确插值，避免淡入沿用旧值。没有修改通用 CUIOverlay 绘图状态。

报告：`build/overlay-validation/normal-zero-fixed-pixels.txt`、`build/overlay-validation/modern-zero-fixed-pixels.txt`；截图：`run/agent/screenshots/*zero-fixed*`。这些运行产物不进入生产源码。当前结论限于上述目标、默认样式和测试配置；不代表所有模组组合或其他自定义样式均已测试。

以上 zero-fixed 报告为第一轮重构的历史验证。第二轮职责拆分在保留原生 Create 覆盖层禁用逻辑的情况下重新实测：普通场景 30/30、ModernUI 36/36 组非空截图 RGB 差异为 0，测试范围和 ModernUI 实际缩放限制同上。

本轮证据：`build/overlay-validation/provider-normal-pixels.txt`、`provider-modern-pixels.txt`；截图 `run/agent/screenshots/provider-*-comparison.png` 及 `*-provider-*-temperature-live.png`。热网文本另外比较了缺失、无效、包含正值/零值/非匹配端点、空网络四种状态的完整 Component JSON，均与旧实现一致；结果在 `run/agent/heat-text-provider-validation.txt`。本轮离线编译及两个控制桥客户端运行成功退出。多方块、装配错误和列车等其他兼容分支未分别建立游戏内截图场景。

第三轮（参考现有 CUIOverlay 实现）：离线 compileJava 成功（24 秒，退出码 0）；改为具名主层和持久子元素后，普通场景 30/30、ModernUI 36/36 组非空 A/B 截图 RGB 差异仍为 0，热网文本四种状态的 Component JSON 检查继续通过。测试范围及 ModernUI 实际缩放限制沿用上文。

本轮独立证据：`build/overlay-validation/cui-normal-pixels.txt`、`cui-modern-pixels.txt`；`run/agent/screenshots/cui-*-comparison.png` 和 `*-cui-*-temperature-live.png`。两个控制桥客户端均正常退出。前面 provider-* 报告属于信息提供器拆分阶段，cui-* 报告验证后续 CUI 生命周期调整，二者不混用。
