# Brick变动时保留未变化材料的红外温度

- Time: `2026-09-14 00:20:45 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed`
- Scope: `红外服务端快照、材料温度读取、局部变更过滤及回归`

## Completed

- 定位闪蓝原因：`beginGeometryMutation`使Page的`currentPublication`暂时为空，原IR随后清除整个Page的材料presence并输出field/INVALID；新拓扑发布后恢复，形成错误的“无温度”间隔。
- [MinecraftThermalInput](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/runtime/minecraft/MinecraftThermalInput.java)的IR与`sampleMaterial`复用`lastPublication`，继续通过query topology/slot generation和发布引用检查一致性。真正改变的物体由既有材料journal排除，未改变的同Brick/同Page物体仍然可读。
- [MinecraftPageManager.collectMaterialChangesSince](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/runtime/minecraft/input/MinecraftPageManager.java)一次展开现有journal到调用方共用64-long scratch。只在Page几何revision落后时扫描，新增有效载荷512 bytes及一个primitive mask；没有每Page/每玩家历史、材料副本或客户端缓冲。
- 未提交的替换即使尚未推进IR epoch，也触发受影响Brick的delta；真删除和A→Air→A仍清旧值。Page presence只排除真正失去全部材料的Page，避免无关材料闪蓝。
- 并发读取冲突改为最多两次整份重读，仍冲突则返回无响应，客户端保留已提交基线直至正常再次请求。真实稳定invalid/超龄cut保留独立解析场逻辑。移除无调用的`InfraredBrickCodec.Builder.rewind`。
- Documentation impact: 更新世界温度、runtime、数据生命周期、IR plan及材料主plan。没有修改design、GPU renderer、Mixin、色标或服务端能量/空气通路模型。

## Decisions

- 材料本体身份与空气几何通路有效性分开使用：未改动物体仍拥有其已发布状态；这不授权solver沿失效路线换热。
- 不用客户端淡入淡出或旧物体温度替代未知的新物体。复用已有H/T发布与变更日志，不添加旧模型兼容分支。
- 临时读竞争不代表删除；真实移除仍明确编码INVALID。保持原请求周期和网络格式，避免新增调度和ACK机制。

## Validation

- 新增`ThermalInfraredGameTests.brickMutationKeepsUnchangedMaterialTemperatures`：删除一个材料块后，同Brick与相邻Brick的原材料保持可读；检查温度计、局部delta、完整快照、删除/A-Air-A不继承及wire往返。
- 更新旧`campfireSurfaceUsesMaterialAndLocalAvailability`中错误期望整Page回退的断言，继续验证解析场保底、恢复和真正关闭query publication后的field-only显示。
- 首轮73/73通过；清理无用回滚方法并补齐cursor检查后，最终Java17 `gradlew.bat runGameTestServer --offline --no-daemon --console=plain -Dnet.minecraftforge.gradle.check.certs=false`再次73/73通过，BUILD SUCCESSFUL。最终日志`build/infrared-mutation-final.log`。
- `git diff --check`通过。GPU与协议格式未改，未重复隐藏客户端和8,640 GPU场景测试；本轮验证的是服务器实际产生的温度数据。

## Remaining

- 本次局部变化导致无关材料缺值的问题已修复。被替换的新物体在尚无有效材料状态且无解析场时仍为蓝色占位，这是实际未知而非无关Page被清空。
- 原计划中大型整合包性能和扩展场景验收仍按原状态推进，本次不扩展其范围。
