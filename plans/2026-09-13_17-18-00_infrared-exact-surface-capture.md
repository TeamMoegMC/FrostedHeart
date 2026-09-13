# 红外蓝线根治：自有渲染扩展、显式归属与最小桥接

- Time: `2026-09-13 17:18:00 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Updated: `2026-09-14 01:33:18 +08:00`
- Status: `in-progress`
- Implementation status: `代码替换与本轮自动化验收已完成；P4完整场景CPU/native/GPU对照及扩展游戏场景验收尚未完成，因此总plan不标全量completed。`
- Scope: `固定整合包版本、Embeddium正常地形管线、Oculus安装但未启用光影包；Forge AFTER_LEVEL最终显示；准确捕获普通terrain方块表面温度`
- Outcome: `自有格式、encoder、原renderer子类、4个小桥接、R16I同次地形捕获及AFTER_LEVEL直接混合已实施。旧深度猜测和RGBA8中转已删除。实际客户端两种格式、模型scope、resize、资源重载、关闭释放和重开全部通过；37,246实际表面像素最终RGB误差≤1、alpha精确保留。严格GPU归属8,640场景、353,123,074分类像素错误0，GL error 0；详细证据与剩余项见第15节。`
- Related: [材料主plan](2026-09-12_23-53-20_thermal-material-enthalpy-lifecycle-repair.md)、[显示数据plan](2026-09-08_21-58-58_thermal-unknown-block-ventilation-75.md)、[当前行为](../docs/climate/world-climate-and-temperature.md)、[runtime](../docs/climate/thermal-runtime-architecture-and-optimization.md)。

## 1. 最终决定与交付边界

最终选型为**Forge最终显示入口 + Embeddium既有地形流程 + 自有功能类 + 必要的小桥接**。用户明确整合包版本基本不变；本次不维护Embeddium fork、不建设跨版本SPI、不转向独立Forge几何渲染。Forge独立绘制不是做不到，但会新增几何/更新/覆盖一致性的职责，尚无性能证据支持为本任务付出这些成本。不要在下一轮重新把这几条路线当成未决选项。

用户已明确要求彻底修复，撤销“接受少量归属误差”的选项。停止为深度猜测增加偏移、法线或邻温度补洞。最终生产路径只有一条：

```text
构建当前方块的地形网格
  → 写入真实所属方块的Section内整数坐标
  → 顶点shader按所属方块读取现有144³显示温度纹理
  → flat整数温度随原地形绘制写入R16I屏幕缓冲
  → AFTER_LEVEL直接混合热色到主颜色附件
```

删除原RGBA8中间画面及其颜色复制。自有紧凑格式仍20字节，光照由两个明确的8位属性分量承载，owner是独立16位属性；普通shader不增加解码补丁。保留原地形批量绘制和网格管理，后处理draw从2次降到1次。2026-09-14补齐实体遮挡后，当前自有图像为2P温度+约4P原精度深度快照，总6P bytes；此前2P方案遗漏了遮挡信息，不能继续当作当前预算。

维护性是本版的明确约束：不改全局ShaderLoader，不修改Embeddium的COMPACT/VANILLA_LIKE常量或原encoder，不往普通光照值里隐藏owner再要求普通shader剥离。普通场景使用原shader来源与逻辑；FH只拥有自己的格式、encoder、红外program和帧资源。

保持已有材料H→T、存档、网络增量、解析场合成和144³温度纹理。楼梯各面仍读取同一块的材料温度，气隙没有温度状态。真实缺值继续蓝色占位，不能用消灭所有蓝像素代替归属正确性。

本轮覆盖原有普通terrain solid/cutout范围，包括楼梯、台阶、雪层、围墙、植物偏移模型和普通BakedModel。实体按自身位置的既有显示值融入环境，不读取被遮挡的背景热图，不新增体温或真实Air同步。透明介质/流体的独立热成像、特殊BE的独立热状态、多物体模型的额外语义及启用光影包后的HDR/TAA/多目标管线不在本轮。Oculus已安装但未启用光影包必须正常工作；不为任意光影包建立通用适配框架。

## 2. 已核实的源码与失败基线

下表和旧错误计数记录实施前基线；当前结果以第15节为准。

| 锚点 | 核实结果及工程含义 |
|---|---|
| `gradle.properties` / `build.gradle` | 当前依赖Embeddium `0.3.31-beta.53+mc1.20.1`；Oculus `1.20.1-1.7.0`。这是核查版本，不新增版本锁、文件hash或工程副本。 |
| `run/config/embeddium-options.json` | 开发配置启用compact；用户明确未启用光影包。不能把安装Oculus等同于启用其渲染管线。 |
| `CompactChunkVertex` | stride=20；position/material/section占0..7，color占8..11，UV占12..15，light占16..19。 |
| `VanillaLikeChunkVertex` | stride=28；offset24的uint已经同时打包material、section和两个8位light通道，不能照搬compact空位。 |
| `VanillaLikeChunkVertex.encodeLight` | 当前有效block/sky光照分别取原light的低8位与bits16..23，为自有格式声明两个8位光照通道提供依据。 |
| `ChunkBuilderMeshingTask.execute` | 普通模型经`context.update`后调用`BlockRenderer.renderModel(context,buffers)`；流体另有入口；MeshAppender在块循环之后。 |
| `BlockRenderContext.pos()` | 就是被绘制方块的真实world BlockPos，不需要从顶点或面中心推测。 |
| `ChunkBuildBuffers` / `BakedChunkModelBuilder.begin` | 共用worker构建缓冲；各pass/facing缓冲在init时启动，顶点stride会影响CPU native暂存、上传副本与GPU，而不只是GPU。 |
| `ShaderChunkRenderer.begin/end/createShader` | 原program缓存、shader构建和pass绑定入口可以复用；无需另建地形渲染器。 |
| `DefaultChunkRenderer.setModelMatrixUniforms` | 已按region设置模型偏移，可在同处提供整数region→温度窗口偏移，不增加第二次可见region遍历。 |
| `block_layer_opaque.vsh/.fsh` / `chunk_vertex.glsl` | `_draw_id`提供region内Section坐标；原fragment按material alpha cutoff执行discard。温度必须跟随同一discard与depth。 |
| `InfraredViewRenderer.updateData` | render thread上只有LAST提交origin/epoch并上传纹理；显示读取不能改回`materialReadable`控制。 |
| `getOrCreateOverlayTarget` | 当前额外创建无depth的RGBA8 `TextureTarget`，随后把它整屏复制回main。该分配和复制可以一并删除。 |
| LDLib `ShaderManager` / `ShaderProgram` | `renderFullImageInFramebuffer`必定绑定传入target；`ShaderProgram.use/release/delete`可独立使用。新混合不应伪造拥有main附件的TextureTarget。 |

当前严格GPU基线：1,440平面场景错误0；720完整块场景缺值310、错材料350；720楼梯场景缺值858、错材料1,040。完整测试仍失败。不能把这些错误全部解释为“不可避免”，也不能保留宽松阈值作为新方案验收。

### 2.1 公开接口核查与选型

| 方式 | 核查结果 | 选择 |
|---|---|---|
| `BlockRendererRegistry` | 提供自定义block renderer及PASS/OVERRIDE；不提供包围默认模型输出的完成/异常退出回调，也不提供MRT输出接口。用回调副作用冒充完整scope会把owner泄漏给fluid/appender。 | 不接管全部方块重画，也不把它当作不存在的scope API。 |
| `ChunkMeshEvent` / `MeshAppender` | 用于追加几何；`ChunkDataBuiltEvent`只提供Section信息builder，不能装饰全部既有顶点或注册地形renderer。 | 不为规避Mixin复制一套IR几何、网格或每帧draw。 |
| Forge事件独立绘制IR模型 | Forge事件能提供执行时点，不能直接提供Embeddium已绘出的逐像素owner；独立模型流程还需自行维护几何、更新与覆盖。 | 当前不采用；不把未经实测的方案说成一定更快或一定更慢。 |
| 修改/维护Embeddium fork | 能补正式扩展接口，但需要维护、构建和分发另一份后端。 | 当前不引入fork；未来上游有正式入口可替换下述桥接。 |
| 多处Mixin改原字段/encoder/全局shader | 上版实现方向；外部类行为改动分散，光照编码与全局解码耦合。 | 撤回。不能以GPU耗时好代替维护性评估。 |
| 自有类型/子类 + 缺口桥接 | `ChunkVertexType`、`ChunkVertexEncoder`、`GlVertexAttribute`等public类型可直接使用；`DefaultChunkRenderer.render/compileProgram`与`ChunkShaderInterface.setupState/setRegionOffset`可覆写。 | 选用。继续调用原批量绘制，不复制其算法。 |

这些public类位于后端实现包，并非上游承诺长期稳定的SPI。维护收益是把版本耦合集中在一个适配包和少数入口，不能宣称以后升级无需测试。

源码确认还缺3个桥接边界，作为最后手段：

1. **创建入口**：`RenderSectionManager`构造时硬编码选vertexType并`new DefaultChunkRenderer`，没有注册API。`RenderRegion.DeviceResources`也单独从原常量选择GPU arena stride，必须一并替换其格式读取。小桥接统一选择自有type/renderer，并把同一type传给原ChunkBuilder与GPU arena。固定factory/字段读取锚点，避免局部变量序号依赖。
2. **自有属性绑定**：`DefaultChunkRenderer.getBindingsForType`为private，只识别两个原type的引用，其它返回null。桥接仅在自有type时返回它的5个attribute binding；其它类型沿原逻辑。不能因此覆写draw循环。
3. **方块scope**：包围`BlockRenderer.renderModel(context,buffers)`以设置/恢复真实owner；公开registry没有相应finally边界。只转发自有scope，原renderer照常执行。

原目标3个小Mixin类在P0高精度测试中发现GPU arena入口遗漏，最终为4个类。第四类只替换arena的一处高精度格式读取；原类在内部直接new资源，没有可覆写工厂。保留单目标、必须命中的明确锚点，比把多目标塞进同一类并放宽匹配更易维护。它们只做连接，不包含编码、shader、FBO或热学算法。

### 2.2 固定版本的具体桥接清单

以下4个桥接类已实施，针对4个目标类、6个实际位置（manager创建3处、arena 1处、绑定1处、scope 1处），全部仅在客户端加载：

| 自有桥接类 | 候选锚点 | 只允许做的事 |
|---|---|---|
| `InfraredRendererFactoryMixin` | `RenderSectionManager.<init>`两处格式读取、一处`new DefaultChunkRenderer` | 统一manager/renderer/ChunkBuilder自有type。priority 900使Oculus已有构造ModifyArg先应用。 |
| `InfraredArenaFormatMixin` | `RenderRegion.DeviceResources.<init>`的`VANILLA_LIKE`读取 | GPU arena高精度步长使用32；compact本就是20且其读取已由Oculus占用，完全不拦截该位置。 |
| `InfraredVertexBindingsMixin` | `DefaultChunkRenderer.getBindingsForType()`一处 | 仅为自有type返回5个明确attribute binding；其它type保留原返回路径。 |
| `InfraredBlockScopeMixin` | `ChunkBuilderMeshingTask.execute`中`BlockRenderer.renderModel(context,buffers)`调用一处 | 仅为自有type进入/恢复真实方块scope，并调用原方法。 |

不注入encoder热循环、原format静态字段、全局ShaderLoader、shader interface、region循环或每帧draw body。前5个草稿Mixin及辅助代码已撤销；不得按旧草稿重新铺开。

**实际模组组合的约束：** Oculus的`vertex_format.MixinRenderSectionManager`对DefaultChunkRenderer和ChunkBuilder构造调用施加`ModifyArg`；`MixinRenderRegionArenas`占用arena的COMPACT读取。最终factory priority 900在原构造适配之后应用，arena只拦截未被占用的VANILLA_LIKE读取。没有屏蔽任何Oculus原Mixin，也没有放宽必需注入的匹配要求。

P0记录最终匹配的锚点、每个桥接的实际调用次数与type一致性；优先已有构造/字段符号，不凭局部变量序号猜位置。若组合验证表明构造替换不可组合，就在这一创建边界内解决并更新清单，不能扩散成另一个渲染后端或静默退回深度猜测。

## 3. 方块归属编码：独立属性、明确格式

### 3.1 统一整数含义

```text
local = (x & 15) | ((z & 15) << 4) | ((y & 15) << 8)  // 0..4095
ownerWord = 0x1000 | local                              // bits0..12
ownerWord = 0                                          // 无所属方块
```

`ownerWord`只表示几何所属位置，不是注册表ID、材料参数ID、温度、窗口内texel编号或会变化的句柄。有效标志单列，Section内(0,0,0)必须有效。负world坐标用同一`&15`关系。每个quad四个顶点使用同一word；flat插值及任意三角索引顺序都会得到相同归属。

### 3.2 自有Compact格式：20字节，普通shader不变

创建`OwnedChunkVertexType`，不修改原`CompactChunkVertex`。前16字节沿原定义；尾部显式布局为：

| offset | 字段 | GPU属性声明 |
|---|---|---|
| 16 | block light，unsigned byte | 原`a_LightCoord`的第1分量 |
| 17 | sky light，unsigned byte | 原`a_LightCoord`的第2分量 |
| 18..19 | ownerWord，unsigned short | 独立`fhOwner`整数属性 |

原light语义本就是两个有效8位通道；自有格式把它们紧凑排列，声明`LIGHT_TEXTURE = UNSIGNED_BYTE × 2, integer=true, offset16, stride20`。GPU按attribute描述将其转换为原shader的`ivec2 a_LightCoord`，无需掩码、位移或修改普通shader。

自有encoder复用base encoder写前16字节和其它原内容，尾部明确写：

```java
int lightBytes = (vertex.light & 0xff) | ((vertex.light >>> 8) & 0xff00);
memPutInt(ptr + 16, lightBytes | (ownerWord << 16));
```

owner用`new GlVertexAttribute(UNSIGNED_SHORT, 1, false, 18, 20, true)`单独绑定到FH属性位置4，不加入或修改`ChunkMeshAttribute`枚举。`GlVertexFormat`已有4个属性的stride设为20；独立owner binding覆盖尾部。原位置、UV、颜色、material/section及其解码不变。

这是自有format的存储布局变化，不是“完全不碰数据格式”；维护优势是producer/descriptor成对定义，正常shader收到的光照值不变，owner有独立类型和地址。独立探针已验证524,288组typed字段往返及GPU整数属性读取。P0仍须验证当前模组输出的light语义与普通画面一致；不能丢弃真实光照信息来凑20字节。

`OwnedChunkVertexType`只创建两份固定格式描述，按base type复用，不按方块、任务或开关创建新type。保留base的position scale/offset、texture scale和前16字节编码。四个原属性的offset、类型与normalized/integer标志逐项列入测试；compact唯一改变的原属性是明确缩紧的light存储。第5个owner binding使用unsigned-short、integer=true、normalized=false。绑定描述与encoder必须属于同一type对象。

### 3.3 已有高精度选项：单独核算，不强制用户切配置

高精度自有type采用32字节布局：保留base的0..27原内容，offset28写ownerWord，剩余2字节为padding。原`LIGHT_TEXTURE`仍是offset24的单个unsigned int，原shader的draw/light解码完全不变；owner通过独立unsigned-short属性读取。编码推进量与format stride一起设为32。不修改原`VanillaLikeChunkVertex`类或其静态format。

这不是默认compact的开销：高精度选项会增加4字节/顶点，必须单列测量。保持两种已有格式的正确性，不引入按红外开关重建网格的动态格式切换。启用光影包的XHFP格式不能套这两种布局；该管线在本轮范围外，禁止对它写固定offset。

## 4. CPU接入：随原网格构建携带，不逐帧找方块

1. 自有type的`getEncoder()`提供普通Java encoder，委托原encoder并写自己定义的尾部。删除对`ChunkMeshBufferBuilder.push`、`ChunkBuildBuffers`字段和原encoder的Mixin。
2. 小scope桥接只在`buffers.getVertexType()`是自有type时工作：从`context.pos()`算owner，进入自有`BlockOwnerScope`，调用原renderer，finally恢复之前word。普通、Forge fallback及FRAPI都沿原调用；其它后端不进入该scope。
3. scope使用每线程一个可变整数cursor，ThreadLocal只初始化一次；不把int逐方块装箱，不保存world/BlockPos/任务引用，不创建每顶点对象。初始word=0，嵌套调用与异常均恢复；fluid/appender输出不继承上一块。
4. encoder从当前线程cursor读word。首版允许一次ThreadLocal读取/顶点及一次额外尾部store，明确计入CPU测试。不能在未经证明的线程归属下把首次线程cursor永久缓存到可能被别的线程使用的encoder。
5. worker、网格合并、排序、上传和GPU区域分配仍由原后端管理。温度改变与红外开关不引发网格重建。只有实际CPU profile证明cursor读取是热点，才改为已验证的worker持有方式；不预先增加多处builder注入。

这项取舍用少量构建期读取换取更少外部类改动。GPU探针不测这个CPU成本，实际构建p50/p95、分配和取消/复用必须通过P0/P4验证。

实现使用`int previous = enter(x,y,z); try { 原renderModel调用; } finally { restore(previous); }`；enter/restore操作同一线程cursor。不为scope创建Runnable、Consumer、AutoCloseable或装箱Integer。`getEncoder()`按格式一次选择compact/high-precision writer，避免逐vertex再判断type。compact返回原20字节推进量；高精度委托28字节base写入后在offset28写owner及零padding，返回`ptr+32`。

自有encoder只持有base encoder和固定描述，不持有WorldSlice/BlockPos/任务或缓存某次线程的cursor。profile再决定是否优化ThreadLocal读取；不能以省这次读取为由重新增加多个builder状态注入。

## 5. 温度在顶点阶段读取，fragment只写结果

`InfraredChunkRenderer extends DefaultChunkRenderer`，其`render(...)`保持调用`super.render(...)`，继续使用原批量提交、culling、排序、region和index管理；不复制这些算法。

在可覆写的`compileProgram`里缓存从owned options到base options/capture program的小型条目；正常绘制委托`super.compileProgram(cachedBaseOptions)`，继续使用原normal program缓存、shader来源和父方法行为。capture才加载原来源并组合FH归属/MRT片段。只有cache miss构造base options，避免每pass额外创建记录对象。solid/cutout首次创建条目时准备capture program，避免开关时重新编译；translucent不创建capture program。

自有缓存只拥有capture program；normal program由父类缓存拥有，不再持有第二份所有权。delete先删除自有capture并清条目，再调用super释放normal program、index buffer和原batch，避免双删或遗漏。不因开关/窗口移动/温度更新增长program key集合。由于本方案使用继承，必须验证实际安装模组对父类方法的注入仍获得正确调用，不能只验证Java签名可覆写。

重要：传给shader常量生成的必须是自有type的base type。原`ChunkShaderOptions`按`== VANILLA_LIKE`判断压缩格式，直接传wrapper会错误地把高精度格式当成compact。这个映射在自有program创建处完成，不修改原常量生成类。

`InfraredChunkShaderInterface extends ChunkShaderInterface`，通过普通覆写实现uniform和捕获：`setupState`在原pass设置之后绑定capture目标，避免父类的`super.begin`覆盖提前绑定的FBO；外层render的finally恢复。`setRegionOffset`在原设置后更新FH窗口偏移。source组合只在自有capture program创建/重载时发生，不注入全局`ShaderLoader.getShaderSource`。

- vertex从独立`layout(location=4) in uint fhOwner`读取owner；不从光照/位置/颜色推导。整数`fhRegionToTextureOrigin`代表`regionOrigin - committedTemperatureOrigin`。
- region内Section坐标继续用`_get_relative_chunk_coord(_draw_id)`，乘16后加local。
- 得到`ivec3 cell = regionDelta + sectionLocal*16 + blockLocal`，先判断有效owner及各分量`0 <= cell < 144`，再`texelFetch`现有3D `isampler3D`。
- 输出`flat out int fhTemperature`；未知owner、窗口外、尚无显示纹理均输出`-32768`。有效0°C和负温不能被判为未知。不使用`materialReadable`屏蔽解析场纹理。
- 一块温度对本quad所有顶点一致，不做温度插值。温度更新、开关能量塔、窗口移动不重建地形，下一帧读取新纹理即可。
- fragment在原discard完成后与`fragColor`一起写`layout(location=1) out int fhSurfaceTemperature`。遵守原深度、裁剪、alpha cutoff、culling与几何变换，不重画盒子，不做第二套表面。

坐标顺序必须明确，避免再次交换Y/Z：

```glsl
ivec3 blockLocal = ivec3(owner & 15u, (owner >> 8u) & 15u, (owner >> 4u) & 15u);
ivec3 sectionLocal = ivec3((_draw_id >> 5u) & 7u, _draw_id & 3u, (_draw_id >> 2u) & 7u);
ivec3 cell = fhRegionToTextureOrigin + sectionLocal * 16 + blockLocal;
```

这里输出顺序为GL纹理的`(x,y,z)`，即使CPU打包顺序是`x,z,y`也不能照抄成纹理坐标。

温度放在vertex而非fragment是默认选择，稠密网格已做探针对照。维护性优先后，关闭红外明确使用normal program，放弃上版“所有帧都运行改过的普通shader以省一个program”的取舍。capture内部仍需要可用性uniform，处理无显示纹理/非主视图等本帧状态。

所有sampler绑定位置在program使用前明确设置，即使捕获关闭也不能让`isampler3D`和原2D sampler都默认指向unit0。当前无光影管线可使用原block=0、light=1之后的unit2。位置在链接时缓存，不能逐vertex或逐region查询uniform location。

两种格式的普通light输入声明/解码保持原样。capture只增加独立owner输入、温度读取、MRT输出，不替换普通light逻辑。正常游戏画面必须逐像素对照。

减少region注入的具体办法：原`setRegionOffset`输入是`(regionOrigin - camera.int) - camera.frac`，可用当前render参数里的同一`CameraTransform`计算`round(offset + camera.frac) + camera.int - committedTextureOrigin`。这是恢复有源码定义的整数region平移，不是从depth猜方块。P0必须覆盖正负region和最大支持视距、相机小数接近0/1；误差余量不足时不得改成盲目floor。不要为省一个窄桥接而接受坐标错误。

已进一步核实：当前`CameraTransform.integral`使用向零截断，fraction允许负值，并经过带符号128的float精度对齐。必须使用传给当前`render`的同一个CameraTransform，不能换成自己计算的floor/小数部分。临时探针直接调用实际后端类，覆盖正负约3千万坐标、负小数与±8192区域相对偏移，共8,755例，整数region恢复错误0；日志`build/infrared-region-offset-probe.log`。它证明数值恢复关系，不替代实际uniform调用链验证。

## 6. 一张R16I图像，两个只引用既有附件的FBO

建议增加一个职责明确的`InfraredSurfaceTarget`，只管理以下资源：

| 对象 | 内容与所有权 |
|---|---|
| `surfaceTemperatureTexture` | 唯一新增图像：实际main尺寸、单采样`GL_R16I`、nearest、无mipmap、GPU-only，无CPU整屏镜像。 |
| `captureFbo` | color0引用main color，color1引用R16I，depth引用main depth。仅FBO容器自有，main附件不归FH释放。 |
| `blendFbo` | 只引用main color，无depth附件。用于最后的直接混合，避免同时采样当前draw FBO上挂载的depth。 |

本帧首次地形捕获前，仅用`glClearBufferiv`把R16I清为INVALID。不能用一次清色同时抹掉主颜色，也不能让旧帧温度留在已移走的轮廓上。

只在主世界视图的solid/cutout pass绑定captureFbo，draw buffers为color0+color1；渲染结束恢复原目标。其它pass不写R16I。原opaque/cutout深度胜出的fragment同时写颜色和温度，cutout洞两者同时discard，正是消除归属蓝线的保证。

按`DefaultTerrainRenderPasses.SOLID/CUTOUT`或原`isReverseOrder`语义识别目标层；不能用`isSorted()`排除透明层，因为后者受`canApplyTranslucencySorting()`配置影响。关闭透明排序也不能让透明层进入捕获。

不要假定所有Embeddium调用都属于主视图；在pass边界确认目标是本帧main，而非另一个离屏视图。相关检查按pass进行，不放到逐region/逐draw/逐vertex循环。图像尺寸取RenderTarget实际像素尺寸，不取GUI尺寸。无需额外depth纹理、整屏ID纹理、几何缓存或FBO历史队列。

## 7. AFTER_LEVEL直接混合，删除RGBA8中转与复制

AFTER_LEVEL读取同像素的main depth与R16I；采用`texelFetch`和匹配的实际viewport坐标，禁止对整数温度做线性过滤。depth只用于原扫描距离与动画，完全不再决定方块归属。

绑定只有主颜色的blendFbo后，设置：

```text
RGB equation = ADD
RGB factors  = ONE, ONE_MINUS_SRC_ALPHA
alpha factors = ZERO, ONE
depth test/write = disabled
```

输出使用预乘形式：

| 像素 | shader输出 | 最终效果 |
|---|---|---|
| 稳定扫描圈 | `vec4(heatColor * 0.43, 0.43)` | `0.43*heatColor + 0.57*原RGB` |
| 前沿3 blocks | `vec4(vec3(edge), 0)` | `原RGB + edge`，原白色扫描前沿 |
| 天空/圈外 | `discard` | 主画面保持原值 |

alpha factors保证目标alpha不变。保留原-20..20°C色标与INVALID蓝色语义。硬件混合仍读取目标颜色，不能把移除`mainTexture` sampler说成“不再读颜色带宽”。

probe在RGBA8中验证RGB最大差异为1个8位色阶（定点混合舍入），alpha差异0，并覆盖半透明目标alpha、正常圈与白色前沿。温度/归属比较仍要求精确相等，不能拿这1个色阶容差放宽整数温度断言。不要把这项结论扩展为HDR/TAA/任意光影兼容证明。

现有LDLib `ShaderManager.renderFullImageInFramebuffer`会重新绑定target，不用于新最终pass。复用`ShaderProgram`和现有fullscreen顶点shader、Tesselator绘制一个quad；只缓存一个后处理program，不伪造借用main附件的TextureTarget，不再保留IMAGE_F复制调用。shader重载时替换该program，删除自己创建的program而非共享shader。

只保存和恢复实际改变的FBO、depth、blend equation/factors、必要的scissor/纹理绑定状态；使用render state wrappers保持Minecraft缓存一致。整帧常数级状态读取可以接受，但要计入CPU测量；不建立通用GL状态快照框架，不在region热路径`glGet*`。

## 8. 帧、网络和资源生命周期

- 帧入口重置主帧状态，首次捕获pass按8.1准备目标并读取已经LAST提交的3D纹理handle和整数origin。`updateData`保持原render-thread交接；核对render-call队列消费时点，捕获过程中不主动drain队列。没有并发读写理由就不新建上传队列、第二份镜像或观察者缓存。
- 尚未收到完整显示数据时，vertex直接写INVALID，不能把不完整或未绑定纹理返回的0当0°C。场数据可独立于物理材料发布生效。
- 新窗口未完整提交时继续按旧GPU纹理的旧origin读取。网格owner不随窗口变化；不能把新的请求中心提前当成已上传中心。
- 移动、遮挡变化无需标记屏幕dirty：每个有地形捕获的活动帧清R16I一次并由本帧地形重写；无捕获帧按8.1使用INVALID。网络无变化不代表屏幕无变化，不能错误跳过捕获。
- resize或main颜色/depth附件ID变化时重新连接两个FBO，即使尺寸相同也要识别附件替换；只在重建时查格式/完整性，不逐帧重建或完整性查询。支持本轮实际RGBA8、单采样main配置。
- 同时保持`radius>0`期间的缩圈效果。缩到0之后释放自有屏幕温度图像与FBO，关时没有捕获、清屏或最终混合。3D显示缓存继续按现有生命周期处理，本轮不扩大删除范围。先测试重新开启分配的首帧成本；不先加保留定时器或多尺寸缓存。
- world reset、断线和资源重载沿render thread释放自有资源；借用的main颜色/depth绝不删除。正常材质网格始终包含owner，开关红外不造成chunk重建潮。
- 首次安装本代码时需要正常重启客户端，让新encoder和shader从同一份网格合同启动；仅F3+T不能给旧网格补owner。其后F3+T按既有renderer资源重载流程维护一致性；高精度格式切换须销毁/重建原网格与对应program，不能让20/28/32字节布局混用。
- region offset uniform按既有region循环更新，begin/end将capture开关设对；不会让最后一个地形pass的uniform/FBO泄漏给实体、天气或HUD。

### 8.1 最小帧调用顺序

复用现有`LevelRendererMixin`调用`InfraredViewRenderer.setCameraPose`的帧入口，以及已有Forge `AFTER_LEVEL`事件；不新增一套世界绘制事件或依赖天空一定绘制。帧状态只需当前主视图标志、`surfacePrepared`、已提交纹理/origin的primitive快照和原camera pose，不创建帧对象队列或通用状态机。

实际Forge `GameRenderer.renderLevel`传给`LevelRenderer.renderLevel`和`AFTER_LEVEL`的是两个不同PoseStack。最终入口不能比较其对象身份；保留已捕获camera pose，结合本帧terrain访问标志和主FBO判断，最终消费后清空帧状态。真实客户端测试必须检查最终program已执行并读回颜色，不能只检查捕获图。

```text
现有主视图帧入口：surfacePrepared=false
  ↓
自有renderer.render：记录当前pass及实际CameraTransform，调用super.render
  ↓ 原pass.startDrawing → 原program绑定
自有capture ShaderInterface.setupState：
  super.setupState
  检查主目标和SOLID/CUTOUT
  本帧首次捕获才ensure/清R16I，并取得一致的已提交3D纹理与origin
  绑定captureFbo及明确的sampler/可用性uniform
  ↓ 原region循环、原multiDraw
render的finally：恢复本pass借用的FBO/纹理状态
  ↓ 实体、BE、粒子等原绘制继续
Forge AFTER_LEVEL：直接混合，然后结束本帧
```

SOLID与CUTOUT之间不能第二次清R16I，否则前一层温度会丢失。主视图入口只重置标志；图像分配/清除延迟到首个可捕获pass。实际实现统一在空的已访问pass也准备INVALID图；若只访问了其它pass，最终显示前补一次prepare。这样省去`hasSurfaceCapture` uniform和另一条空帧采样分支，且不可能读取上一帧热图；代价是活动空帧仍清一次2P bytes温度图。没有访问本后端主视图pass时不执行FH混合。有限depth按蓝色占位/扫描前沿显示，天空保留原图。关闭到radius=0时释放自有屏幕资源。

初次3D纹理初始化仍遵守现有`receivingResponse`/LAST边界，不把staging mirror上传成完整显示。capture shader在没有已提交GPU纹理时必须输出INVALID；texture handle、origin和可用性在首次prepare后整帧一致。三维数据来源不变，不新增背景估计或网络owner字段。

其它离屏视图不能重置/消费主视图的帧状态，也不能向主R16I写入。pass恢复和AFTER_LEVEL完成均用finally，避免中断后留下借用附件绑定。只保存改动过的状态；不为了异常路径复制主颜色/depth或保留多帧R16I。

## 9. 必须一起删除的旧逻辑

最终切换后移除：

- shader的`surfaceTangent`、第二圈深度读取、法线归属、最近整数面猜测、edgeBias及内移取块逻辑。
- `depthSampleStep`、`updateDepthPrecision`及只为其服务的缓存字段/import。
- 后处理的`cameraFraction`、`temperatureBlockOffset`、3D温度采样和`mainTexture`原图采样。
- `overlayTarget`、`getOrCreateOverlayTarget`、RGBA8中转clear/resize/release，以及IMAGE_F整屏复制。
- 仅为这些路径服务且已经无调用的helper/临时验证替换标记。

保留一份用于扫描距离及遮挡像素环境定位的inverse VP；保持网络、材料、解析场和温度色标原合同。旧GPU日志保留为历史基线，但不保留旧归属算法作为运行时fallback、配置选项或兼容层。切换前在开发中做新旧对照，切换后只留准确捕获路径。

## 10. 内存与带宽账本

设`P=width*height`，`V=驻留网格顶点数`。以下为图像/字段有效载荷，不包含驱动内部padding和分配器容量。

| 分辨率 | 原RGBA8中转 | 新R16I | 净节省 |
|---|---:|---:|---:|
| 1920×1080 | 7.91 MiB | 3.96 MiB | 3.96 MiB |
| 2560×1440 | 14.06 MiB | 7.03 MiB | 7.03 MiB |
| 3840×2160 | 31.64 MiB | 15.82 MiB | 15.82 MiB |

上表是加入遮挡修复之前的`4P`与`2P`历史对照。当前为`2P+4P=6P` bytes：两个FBO仍借用main附件，另保存一张原精度地形depth快照。原144³ R16I约5.70 MiB和原CPU direct mirror各保留一份，原8 KiB Page上传scratch不变；不新增CPU屏幕数组。实际增量及复制时间见末尾的实体遮挡修复记录。

compact的GPU网格、CPU合并/上传副本和原worker暂存stride仍20，字段净增0字节/顶点。自有scope每个执行线程只持有一个primitive cursor及ThreadLocal条目，不向所有builder挂共享字段。CPU的cursor读取、编码和额外store不是零，要实测。新增少量normal/capture program缓存也计入资源生命周期，不再用更改普通shader换掉这个常数级成本。

高精度格式的新增GPU有效载荷为`4V`字节，相关CPU副本也增长。按源码中3个pass×7个facing×128*1024初始顶点容量估算，增加4字节stride可增加约10.5 MiB/worker的native初始暂存；实际分配/增长/保留量要用真实进程核实，不冒充heap实测。这也是不能统一把所有格式都扩到更宽的原因。

新的带宽支出是R16I清除、与可见/overdraw地形fragment一起写R16I、vertex温度读取，以及最终depth/温度读取与主颜色混合。节省的是旧多邻点深度读取、RGBA中转写入/清除/采样及整屏复制。硬件depth early-test、整数附件、缓存和压缩会改变实际流量，不能仅相加理论字节就宣布更快。

首版不做半分辨率温度图、温度过滤、分块屏幕缓存、SSBO owner旁表、CPU逐帧射线或第二次地形绘制：它们会重新引入边缘归属、管理或带宽成本，当前数据不足以证明必要。

## 11. 已执行的独立探针与性能依据

### 11.1 MRT/直接混合的前期选型数据

临时探针：`build/InfraredMrtProbe.java`；日志：`build/infrared-mrt-probe.log`。沿现有LWJGL/JOML与Java17，在隐藏GL上下文执行；没有改生产Java/shader，没有操作用户游戏，没有新增依赖。

硬件为RTX4070 Laptop；每个分辨率分别使用1,024和262,144个20字节顶点的合成opaque网格。16轮预热，48轮交错GL_TIME_ELAPSED查询；结果读回在计时提交之后。计时含地形和完整后处理，旧路径包括最终复制，不含游戏tick。早期只加MRT不删除中转的探针在4K发生回退，因此最终选择合并删除中转的版本。

| 分辨率/顶点数 | 当时深度方案p50/p95 ms | 前期MRT+uniform开关+直接混合p50/p95 ms |
|---|---:|---:|
| 1080p / 1,024 | 0.1751 / 0.1751 | 0.1075 / 0.1311 |
| 1080p / 262,144 | 0.2222 / 0.2458 | 0.1761 / 0.1853 |
| 1440p / 1,024 | 0.2929 / 0.3103 | 0.1782 / 0.2007 |
| 1440p / 262,144 | 0.3297 / 0.3512 | 0.2509 / 0.2529 |
| 4K / 1,024 | 0.6011 / 0.6236 | 0.4669 / 0.5120 |
| 4K / 262,144 | 0.6871 / 0.7086 | 0.5960 / 0.6083 |

vertex-fetch与fragment-fetch得到完全相等的R16I结果，vertex-fetch没有呈现值得增加动态策略的劣势。统一开关关闭时，262,144顶点的terrain p50分别为：1080p参考/关闭分支0.0676/0.0676 ms；1440p 0.0901/0.0901 ms；4K 0.1864/0.1874 ms。该对照共享光照解码，只能说明uniform分支成本，不能证明真实接入与CPU打包的全部关闭开销为零。

探针另验证owner/light往返、vertex/fragment温度相等、非空有效覆盖、直接混合RGB最大差异1与alpha差异0。它没有覆盖真实Mixin、资源包、cutout纹理、楼梯复杂遮挡、异步chunk构建或完整客户端heap；因此只能用于选择工程方向，不能用表中数字替代最终修复验收。

复现使用已有`build/infrared-raster-classpath.txt`，Java17 source-file运行上述探针。build目录为临时实验产物，clean可删除；接入阶段把必要的归属/混合断言纳入正式`InfraredRasterValidation`，不把微基准工具变成生产组件。

### 11.2 本次显式属性布局对照

实际比较了三个自有布局：20字节typed light + 独立owner属性；保持原light布局并追加owner的22字节；补齐对齐的24字节。初步分开运行存在GPU频率/负载差异，因此补做同一shader、同一场景、轮换顺序的96轮交错计时，不用分开运行的小差值强行排名。

`build/InfraredAttributeLayoutProbe.java`和`build/infrared-attribute-layout-probe.log`记录这项测试；计时仅包含terrain MRT，不含最终混合。GPU仍为RTX4070 Laptop。

| 分辨率 / 顶点数 | 20字节p50/p95 ms | 22字节p50/p95 ms | 24字节p50/p95 ms |
|---|---:|---:|---:|
| 1080p / 262,144 | 0.1270 / 0.1270 | 0.1270 / 0.1270 | 0.1260 / 0.1270 |
| 1440p / 262,144 | 0.1823 / 0.1853 | 0.1833 / 0.1874 | 0.1833 / 0.1874 |
| 4K / 262,144 | 0.3994 / 0.4127 | 0.4004 / 0.4106 | 0.4004 / 0.4127 |

1,024顶点场景三者p50分别同为0.0635、0.1116、0.2611 ms。三种布局的RGBA和R16I逐像素精确相同，覆盖多组不同光照值；有效覆盖检查通过。差异很小，选20字节的依据是明确节省2V/4V的存储且没有测得值得增宽的GPU优势，不能宣称它在所有硬件绝对最快。

`build/infrared-typed-owner-probe.log`另外保留typed属性的完整后处理探针、524,288组typed字段往返及RGB混合误差≤1/alpha精确。上述均不包含真实自有renderer继承、3个桥接、ThreadLocal读取或chunk构建成本；这些仍必须实际测量，不把选型探针冒充接入完成。

## 12. 实施顺序与每一步出口

### P0：先证明真实接入可行

- 先验证自有type、encoder、renderer/ShaderInterface子类及第2.2节4个桥接，核对它们确实继续调用原multiDraw/culling/排序；Oculus安装但光影关闭。不能以减少文件数为名复制DefaultChunkRenderer。
- 按2.2逐项核对实际注入组合，特别是Oculus已有的构造ModifyArg。输出manager/renderer/worker实际type及最终attribute描述；编译成功不等于注入成功。
- 先打通一个Section的真实owner→vertex→整数MRT链，验证原颜色、light、depth及cutout覆盖不变。禁止拿合成shader能运行当成实际renderer已接通。
- 验证20字节typed属性与原普通shader配对；高精度32字节分支的base shader选项、attribute/stride一致，不能把wrapper误识别成compact。
- 验证`setupState`实际发生在原pass绑定之后；验证region平移的整数恢复；两个隐藏接入问题都必须通过，不能为了少一个Mixin放宽正确性。
- 验证normal使用父类program路径、高精度按base生成宏、capture/normal缓存分别释放。只有完整最小链路的encoder、描述、program、uniform和FBO都配对后，才加入默认开发运行入口，避免再次注册一批半成品造成普通渲染异常。
- 不在尚未证明MRT覆盖一致前删除旧绘制入口，也不把失败候选上传到用户实例让用户替代测试。

### P1：完整生产者与坐标链

- 接通普通、Forge fallback、FRAPI、偏移模型；异常/取消/复用worker及非模型入口不串owner。
- 验证local0、4095、负坐标、Section/region边界及温度窗口边界。
- 严格禁止用quad中心、顶点中心、world-position floor或主depth补回遗漏的owner生产者。遗漏就是修生产入口。

### P2：帧资源与同次地形捕获

- 实施R16I和借用main附件的两个FBO，逐pass绑定/恢复、每活动帧仅清温度。
- 温度纹理origin与LAST提交一致，field-only照常显示；实体等不覆盖terrain热图。
- 原地形draw计数不增加、不复制网格；CUTOUT完成后仅复制一次原精度深度，用于最终遮挡判断。

### P3：直接混合与一次切换

- 实施预乘混合、白色前沿与alpha保留，AFTER_LEVEL不变。
- 所有准确归属测试通过后一次切换，按第9节删除旧shader与RGBA中转逻辑。正常关闭红外画面须与原画面一致。

### P4：生命周期和实际成本

- 验证开关/缩圈、首次开启、重复开启、resize、F3+T、维度切换、断线、chunk替换及数据窗口重定位。
- 记录CPU主线程/构建worker p50/p95、allocated bytes、native暂存峰值、上传字节、GPU网格/图像容量和实际GPU时间。首次开启与稳定帧分开报告。
- 场景固定为普通地形、密集楼梯/围墙、cutout植被、营火与能量塔热区；分别测IR关/稳定开/移动和1080p/1440p/4K。相同世界与相机条件比较，不能用整机FPS波动代替GPU计时。
- 默认compact不得增长stride、vertex/index数量或触发额外chunk重建。稳定帧不得新增随P/V增长的CPU工作或整屏CPU分配。关闭时不得清R16I/执行IR draw/进行温度采样；小幅状态/解码成本必须计入测量。
- 成本按触发频率分开记录：每vertex的cursor读取/尾部store只发生在网格重建；每region只有原uniform加FH整数偏移；每pass有有限选择/绑定；有捕获的活动帧最多一次R16I清除，最终一次IR draw。固定描述、clear参数、program条目均复用，正常program不能因温度epoch变化重新编译。
- 关闭红外时自然发生的chunk重建仍会编码owner，以避免下次开启时重建全窗口。该构建成本和缓存中的capture program必须计入关闭状态测量；“没有IR draw”不等于整个扩展零开销。
- 只保留有证据的优化。若真实场景某项成本回退，定位是编码、绑定、整数MRT还是混合后再修；不降低分辨率或放宽正确性换取成绩。

## 13. 严格正确性验收，不再让用户承担基本回归

必须在实际生产shader与实际顶点写入链上验证：

1. 原1,440平面、720完整块、720楼梯全部通过。边缘像素不排除，不允许缺值像素或错误材料ID；每块测试值可不同，防止同温掩盖串块。
2. 测试oracle来自独立的已知模型/方块标签，不从被测深度/owner解码结果生成期望值。仅替换最终颜色统计不能改采样逻辑。
3. cutout洞、相邻不同温度、细轮廓、遮挡交界、台阶/雪层/偏移植物、远近距离、负坐标和region接缝。更换索引/quad方向后归属不变。
4. 原灯光与材质显示正确：compact两个光照低字节精确保留；开关红外均不改变AO、UV、亮度、alpha cutoff。高精度格式没有错stride或错attribute。
5. 材料有效且无场、field-only、材料加场、INVALID、0°C和负温；塔开/关/缩圈后无旧热色。混合前温度精确比较，最终RGBA只允许已验证的1色阶RGB舍入与alpha精确不变。
6. 移走前景后无上帧轮廓；场景遮挡色与温度来自同一地形fragment。实体保持AFTER_LEVEL环境染色，取自身位置的既有显示数据，不能继承背景热纹理，不声称真实体温。
7. FIRST但非LAST不发布半份窗口；LAST前后、重请求和chunk/维度更新不串origin。首帧无纹理不显示伪0°C。
8. resize/重载/退出后的附件引用、viewport、depth/blend状态正确；自有对象数量有界，borrowed附件仍可被后续世界/HUD绘制使用。反复开关不能累积FBO/texture/program或worker引用。
9. 同帧SOLID后CUTOUT保留两层已绘结果；一帧有地形、下一帧只有天空/实体时不留热影；关闭透明排序不改变捕获层。主视图与离屏绘制交错不覆盖主帧数据。

开发使用Java17、`compileJava processResources compileGameTestJava`及独立GL验证。该变化不改变服务端热行为，不为渲染公式重复整套72项服务器测试；必要的数据合同回归沿已有IR测试执行。最终还有一次真实开发客户端视觉验证，必须观察完整块、楼梯、能量塔和实体，不能仅用编译或合成探针宣布完成。

## 14. 文件职责与文档交接

- `InfraredViewRenderer`：保留原网络/3D纹理生命周期，增加帧边界与单次最终混合，删除旧中转/归属字段。
- `InfraredSurfaceTarget`：一张自有R16I、两个借用附件FBO的创建/重连/清除/释放。
- `OwnedChunkVertexType`及自有encoder：显式格式和owner属性、委托base编码；原类型与常量不修改。
- `BlockOwnerScope`：每线程primitive cursor、进入/恢复；不建立world/worker映射服务。
- `InfraredChunkRenderer` / `InfraredChunkShaderInterface`：普通继承完成program选择、uniform和帧捕获，调用原绘制循环。
- Mixin仅限创建入口、自有属性绑定和方块scope三类桥接；不承担格式编码、shader变换、FBO或绘制算法。此前5个Mixin草稿及其辅助代码已删除。
- `FHShaders` / 红外shader：一个后处理program及reload处理；资源中明确vertex捕获片段与最终blend片段的用途。
- `InfraredRasterValidation`：生产链归属、MRT覆盖、原色/混合及生命周期回归；临时性能探针不进入生产。

世界温度/runtime文档已按当前真实代码更新，旧深度算法只保留为本plan的历史失败基线。真实客户端及成本验收结果写入Outcome与新diary；未验证项明确保留，不能以代码量或低错误比例替代验收。

## 15. 本轮实施结果与验证边界

本节前半记录2026-09-13首次实施结果；2026-09-14新增的实体遮挡/环境取色与成本更新见本节末尾，不能用早期2P预算代替当前实现。

### 已完成

- 生产代码只有精确owner→vertex温度→同次地形R16I→AFTER_LEVEL直接混合这一路。旧depth/法线/整数面猜测、RGBA8中转和复制均已删除；原材料/解析场合成、量化、网络、色标及扫描语义不改。
- 实际接入修正两项原计划遗漏：GPU arena高精度stride单独硬编码，必须补一处创建桥接；Forge AFTER_LEVEL与地形入口PoseStack不同，不能做对象身份比较。未通过关闭Oculus注入、放宽匹配或添加旧路径掩盖问题。
- `compileJava processResources compileGameTestJava`通过；最终`runClient -PinfraredValidation --offline --no-daemon --console=plain -Dnet.minecraftforge.gradle.check.certs=false`在Java17下`BUILD SUCCESSFUL`，结果文件`build/infrared-client-result.txt`为PASS。
- 开发客户端在独立的`run-infrared-validation`隐藏窗口、独立测试世界中运行，自动准备已有research测试catalog。普通运行及产物不加载测试窗口Mixin/控制器。失败结果会使该可选Gradle运行失败，不会仅靠游戏正常退出判成功。
- 实际compact/high-precision各捕获23,932热像素；800×450 resize后37,246像素。资源重载后owner模型回调累计735次；缩圈到0删除屏幕附件，重开恢复捕获。原图与重开图37,246像素对照：RGB最大误差1色阶、alpha精确相同。截图保存在`build/infrared-client-stage-{1..6}.png`，实际stage5是关闭原图，stage6是重开图。
- 独立与实际客户端内的隐藏GL回归均通过：8,640场景，353,123,074分类像素，错误0，GL error 0。使用生产encoder/属性/capture/final shader，覆盖20/32-byte、D24/D32F、平面/完整块/楼梯/cutout，不豁免边缘。额外验证INVALID、0、负温、扫描前沿、混合alpha、全屏清温度、resize、借用附件存活、并发/嵌套/异常scope恢复。
- `git diff --check`通过；定向搜索未发现旧`surfaceTangent`、depth precision/inset、`overlayTarget`或IMAGE_F复制调用残留。

### 成本证据

- 默认compact维持20V bytes网格、原顶点/索引数和原terrain draw数量。屏幕自有有效图像存储为2P bytes，替代旧4P；1080p/1440p/4K分别节省约3.96/7.03/15.82 MiB，未计驱动对齐和FBO元数据。3D温度图、mirror和Page scratch容量不变。
- 稳定帧无CPU逐像素/逐方块扫描。每活动帧一次R16I清除、原地形MRT、一遍最终混合；每region一个整数偏移uniform。关闭时无捕获/混合/清温度，原自然网格重建仍编码owner；capture program首次实际使用才编译，随后缓存有界。
- 高精度28→32 bytes，增加4V bytes上传和网格；现有3 pass×7 facing×131,072初始顶点scratch意味着构建期间增加10.5 MiB/context，这是代码容量关系而非完整进程native峰值测量。没有把这一增量施加给默认compact。
- 临时`build/InfraredEncoderCostProbe.java`使用实际base/owned encoder，逐块变化坐标、颜色与light，包含每块scope/finally。4096块×24顶点，160轮预热、96轮交错顺序；此为CPU编码成本探针，不是完整chunk构建或游戏帧时。

| 格式 | base p50/p95 ms | scope+owned p50/p95 ms | 96轮双路径测量区间heap分配 |
|---|---:|---:|---:|
| compact20 | 0.5109 / 0.6942 | 0.9096 / 1.3039 | 128 bytes总计 |
| high precision32 | 0.8230 / 1.0292 | 0.9629 / 1.2460 | 0 bytes |

样本中compact增加约0.399 ms/98,304顶点，说明携带归属并非免费；它只发生于网格构建。没有随vertex/block增长的scope对象分配。保留委托base编码和ThreadLocal cursor以避免复制后端编码或增加worker状态桥接，不用“零IR draw”冒充零编码成本。此前GPU布局/直接混合选型实测仍见第11节，其适用范围未扩大。

### 尚未覆盖

第12节P4的完整整合包长时间CPU/worker/native/GPU基准矩阵、实际能量塔/营火动态场景、实体/特殊资源包模型组合，以及维度切换/断线/交错离屏场景的自动化视觉验收尚未全部完成。本轮没有改服务端数据合同，未重复整套72项服务器GameTest。代码与已列测试足以确认本次普通terrain精确归属和原色混合路径接通；不宣称已经证明所有硬件/所有场景全局最优。

本轮记录：[开发日记](../diary/2026-09-13_21-04-01_infrared-exact-owner-renderer-implementation.md)。

### 21:49 代码复查与修正

- 发现最终混合调用`ShaderProgram.release()`将GL program解绑到0，却未恢复进入前的program；`ShaderInstance.lastProgramId`仍可记着原shader，重复apply会跳过绑定。真实客户端新增`verifyBlendState`在旧代码上稳定失败，日志`build/infrared-review-before.log`。
- 修复只增加一个局部program编号快照，以`GL20.glUseProgram(previousProgram)`替代解绑。必须与LDLib原始GL绑定成对恢复；Oculus在`GlStateManager._glUseProgram`上有绑定缓存，即使未启用光影也可能跳过该恢复。不新增缓存、Mixin、纹理或draw。
- 修复后`build/infrared-review-after.log`和结果文件PASS：同一原版shader重复apply、viewport/scissor恢复、两种格式、resize、reload、关闭释放、重开、37,246像素RGB/alpha对照及8,640场景GPU回归均通过；GL error 0。
- 定向复查所有四处后端原格式引用使用点及生产符号，未发现未接通的格式分支、旧猜块/复制分支或无调用兼容层。没有因此扩展热模型或重新做实现无关的性能实验；上节完整负载/扩展场景未覆盖项仍有效。
- 复查记录：[开发日记](../diary/2026-09-13_21-49-19_infrared-review-program-state-restore.md)。

### 2026-09-14 Brick变化时的显示连续性修复

- 用户报告局部Brick变化时先变蓝再恢复。根因在服务端：单方块几何变动使整Page的`currentPublication()`暂时为空，旧IR把整Page编码成field/INVALID；下一cut发布和同步再恢复。不是R16I归属或色标变化。
- IR与材料温度计改读现有`lastPublication()`，以现有材料变更日志过滤发布后真正改变的物体；同Brick/同Page未改变的材料继续读一致query cut。删除、A→Air→A、整段替换仍不能继承旧温度。Air传输和solver的几何有效性规则不变。
- `collectMaterialChangesSince`仅在几何revision落后时，把既有journal一次展开到共用64-long临时位图（512 bytes有效载荷）。受影响Brick即使worker温度epoch未更新也发送局部delta；不增加协议字段、客户端缓冲、观察者历史或温度副本。
- 并发publication/slot竞争改为两次整份读取，仍冲突则不发响应、保留客户端已提交基线，按原周期再次请求。稳定invalid/超龄cut和真实材料删除继续原field/INVALID语义；不把暂时读竞争当成删除。删除因此无调用的`InfraredBrickCodec.Builder.rewind`。
- 最终Java17 `runGameTestServer --offline --no-daemon --console=plain -Dnet.minecraftforge.gradle.check.certs=false`通过73/73，包括新增`brickMutationKeepsUnchangedMaterialTemperatures`的同Brick/跨Brick、删除/A-Air-A、full/delta/温度计和协议断言。日志`build/infrared-mutation-final.log`。
- 本次只改材料读取/数据生成，没有修改四个Mixin、顶点格式、色标或GPU资源，不重跑无关GPU矩阵。记录：[开发日记](../diary/2026-09-14_00-20-45_infrared-brick-mutation-continuity.md)。

### 2026-09-14 实体遮挡与最低复杂度环境显示

- 根因：原`AFTER_LEVEL`用屏幕地形热图直接覆盖实体，即使alpha保留也会把背景冷热轮廓投到实体上，产生看穿效果。用户要求修复，并要求实体以最低成本融入环境。
- 已实施准确遮挡：`InfraredChunkRenderer.render`在原CUTOUT完成后，调用`InfraredSurfaceTarget.captureTerrainDepth`保存原精度深度。最终shader逐像素比较快照与main depth，相同才读取地形热图。D24/D32F直接GPU复制实际存储，不估计法线、不猜块、不压低深度精度，也不引入epsilon。
- 已实施环境近似：遮挡像素通过已有inverse VP及camera相对温度窗口origin定位，读取既有144³显示纹理；已有能量塔等解析场显示数据可以直接使用。无有效值仍是蓝色基底。没有真实Air同步、脚下块搜索、实体体温模型、逐实体缓存或新增网络字段。方块继续读精确owner，环境近似只用于非terrain遮挡像素。
- 没有新增Mixin或地形/实体draw；沿用4个生产桥接。新增一张深度图、一个帧有效标志、一次GPU复制、最终一次depth比较；每个遮挡像素最多一次额外3D读取。两张自有图像随现有resize/reload/reset/缩圈生命周期管理，借用main附件不删除。
- 当前D24/D32F图像预算为6P bytes：温度2P加深度约4P。额外深度在1080p/1440p/4K分别约7.91/14.06/31.64 MiB；总自有图像约11.87/21.09/47.46 MiB。不增加CPU屏幕数组，关闭缩圈完成后释放。
- 实际开发客户端测试改为“左侧-20℃/右侧20℃背景，前景盔甲架所在位置0℃”。37,246个覆盖像素含1,376个实体像素，按独立的前景距离阈值验证RGB混合与alpha，均通过；实体取自身位置0℃，没有继承背景热分区。20/32-byte格式、resize、reload、开关及program/viewport/scissor恢复通过，日志`build/infrared-environment-client.log`，结果PASS。
- 同次生产encoder/GLSL严格GPU回归8,640场景、353,123,074分类像素错误0、GL error 0。新增前景矩形挡住六档背景温度测试：无本地数据时统一蓝底，有本地0℃数据时统一按0℃取色。借用资源存活和新增depth纹理释放均通过。
- GPU成本探针`build/InfraredDepthCopyCost.java`先渲染非恒定depth，120轮预热、96轮交错两种复制接口，GPU timestamp测复制本身；不含其他terrain/最终shader时间。RTX4070 Laptop实测如下。没有测得`glCopyImageSubData`收益，因此保留GL3.3可用的`glCopyTexSubImage2D`，未新增硬件能力分支或提高最低GL要求。

| 分辨率 | 当前复制p50/p95 ms | CopyImage对照p50/p95 ms |
|---|---:|---:|
| 1080p | 0.0297 / 0.0328 | 0.0430 / 0.0430 |
| 1440p | 0.0481 / 0.0625 | 0.0737 / 0.0819 |
| 4K | 0.2447 / 0.2970 | 0.2724 / 0.2918 |

- 边界：环境近似不是实际空气或体温；没有数据的区域不编造自然温度。不写depth的透明实体/粒子不能由此深度比较单独识别，透明介质热成像仍留以后。完整整合包帧时与真实能量塔组合视觉测试仍不以局部探针替代。
- 记录：[开发日记](../diary/2026-09-14_01-02-00_infrared-entity-occlusion-environment-tint.md)。

### 2026-09-14 同次MRT深度替代评估：完成，不切换生产

用户要求比较性能与代码成本并执行最佳方案。本轮在独立GL实验中完成MRT候选实现、原生深度资格测试和完整受控管线计时。结论是**保留原精度快照，不引入第二条生产路径**；先前“同次MRT更有希望”的推荐被本次数据限定，不能当作已证实收益。

#### 正确性门槛

- Minecraft无stencil主深度使用`GL_DEPTH_COMPONENT + GL_FLOAT`分配；本机实际为24-bit unsigned-normalized。测试同时显式覆盖D24和D32F，不通过改变主深度格式来让候选通过。
- 通过实际fragment的`gl_FragCoord.z`写入R32F，再在第二个shader用`texelFetch`与原生depth采样比较，避免CPU读回转换影响结论。每格式33,554,432像素，覆盖近深度、渐变和量化边界。
- 原始MRT在D24有9,606,912个深度不一致像素；简单`round(z*16777215)/16777215`为25,875,968个；进一步以整数位运算处理量化/浮点表示仍有35,840个。默认分配得到的D24结果相同。D32F原始MRT为0个差异。
- 这些是反例，不用于推算实际游戏错误率。它们已足以否决当前MRT候选替换：会把普通地形误判为遮挡。没有增加epsilon、容忍错判、改`gl_FragDepth`、改主深度格式或加硬件专属校准来掩盖失败。
- 临时实验为`build/InfraredMrtDepthCheck.java`，日志`build/infrared-mrt-depth-check.log`和`build/infrared-mrt-depth-exact.log`。

#### GPU成本对比

仅在两条路径都正确的D32F下比较：相同颜色/温度输入、主颜色/depth清除、辅助图清除、1或4层实际通过depth测试的绘制、复制或MRT写入，以及生产最终shader。每场景RGBA逐像素完全一致；80轮交错预热、96轮交错GPU timer，RTX4070 Laptop，GL error0。不是完整游戏帧时，也不包含网格构建。

| 分辨率/重叠层数 | 快照p50/p95 ms | MRT p50/p95 ms |
|---|---:|---:|
| 1080p / 1 | 0.1341 / 0.1341 | 0.1270 / 0.1270 |
| 1080p / 4 | 0.2560 / 0.2560 | 0.3123 / 0.3123 |
| 1440p / 1 | 0.2478 / 0.2499 | 0.1987 / 0.2017 |
| 1440p / 4 | 0.4403 / 0.4454 | 0.4895 / 0.4895 |
| 4K / 1 | 0.7250 / 0.8059 | 0.5847 / 0.6502 |
| 4K / 4 | 1.1459 / 1.2175 | 1.2339 / 1.2503 |

两者都是R16I+4-byte深度数据，约6P bytes；MRT没有显存节省。低overdraw时能省复制，高overdraw时多写深度会抵消甚至反超收益。临时实验`build/InfraredDepthPipelineCost.java`、日志`build/infrared-depth-pipeline-cost.log`。

#### 工程落地

- 生产保留当前准确快照与实体自身位置环境取色，不新增MRT/复制模式切换、配置项、Mixin或主深度改写；未通过资格的候选只在build实验中。
- 正式`InfraredRasterValidation.verifyNativeDepth`新增默认/D24/D32F近深度与量化边界检查，使用生产最终shader判断温度归属；96个渐变场景、12,441,600像素全部通过，任何漏色或误判都失败。
- 原8,640场景、353,123,074分类像素继续错误0，实体遮挡/自身环境/混合/资源合同通过，GL error0。`compileGameTestJava`和`git diff --check`通过。生产代码未变，不重复启动整个客户端或服务器。
- 本轮给出的取舍仅针对已测候选与当前准确性/维护约束，不能宣传为所有实现或硬件的全局帕累托前沿。记录：[日记](../diary/2026-09-14_01-33-18_infrared-mrt-depth-qualification.md)。
