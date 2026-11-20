# 栽培树木原版结构调查与重设计

- Time: `2026-11-19 10:42:00 UTC +00:00`
- Authors: `Codex（OpenAI，代码调查与设计）`
- Status: `completed`
- Scope: `CultivatedOakProfile、CultivatedNativeTrunkPlacer、CultivatedOakBlockEntity、CultivatedGiantTree`
- Related: `docs/climate/cultivated-species.md、cultivated-oak.md、cultivated-spruce.md`

## 已核实的问题

现有适配器收集原版主干候选后只保留配额内的前若干木头，随后在原点补直柱，删除没有保留木头支持的树冠附件。原版算法通过实时 setter 放置并读取已放置方块；候选收集还改变这一语义。树形失真来自适配器，不是原版生成器本身。橡树则另用人工横向短杈，并未使用 FancyTrunkPlacer。巨型云杉补齐三个顶部格以满足精确配额，也改变原版收尖结构。

## 本地原版依据

直接读取 Minecraft 1.20.1 服务端 jar 的 `data/minecraft/worldgen/configured_feature/*.json`，并反编译当前 Forge/Parchment 映射 jar 的生成器。调查产物在 `build/vanilla-tree-research/tree-classes.jar`（Java 源码）和 `decompile.log`，不引入新运行时依赖，也没有联网。

| 树种 | 原版结构与必须保留的行为 |
| --- | --- |
| 橡木 | OakTreeGrower 90%普通／10%大型；普通 Straight+Blob，大型 FancyTrunk+FancyFoliage。Fancy 算法以整体高度的0.618建立中轴，再建立斜枝和多处树冠；不能用同高度水平木头替代。花卉附近另选 bees_005 配置。 |
| 云杉 | 单棵 Straight+SpruceFoliage；巨型 MEGA_SPRUCE／MEGA_PINE 等概率，树冠高度分别13～17／3～7。GiantTrunk 原生总木头4h-3，顶部仅一根柱延伸到最后一层。 |
| 白桦 | Straight+Blob；原版主干5～7，树冠三层；花卉附近可选蜂巢配置。 |
| 丛林 | 单棵 Straight+Blob；巨型 MegaJungleTrunk 在上半树干间隔产生斜向五格枝杈，每枝独立叶冠，并保留树干藤蔓和叶藤装饰。 |
| 金合欢 | ForkingTrunk 主干上部弯折1～3步，可向另一方向生成第二支；两个末端分别产生扁平 AcaciaFoliage。不能删除第二冠后用原点直柱补齐。 |
| 深色橡木 | DarkOakTrunk 2×2整体弯折，冠下随机下垂短枝及各自树冠附件；DarkOakFoliage。仅巨型。 |
| 红树 | MangroveTreeGrower 85% tall_mangrove／15% mangrove；UpwardsBranching 每层以0.5概率尝试上升支路，RandomSpread 树冠。根生成器先确定抬高后的主干原点，再生成支柱根；高矮根偏移分别3～7／1～3。必须保留根、苔毯、藤蔓、胎生苗及蜂巢装饰。 |
| 樱花 | CherryTrunk 随机1～3条枝干，分叉从顶下3～4格开始，横伸2～4格并沿路径连接，保留木头轴向；CherryFoliage 含空洞与下垂叶。 |

TreeFeature 的正常顺序是选择高度、计算树冠参数、确定根抬升原点、检查空间、根生成、主干实时放置、按全部 FoliageAttachment 生成树冠、装饰、更新树叶距离。重设计应保留这一过程。

## 拟定方案

1. 去掉按候选顺序截断枝干、补直柱和删树冠的配额适配方式；原版完整分叉、轴向、树冠、根与装饰保持一套配置。
2. 按树种选择其原版形态变体。保留栽培进度、温度／肥料信用、同种2×2选择与失败恢复，不改已确定的成功生长扣4格结算。
3. 产量预算与几何高度分开：预算先转换为某个完整原版结构的尺寸，而不是生成后裁木头。期望产量校准应逐树种统计主干与所有枝杈，并分开校准单棵与巨型；运行时采用简单公式／小参数集，不在随机刻中反复试生成。
4. 在统一开放场地，对多随机种子、各预算和所有原版变体验证木头、叶冠连接、轴向、根、装饰与成功率。另需客户端目视验收；仅通过木头计数不能宣称树形符合原版。

## 已确认的取舍（2026-11-20）

用户确认优先完整原版树形，允许实际木头数围绕期望波动。原2×2每株多2块改为期望优势，总目标4H+8，不裁枝或补平树顶凑数。主干与枝杈共同计入木头期望，不再使用人工短枝的0.6／0.15扣减。

实现中：实时委托完整原版主干，恢复变体、蜂巢条件及完整树冠／根／装饰；用独立原版抽样校准小尺寸模型，再以另一组种子检验均值。正式运行只做公式换算与随机整数插值，不在随机刻反复预生成或建立大表。

## 文档与验证影响

实现时同步三份栽培文档、各树种结构测试、产量期望图表和新日记。旧文档关于精确裁剪配额的描述仍是当前行为，不应提前改成已实现的新算法。

## Outcome（2026-11-20）

完成原版结构重写，实际高度不设原版上限。生产实时委托原版主干，完整保留枝杈／树冠／根／装饰；产量目标通过小规模校准点反算尺寸，超过32继续外推，不进行反复预生成。删除人工横杈、候选裁剪、直柱补齐和巨型补平树顶；低信用樱花继续攒产量，不以直干代替分叉。

原版参考抽样、独立均值验证、2912组结构逐块对照及实际64格云杉全部通过；最终编译与专项36/36，日志build/cultivated-native-final-validation.log。默认模型统计误差与客户端目视验收未完成的边界见docs/climate/cultivated-tree-shapes.md。同步三份栽培文档、两张图、Python脚本及验证CSV；日记diary/2026-11-20_01-10-00_cultivated-native-tree-shapes.md。未提交、未推送。
