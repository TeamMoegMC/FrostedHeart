# 实体遮挡背景热图，并以自身位置融入环境

- Time: `2026-09-14 01:02:00 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed`
- Scope: `红外实体遮挡、环境显示近似、深度资源及客户端/GPU验证`

## Completed

- 修复实体被背景热纹理投影而看似透明的问题。保留AFTER_LEVEL，在原SOLID/CUTOUT地形完成后保存实际深度；最终深度不同的像素不再读取背景热图。
- 按用户“最低成本融入环境”要求，遮挡像素读取自身可见位置对应的既有144³显示数据，可复用解析场，缺值仍用蓝色基底。没有加入真实Air同步、脚下搜索、体温状态或逐实体缓存；方块仍使用原精确owner。
- 修改集中于既有`InfraredSurfaceTarget`、`InfraredChunkRenderer`、`InfraredViewRenderer`和最终shader。新增一张原精度深度快照及帧标志；4个生产Mixin和20/32-byte顶点格式不变。无额外实体/地形draw，无CPU整屏读回。
- Documentation impact: 更新世界温度、runtime文档和IR plan，移除当前语义中的“实体沿用背后热图”，明确6P预算、环境近似及测试范围。未编辑design或服务端热行为。

## Decisions

- 使用原精度GPU深度复制，而非压缩depth或容差判断；避免把旧边缘猜测问题引入遮挡判断。GL3.3复制路径在所测硬件比CopyImage对照更好或相近，不引入额外能力分支。
- 当前6P-byte自有图像由2P温度+约4P深度组成。额外深度为1080p约7.91 MiB、1440p14.06 MiB、4K31.64 MiB；这是真实新增成本，早期2P方案漏掉了遮挡信息。所有屏幕资源在缩圈完成/退出时释放。
- 环境取色复用现有3D图和逆矩阵，仅为遮挡像素增加至多一次取值，无网络增量。它是环境显示近似，不宣称读取了实际空气或实体体温。

## Validation

- Java17 `gradlew.bat runClient -PinfraredValidation --offline --no-daemon --console=plain -Dnet.minecraftforge.gradle.check.certs=false`最终BUILD SUCCESSFUL，结果文件PASS。日志`build/infrared-environment-client.log`。
- 实际客户端含前景盔甲架、冷热背景与实体位置0℃数据；37,246覆盖像素（含1,376实体像素）RGB误差≤1、alpha精确。用与实现独立的场景距离阈值区分实体/背景；实体按本地0℃取色。查看了最终截图`build/infrared-client-stage-6.png`。
- 两种顶点格式、resize、资源重载、关闭释放、重开、shader缓存/viewport/scissor恢复通过。
- 8,640个生产encoder/属性/GLSL场景、353,123,074分类像素错误0，GL error0。新增遮挡六档背景温度及本地0℃取色断言通过；新增depth纹理随资源销毁，借用main纹理继续存活。
- 临时复制成本探针使用非恒定depth，120次预热、96轮交错次序，仅测GPU复制：当前路径1080p/1440p/4K p50为0.0297/0.0481/0.2447 ms，p95为0.0328/0.0625/0.2970 ms。CopyImage对照0.0430/0.0737/0.2724 ms及0.0430/0.0819/0.2918 ms；无GL错误。探针/日志在build中，不进入生产代码，不能当完整游戏帧时。
- `git diff --check`通过；服务端行为未改，未重复73项服务端GameTest。

## Remaining

- 不写depth的透明介质/粒子独立遮挡与热成像仍不覆盖；没有有效环境显示数据时仍使用蓝色基底。
- 完整整合包帧时和真实能量塔组合视觉验收保留在原plan，本次局部测试不替代这些测量。
