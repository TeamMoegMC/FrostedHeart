# 固定整合包红外路线与接入细节收敛

- Time: `2026-09-13 19:13:46 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed`
- Scope: `更新plan与工程验证要求；未修改生产代码`

## Completed

- 将[红外plan](../plans/2026-09-13_17-18-00_infrared-exact-surface-capture.md)固定为Forge最终入口、Embeddium原地形流程、自有功能类和必要小桥接；用户整合包版本基本不变，不再把独立Forge几何或维护后端fork列为当前待选路线。
- 列出3个候选Mixin类、约5个具体锚点和各自允许承担的职责。明确这是待真实P0验证的清单，不是已经通过的注入配方。
- 复查发现Oculus即使光影关闭仍对DefaultChunkRenderer和ChunkBuilder构造调用应用ModifyArg；把实际注入组合验证加入P0，不能屏蔽其他已安装模组的注入来通过测试。
- 补充两份固定type描述、无分配scope进入/恢复、按格式选择writer、normal委托父类program缓存、capture独立释放与缓存key有界等实现细节。
- 补充现有帧入口→原pass绑定→首次捕获prepare/clear→原multiDraw→Forge AFTER_LEVEL的调用顺序；SOLID/CUTOUT之间不重复清温度，无地形帧不读取旧热图，关闭时的自然chunk重建编码成本仍需计量。
- 同步材料主plan的最终决策与进度。Documentation impact: 当前行为未变，living docs不提前改为新方案已实现；本轮只维护计划和追加调查日记。

## Validation

- 对当前依赖的源码/字节码核查CameraTransform、ShaderChunkRenderer、Oculus构造注入及现有IR LAST/AFTER_LEVEL入口。
- `build/InfraredRegionOffsetProbe.java`直接使用当前Embeddium的实际CameraTransform，覆盖正负约3千万坐标、负fraction及各轴±8192区域偏移，共8,755例，region整数恢复错误0。日志`build/infrared-region-offset-probe.log`。该探针不证明实际uniform或桥接已接通。
- 计划链接全部有效，相关Markdown `git diff --check`通过。生产源码未变，没有重跑编译、GPU布局基准或服务端热模型测试。

## Remaining

- 新渲染路径尚未实施，当前生产深度候选的严格边缘测试仍失败。
- 下次先执行P0实际模组组合验证；完整块/楼梯/cutout零归属错误、真实CPU/native/GPU成本与开发客户端视觉验收仍为交付门槛。
