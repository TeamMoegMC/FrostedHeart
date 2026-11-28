# Scenario 开发指南

> 状态：Partial（源码核对，未完成运行验证）
> 核对日期：2026-11-14
> 范围：剧情解析、执行、客户端画面、扩展与验证；不包含城镇模拟器。
> 代码锚点：FHScenario、ScenarioParser、ScenarioExecutor、ScenarioConductor、ScenarioThread、ClientScene。

## 阅读入口与执行链

源码根目录为 `src/main/java/com/teammoeg/frostedheart/`。本指南中的 scenario 路径均相对于 `content/scenario/`。

1. `FHScenario` 注册命令与 ScenarioProvider，按玩家获取 ScenarioConductor。
2. `FHScenario.loadScenario` 先请求提供器，再读取 config/fhscenario 下语言目录中的 UTF-8 .ks，最后回退通用文件。提供器优先于文件；empty 是特殊空场景。名称支持问号与 & 分隔的提供器参数，不是任意 URL 解码接口。
3. `parser/ScenarioParser` 把文本转为节点，并预处理标签、条件和宏边界。
4. `runner/ScenarioConductor`、ScenarioContext、ScenarioThread 管理玩家剧情与执行；`event/ScenarioCommonEvents` 在玩家 tick 推进。
5. `ScenarioExecutor` 分发服务端命令；客户端委托经 `network/S2CScenarioCommandPacket` 到 `client/FHScenarioClient`，由 ClientControl 修改 ClientScene。
6. 客户端继续／选项响应走 `C2SScenarioResponsePacket`。等待状态与恢复条件必须配套，不能仅改客户端画面就假定服务端已恢复。

## 解析契约

`@...` 与 `[...]` 分别解析独立和内联命令；# 注释、双引号和反斜杠转义由解析器自身处理。普通行末没有自动插入 r。`*` 起始文本创建 LabelNode，名称包含星号。显式 label 读取 name。

内建 nodeFactory 包括 eval、if、elsif、else、endif、emb、label、sharp、include、call。它们在普通 CommandNode 之前分派，大小写应照 factory 键书写。普通 CommandNode 会将命令名转小写；未知命令捕获 CommandNotFoundException 后作为原始文本追加，不能以“没有异常”证明注册成功。

AssignNode 的值来源优先级为 v2/var2、s/str、exp；目标位置为 v/var。IfNode 以表达式结果大于 0 为成立。EmbNode 支持 exp 或 var 与 String.format 格式，数值格式必须匹配数值类型。不要把该表达式环境当作 JavaScript。

## 注册新命令

服务端命令类由 `FHScenario.register` 注册，方法第一个参数为 ScenarioCommandContext；后续参数用 `@Param` 指定脚本键，重复注解可提供别名。方法必须公开，反射实例化需要公开无参构造。也可用 `registerCommand` 显式绑定 ScenarioMethod。

客户端功能需要核对三处：`commands/client/IClientControlCommand` 委托签名、ClientControl 实现上的实际 @Param、FHScenarioClient 注册。不要根据 Java 接口参数变量名推断脚本键：例如 speed 实际使用 v/space，标题使用 in/show/out。

ScenarioExecutor 注册命令时转小写；参数键不统一转小写。缺失基本数值参数默认 0，布尔默认 false，装箱值和字符串可能为 null。数字参数使用字面量转换，不自动求表达式。布尔按数值大于 0 转换，脚本中使用 0/1。

矩形参数通过前缀展开：@Param("") Rect 为 x/y/w/h，@Param("s") Rect 为 sx/sy/sw/sh；Point 为对应 x/y。整数支持 0x 前缀。新增类型通过 addTypeAdapter 接入，不要在各命令中复制解析逻辑。

## 命令边界

- ControlCommands：jump、queue、return、macro/endmacro，清屏与运行等待状态。
- TextualCommands：文本换行、link/endlink/nolink、delay、nowait/endnowait。
- ActCommand：章节／幕控制；VariableCommand、CookieCommand：变量与客户端标记相关行为。
- MCCommands：游戏行为；FTBQCommands：任务联动，受可选模组条件影响。
- TipCommand 与 ClientControl：提示、文字、图层和声音。

扩展前直接阅读所属类的参数注解和调用方。新增“等待”必须说明由哪个包或事件恢复，验证断线、关闭屏幕和重进世界的实际结果。

## 客户端图层与资源

ClientControl.startLayer 将编辑中的 LayerManager 入栈，showLayer 出栈并提交，存在父层时添加为子层，否则设置为主层。必须配对；不要无 startLayer 调用 showLayer。没有 dialog 时多个画面命令直接返回。

| 命令 | 实际参数键 |
|---|---|
| TextLayer | n/name、text、x/y/w/h、z、opacity、shadow、resize |
| ImageLayer | n/name、s、x/y/w/h、sx/sy/sw/sh、z、opacity |
| FillRect | n/name、x/y/w/h、z、clr |
| DrawLine | n/name、sx/sy、dx/dy、w、z、clr |
| textElement | n/name、text、x/y/w/h、z、opacity、shadow、resize |
| showLayer | n/name、trans、t、x/y/w/h、nowait |
| freeLayer | n/name |

showLayer 默认 w/h=1，t<=0 时使用 10。转场名由 `client/gui/layered/Transition` 解析。该方法接收 nowait，但当前实现中未用该参数决定等待，不应文档化为“写 nowait 就能控制执行等待”。像素绘制矩形与相对布局分别核对 RenderParams、ClientScene 和具体内容类。

图片和声音资源解析见 FHScenarioClient.getPathOf 及 ClientControl。字体、缓存和 Java2D/GL 路线详见[字体渲染说明](../scenario-font-rendering.md)，不在玩家指南展开。

## 管理命令与已知限制

`infrastructure/command/ScenarioCommand` 在 frostedheart 等前缀下注册 scenario，要求权限等级 2，并获取执行玩家。支持 jump、queue、run、pause；jump/queue 接受脚本名及可选标签。run 接受一个字符串脚本，附加 return 并推入调用栈，适合短指令调试，不是磁盘脚本替代品。

continue 的注册当前使用字面量 chapter/act，却读取同名字符串参数；不要将它列为可用恢复流程，修复需另行任务。FHScenario 中保留了注释形式的旧存储代码，不能据此承诺旧目录仍生效；持久化排查应沿当前 conductor、玩家数据与序列化调用链核对。

## 验证方式

文档变更以源码交叉检查和 Markdown 相对链接检查为主，不需要为了文档编译游戏。行为变更使用真实客户端及适合的 Forge GameTest；本仓库没有现行 JUnit 套件。

最小人工回归：通用／语言脚本加载；inline 与 @ 参数；l/p/er；两个 link 分支；跨文件 call/return；计时恢复；HUD 与独立屏幕；图片转场及窗口缩放；声音开始／停止；退出重进。观察服务端和客户端日志，记录实际游戏版本、资源包及失败步骤。本文只完成源码核对，这些人工项目尚未执行。
