# 红外真实归属工程计划与内存/GPU探针

- Time: `2026-09-13 17:22:12 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed`
- Scope: `计划和独立性能调查；未实施生产渲染改动`

## Completed

- 用户确认必须彻底解决蓝线，且未启用光影包。写成新的[唯一渲染实施plan](../plans/2026-09-13_17-18-00_infrared-exact-surface-capture.md)，材料主plan指向它，旧显示plan标记superseded。明确旧深度候选仍未通过，不能称新方案已落地。
- 核实实际Embeddium源码：compact 20字节及light高字节布局、VanillaLike 28字节的已占用字段、worker/builder分配、BlockRenderContext真实位置、draw/region/shader入口。核查Oculus API以区分安装与启用，不规划任意光影包通用适配。
- 设计compact原位13位owner编码，保留原两个8位光照通道；高精度选项单独处理和核算。温度在vertex读取，flat整数经同次terrain draw写R16I，避免按深度猜owner。
- 初始MRT方案在4K探针存在回退。进一步删除原RGBA8中转和整屏复制，改用AFTER_LEVEL预乘混合；单张R16I取代原RGBA8，专用屏幕图像容量减半，而不是简单增加一张图像。
- 为避免采样当前draw FBO上挂载的depth，最终混合使用仅引用主颜色的FBO；捕获FBO引用原颜色/depth并附加R16I。两者都不拥有主附件，不增加depth副本。
- 计划写明owner生产链/清理、字段布局、region整数坐标、MRT覆盖、混合公式、网络LAST/资源生命周期、关闭与首次开启成本、旧逻辑删除、分阶段接入和零错误验收。
- Documentation impact: 当前行为文档只增加计划链接，未把MRT写成已实现；既有失败日记不改写。

## Decisions

- 不能只核算屏幕纹理。统一增加4字节顶点会影响native构建暂存、上传副本及GPU；源码初始化容量估算约10.5 MiB额外暂存/worker，因此默认compact保持20字节。
- 单uniform开关复用原program缓存，避免为红外另建shader变体缓存。关闭分支探针成本接近噪声，但不称整个接入关闭成本为零。
- 完整块/楼梯/遮挡严格测试必须全部通过后再切换并删除旧归属路径，不接受低错误比例。当前实验探针只是工程选型依据。

## Validation

- 独立Java17/LWJGL隐藏GL探针：`build/InfraredMrtProbe.java`，最终日志`build/infrared-mrt-probe.log`；不新增依赖，不操作用户游戏。
- 524,288组owner/light编码往返通过；vertex与fragment取温度得到完全相同的R16I；有效覆盖非空；直接混合相对原数学混合的RGB最大差异1个8位色阶、alpha差异0，包含白色前沿和非1的目标alpha。
- RTX4070 Laptop，1080p/1440p/4K，每档1,024与262,144顶点，16轮预热、48轮交错GPU计时。包含地形和完整后处理的最终路径p50分别为0.1075/0.1761、0.1782/0.2509、0.4669/0.5960 ms；对应当前深度路径0.1751/0.2222、0.2929/0.3297、0.6011/0.6871 ms。完整p95与方法记录在plan。
- 上述为合成opaque网格，不代表实际游戏FPS、cutout/楼梯通过、Mixin接入或CPU/native内存测量。关闭uniform分支对照共享解码，不能覆盖新增CPU编码成本。
- 本轮仅改plan、相关文档链接、日记和build临时探针；没有改生产源码，无需重跑服务端热模型测试或编译生产工程。

## Remaining

- 从plan P0开始实施并验证实际Embeddium链路；生产蓝线问题仍未完成修复。
- 实际CPU/native/GPU容量、首次开启与关闭路径、严格归属回归和开发客户端视觉验收均保留为交付门槛。
