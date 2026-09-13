# 红外接入重新选型：自有类型与最小桥接

- Time: `2026-09-13 18:30:52 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed`
- Scope: `按用户“Mixin只能最后手段”要求重新选型、撤回旧接入草稿、更新工程plan；未完成生产蓝线修复`

## Completed

- 核查`BlockRendererRegistry`、`ChunkMeshEvent/MeshAppender`、`ChunkDataBuiltEvent`的真实能力；没有把注册自定义renderer/追加几何接口冒充默认模型完成回调、属性扩展或MRT接口。
- 选择自有`ChunkVertexType`/encoder及`DefaultChunkRenderer`、`ChunkShaderInterface`子类，保留原网格、排序、culling和批量绘制。普通shader使用原来源/逻辑，FH仅组合自己的capture program。
- 核实当前仍缺创建入口、自有属性绑定、真实方块scope三类接口，目标只在这三处用小桥接。说明public实现类不等于稳定SPI，不承诺升级无耦合或绝对3个Mixin足够。
- 选择显式20字节typed格式：两个8位光照通道+独立16位owner属性。GPU属性描述恢复原光照接口，不改普通shader解码，不改原格式/encoder常量，不修改全局ShaderLoader。
- 撤掉此前5个Mixin草稿、2个辅助Java文件、1个GLSL片段，以及新增注册和plugin分支。先前热模型、网络、原红外shader及AFTER_LEVEL挂点保留。
- 更新[当前plan](../plans/2026-09-13_17-18-00_infrared-exact-surface-capture.md)、材料主plan和当前行为文档的计划说明。旧20字节高位编码/全局shader改动不再是实施指令；历史探针与日记不改写。

## Decisions

- 普通继承实现渲染和uniform行为；桥接只连接缺口，不塞业务算法。`DefaultChunkRenderer.render`显式调用`super.begin`，故capture绑定放到自有`ShaderInterface.setupState`原pass设置之后，不能仅覆写begin。
- 原shader选项按type引用判定高精度格式，wrapper需在自有program创建处映射回base type；不能让它错误走compact分支。
- 单scope桥接使用每线程primitive cursor，避免对多个原builder类注入状态。每vertex的ThreadLocal读取与尾部store有CPU成本，列入真实接入测量，不声称开销零。
- 默认compact保留20字节；高精度采用自有32字节类型和独立属性，原字段不重解释。温度更新/红外开关不引起网格重建。

## Validation

- 新增临时`build/InfraredAttributeLayoutProbe.java`：同一shader/场景轮换20、22、24字节属性布局，32轮预热、96轮交错GPU计时，避免分开运行的频率波动误导选型。
- RTX4070 Laptop，三个分辨率、1,024/262,144顶点。三个布局的RGBA与R16I逐像素完全一致，包含变化的光照值和非空有效覆盖，GL error=0。稠密场景terrain MRT p50：1080p约0.126..0.127 ms，1440p约0.182..0.183 ms，4K约0.399..0.400 ms；未发现值得增加2/4字节stride的优势。
- 日志`build/infrared-attribute-layout-probe.log`；完整后处理typed探针`build/infrared-typed-owner-probe.log`另验证524,288组typed字段往返、温度一致、混合RGB误差≤1和alpha精确。所有这些仍是合成探针，不替代真实桥接、构建CPU或严格楼梯回归。
- Java17 `compileJava processResources --offline --no-daemon --console=plain -Dnet.minecraftforge.gradle.check.certs=false`通过，日志`build/infrared-bridge-reset-compile.log`。检查原Mixin注册/plugin diff已清零，撤回的class/resource不再留在build输出中。未运行无关服务端热模型测试。

## Remaining

- 从plan P0证明自有renderer与三类桥接的真实链路，之后才能切换生产红外并删除旧深度归属算法。
- 真实CPU/native成本、完整块/楼梯/cutout零归属错误、资源生命周期及开发客户端视觉验收尚未完成。
