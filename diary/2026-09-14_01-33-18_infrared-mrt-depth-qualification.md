# MRT深度候选资格与管线对照，保留准确快照

- Time: `2026-09-14 01:33:18 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed`
- Scope: `MRT候选实现/测试、GPU成本对照、正式原生深度回归；生产路径未切换`

## Completed

- 在独立GL实验实现同次输出R16I温度与R32F深度的候选，核对默认主深度实际格式，并用GPU sampler比较原生深度；没有改写主深度来通过测试。
- 默认/D24上原始MRT、普通量化和整数位运算量化均不满足严格相等；D32F原始MRT通过。最终不把候选注册到生产，不新增运行模式或fallback层。
- 在正确的D32F场景比较完整受控GPU路径：包括clear、terrain、copy/MRT及生产最终shader，两种路径RGBA完全相同。MRT单层较快，四层较慢，内存同为约6P bytes，因此不是全面收益。
- 正式GPU harness新增`verifyNativeDepth`，覆盖近深度和量化边界，确保当前深度捕获不把地形误判成实体。原生产精确owner、实体环境取色、4个Mixin和快照逻辑保持。
- Documentation impact: 更新IR plan的资格门槛、实际计时和最终决策；world/runtime文档增加保留快照的依据。未改design、服务端热模型或既有diary。

## Decisions

- 正确性先于“少一次复制”。不加容差、降低精度、改原生depth写入或增加硬件校准来抹掉MRT差异。
- 不为仅D32F且低overdraw的局部优势增加两套生产管线。当前快照路径符合已验证的正确性与低维护成本约束；不将此推论扩展成全局最优证明。

## Validation

- `build/InfraredMrtDepthCheck.java`：每格式33,554,432像素。D24原始MRT差异9,606,912；普通归一化量化25,875,968；整数位运算候选35,840。默认unsized分配实际24-bit normalized，结果与D24一致；D32F原始输出差异0。GPU采样比较与CPU读回比较结论相同，GL error0。
- `build/InfraredDepthPipelineCost.java`：RTX4070 Laptop、80轮交错预热/96轮交错计时。4K单层copy/MRT p50为0.7250/0.5847 ms；4层1.1459/1.2339 ms。1080p和1440p同样单层MRT更快、4层更慢。完整表与范围限制在plan，日志`build/infrared-depth-pipeline-cost.log`。
- 新增正式原生深度回归96场景、12,441,600像素全部通过；原8,640场景、353,123,074分类像素错误0。实体遮挡、自身0℃环境、RGB/alpha、资源与scope测试通过，GL error0。日志`build/infrared-native-depth-validation.log`。
- Java17 `compileGameTestJava --offline --no-daemon --console=plain -Dnet.minecraftforge.gradle.check.certs=false` BUILD SUCCESSFUL；`git diff --check`通过。生产代码没有修改，未重跑整套客户端/服务器。

## Remaining

- 当前候选评估已完成，MRT替换不采用。原计划完整整合包负载、更多资源包/透明介质等范围仍维持原状态；临时GL实验不是完整游戏帧时。
