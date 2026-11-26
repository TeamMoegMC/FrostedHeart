# Chorda UI 选择器与客户端调试工具使用手册

- 状态：`Current`，按当前源码及真实客户端演示核对。
- 最后核验：`2026-11-26`。
- 范围：UIElement 选择器、debug 路径和开发客户端控制桥的六条 CUI 命令。
- 代码锚点（以下路径相对 `FrostedHeart/`）：`src/main/java/com/teammoeg/chorda/client/cui/base/UIElement.java`、同目录 `UILayer.java`、`src/main/java/com/teammoeg/chorda/client/ClientUtils.java`、`src/main/java/com/teammoeg/chorda/client/cui/CUIDebugHelper.java`、`src/gametest/java/com/teammoeg/frostedheart/devtools/AgentClientBridge.java`、`Scripts/agent_client.py`、`Scripts/dev_client_bridge.init.gradle`。

本工具用于在运行中的 CUI 界面里定位元素、读取和修改字段、动态添加或移除子元素。
**列举只列当前界面根节点的一层直接子元素；selector 可以定位任意深度的元素。**
修改只作用于当前客户端中的元素对象，不是保存 UI 布局的编辑器；关闭、重开或重建界面后，修改可能消失。

配套材料：[真实客户端演示视频](../../ai_files/chorda-ui-tools-demo.mp4)（2 分 11.67 秒，中文字幕，无配音）；[CUI 架构指南](chorda-cui.md)。

## 1. 启动与第一条命令

### 1.1 准备条件

需要项目可运行的 Java 17 开发环境、Python 3，以及图形显示环境。以下 shell 示例使用 **Bash**，所有命令都在 `FrostedHeart/` 目录执行。

控制桥是开发用入口。普通生产 JAR 不会自动带上这里的 devtools 源集；只把 `frostedheart.agentBridge` 设为 true，不能代替编译和加载 devtools。

### 1.2 终端 A：启动带控制桥的客户端

```bash
./gradlew runClient -I Scripts/dev_client_bridge.init.gradle
```

已缓存全部依赖、资源时，也可离线启动：

```bash
./gradlew --offline runClient -I Scripts/dev_client_bridge.init.gradle
```

该 init 脚本负责：

1. 创建只包含 devtools Java 源码的 `agentBridge` 源集。
2. 把它加入客户端 mod 源，并设置 `frostedheart.agentBridge=true`。
3. 设置启动窗口尺寸为 1280×720。

不要为了使用控制桥把整个 gameTest 源集及其资源加入正常客户端运行；它们包含测试世界生成资源，会影响创建的存档。

### 1.3 终端 B：确认连接并打开 CUI 界面

```bash
python3 Scripts/agent_client.py 'state'
```

先在游戏中进入世界，打开需要调试的 CUI 界面，例如绘图桌的研究档案页，再执行：

```bash
python3 Scripts/agent_client.py 'cui-elements'
python3 Scripts/agent_client.py 'cui-query .TextBox'
```

`state` 可在没有打开 CUI 界面时使用；六条 `cui-*` 命令要求当前 Minecraft `Screen` 实现 `CUIScreen`，且主层不为 null。
原版界面、主菜单、没有界面或只显示 HUD overlay 时，不会自动被当作该 CUI 查询根。

研究页的 `.TextBox`、`.ResearchArchiveLayer` 是本文示例使用的类型，并不是每个界面都有。
在其他界面操作时，先用枚举或 debug 取得该界面的真实 selector。

## 2. 如何发送命令

### 2.1 每条命令是一个完整参数

```bash
python3 Scripts/agent_client.py 'cui-query .DrawDeskScreen .TextBox'
python3 Scripts/agent_client.py 'cui-get {"selector":".TextBox","field":"name"}'
```

外层单引号属于 Bash 引用；里面的双引号属于 JSON。`>`、`*`、`#` 和空格都应放在引号内，避免被 shell 解释。
本工具不是聊天栏命令：不要加 `/`，不要在游戏聊天框输入 `cui-query`。

可以在一次调用中顺序执行多条命令：

```bash
python3 Scripts/agent_client.py \
  'cui-query .TextBox' \
  'cui-get {"selector":".TextBox","field":"name"}'
```

脚本会逐条打印 JSON；某条结果 `ok=false` 时退出，后面的命令不再发送。
`cui-query` 未命中属于正常结果 `ok=true, element=null`，不会让脚本停止。

### 2.2 目录与等待时间

```bash
python3 Scripts/agent_client.py --dir run/agent --timeout 60 'cui-elements'
```

| 参数 | 默认值 | 含义 |
|---|---|---|
| `--dir` | `run/agent` | 当前工作目录下的控制桥收发目录，必须对应目标客户端的 `<gameDir>/agent` |
| `--timeout` | `600` | 每条命令等待结果的最长秒数，不是整批命令的总时限 |

默认开发运行的游戏目录是 `FrostedHeart/run/`。从其他目录调用脚本时，需给 `--dir` 传正确路径；目录路径也可以是绝对路径。

命令经 `inbox/<id>.cmd` 交给客户端，在客户端线程执行，结果写入 `outbox/<id>.json`。脚本读到结果后会删除该结果文件；需要长期保留时，可将脚本输出重定向到文件。

```bash
python3 Scripts/agent_client.py 'cui-elements' > cui-elements-result.json
```

`--timeout` 超时不会撤回已经提交的命令。客户端未启动或崩溃时，未消费的 `.cmd` 可能留在 inbox，后续启动后仍会执行。
出现 `no reply within ...` 时先核对客户端状态和命令队列，再决定是否重试，避免重复添加或重复修改。

### 2.3 Python 中构造 JSON，避免手动转义

下面代码可保存为 Python 文件后运行；它使用参数列表启动脚本，不依赖 Bash 引用规则。
运行时的工作目录仍需是 `FrostedHeart/`。

```python
import json
import subprocess
import sys

payload = {"selector": ".TextBox", "field": "name"}
command = "cui-get " + json.dumps(payload, ensure_ascii=False)
result = subprocess.run(
    [sys.executable, "Scripts/agent_client.py", command],
    check=True, capture_output=True, text=True,
)
print(json.loads(result.stdout))
```

## 3. selector 语法与匹配规则

### 3.1 支持的写法

| selector | 含义 |
|---|---|
| `#search` | name 等于 `search` 的元素 |
| `.TextBox` | class 列表包含 `TextBox` 的元素，包含其子类实例 |
| `.Focusable` | class 列表包含直接声明接口 `Focusable` 的元素 |
| `*` | 任意元素；根也参与查询，所以在控制桥中通常首先匹配界面根 |
| `.TextBox#search` | 同一元素同时满足 class 和 name |
| `.TextBox.Focusable` | 同一元素同时满足类名和接口名条件；不是两层路径 |
| `.ResearchArchiveLayer .TextBox` | 前者的任意深度后代中匹配后者 |
| `.ResearchArchiveLayer > .TextBox` | 仅在前者的直接子元素中匹配后者 |
| `.ResearchGraphViewport > *[1]` | 图谱视口的第一个直接子元素 |
| `.DrawDeskScreen > .ResearchArchiveLayer > #search` | 从根开始的明确路径 |

name 和 class 名区分大小写。可用字符是 Java 标识符组成字符及连字符 `-`；不支持空格、点号等字符的转义。
建议 name 使用 `search`、`demo-progress` 这类易读值。`setName` 或反射赋值本身不检查这个格式，但不能解析的 name 不会被用于自动路径生成。

`[n]` 从 **1** 开始，表示在父容器完整子元素列表中的位置，包括隐藏和禁用元素，不是第 n 个同类元素。
例如 `.TextBox[2]` 要求该元素既是 TextBox，又恰好处于父列表第二位。
不支持 `[0]`、负数、`:nth-child()`、逗号分组、`+`/`~` 兄弟组合器、CSS 属性匹配或完整浏览器 CSS 语法。

### 3.2 查询范围和首个匹配

`querySelector` 在当前根及其子树中查找，包含根自身，也包含隐藏和禁用的元素。
按子元素列表顺序递归查找，返回首个满足完整 selector 的元素，**不是所有匹配的数组**。
相同 name 允许存在；`#name` 不承诺全局唯一。需要确定目标时，使用返回的完整 `selector` 或增加父路径约束。

空格可以跨层，`>` 不能跨层。研究界面的例子：

```bash
python3 Scripts/agent_client.py 'cui-query .DrawDeskScreen .TextBox'
python3 Scripts/agent_client.py 'cui-query .DrawDeskScreen > .TextBox'
```

前一条能选中深层搜索框；后一条若根的直接子元素中没有 TextBox，就返回 null，不会越过中间容器。
空字符串或非法 selector 不匹配；最外层命令参数仍应遵守命令格式。

### 3.3 class 列表如何生成

`UIElement.getSelectorClasses()` 按以下顺序输出：

1. 当前实际 Java 类名；匿名类没有该项。
2. 当前类直接声明的接口，按 Java 反射提供的顺序。
3. 直接父类名及该父类直接声明的接口。
4. 沿类继承链重复，直到并包含 `UIElement`；不包含 `Object`。

接口不递归展开自己的父接口。匹配名称采用简单类名，不用包名。
class 列表仍可包含深层父类和直接接口；这与“自动生成路径只考虑当前类和直接父类”的规则不同。

### 3.4 自动生成的唯一 selector

`UIElement.getUniqueSelector()` 从根开始，用 ` > ` 连接各层。每层按以下优先级选择：

1. 在同级中唯一、且语法可解析的 `#name`。
2. 在同级中唯一的 `.当前实际类名`。
3. 在同级中唯一的 `.直接父类名`。
4. `*[n]`。

匿名类跳过自己的类名。自动路径不以接口或更深父类作为候选，但会按实际 matcher 检查兄弟节点是否也匹配候选，以判断唯一性。
这里的“同级”包括隐藏和禁用兄弟节点。没有加入父容器的元素返回 null。

添加、移除、修改 name 或重建界面，都可能改变唯一性或索引，使旧路径失效；操作后优先使用最新返回的 `element.selector`。

## 4. 返回结果格式

### 4.1 公共外层

成功查询示意，`id` 和 `state` 的具体值依运行状态变化：

```json
{
  "id": "01795671200000000000",
  "command": "cui-query .TextBox",
  "ok": true,
  "element": {
    "selector": ".DrawDeskScreen > .ResearchArchiveLayer > .TextBox",
    "name": null,
    "type": "ResearchArchiveLayer$1",
    "classes": ["TextBox", "Focusable", "UIElement"],
    "parentSelector": ".DrawDeskScreen > .ResearchArchiveLayer",
    "title": "",
    "rect": [6, 35, 92, 17],
    "screenRect": [6, 35, 92, 17],
    "enabled": true,
    "visible": true
  },
  "state": {
    "user": "Dev",
    "screen": "CUIMenuScreenWrapper",
    "inWorld": true
  }
}
```

`state` 示例仅列部分字段。公共外层有 `id`、原始 `command`、`ok` 和当前客户端 `state`。
错误返回 `ok=false` 与 `error` 字符串。查询未命中返回 `ok=true` 与显式 `element=null`。

### 4.2 元素 debug 信息

| 字段 | 含义 |
|---|---|
| `selector` | 当前元素的自动唯一完整路径 |
| `name` | 元素 name；未设置时为 null |
| `type` | 实际类名去掉包名前缀后的部分；匿名内部类可能是 `ResearchArchiveLayer$1` |
| `classes` | selector 可匹配的有序类名及接口名列表 |
| `parentSelector` | 直接父元素当前的唯一 selector；根没有父节点时为 null |
| `title` | `getTitle().getString()` 的结果，可能为空字符串 |
| `rect` | `[x, y, width, height]`，元素局部坐标和尺寸 |
| `screenRect` | `[getScreenX(), getScreenY(), width, height]`，按 UIElement 坐标接口直接输出 |
| `enabled` | 元素本身的启用状态 |
| `visible` | 元素本身的可见状态 |

所有 CUI 命令中的元素 debug 信息都不递归嵌套 `children`。
子元素的 `visible=true` 不代表其祖先也可见；祖先隐藏时，该子元素仍可能出现在查询中。

`screenRect` 不是视频像素坐标，也不保证可以不经转换直接交给 `click`。
菜单 Screen 可能在渲染时额外加上 `guiOrigin`；`screen` 命令输出的 CUI `rect` 使用其适配逻辑转换后的 GUI 坐标。
要做点击或悬停，请先查看 `screen` 的 `guiScale`、`guiOrigin` 和元素位置。截图像素与 GUI 坐标之间还需要按 `guiScale` 转换。

## 5. 六条命令参考

### 5.1 `cui-elements`：只列举当前根的一层

```bash
python3 Scripts/agent_client.py 'cui-elements'
```

无参数。返回 `elements` 数组，按当前 CUI 根的直接子元素顺序排列，包含隐藏和禁用项，不包含根自身，也不展开孙节点。
当前没有 `cui-elements <selector>` 或递归枚举参数；要定位未列出的深层节点，使用 `cui-query` 或游戏内 debug。

### 5.2 `cui-query <selector>`：查任意深度

```bash
python3 Scripts/agent_client.py 'cui-query .ResearchGraphViewport'
python3 Scripts/agent_client.py 'cui-query #demo-progress'
python3 Scripts/agent_client.py 'cui-query .ResearchGraphViewport > *[1]'
```

参数是命令名后面完整的 selector 字符串，不是 JSON。返回首个匹配的 `element` 或 null。
只查询，不修改 UI。

### 5.3 `cui-get <JSON>`：反射读取字段

```bash
python3 Scripts/agent_client.py 'cui-get {"selector":".TextBox","field":"name"}'
python3 Scripts/agent_client.py 'cui-get {"selector":".TextBox","field":"x"}'
```

| JSON 参数 | 类型 | 是否必填 | 含义 |
|---|---|---|---|
| `selector` | string | 是 | 目标元素，支持任意深度 |
| `field` | string | 是 | Java 字段名，不是 getter 名或字段路径 |

查找从实际类开始逐级向父类查找，遇到最近的同名声明就使用它。支持 private/protected/public 实例字段；不读取 static 字段。
不会调用 `getName()` 等方法，不支持 `parent.name` 这样的嵌套路径，也没有“列出所有字段”命令。

返回 `field`、`declaringClass`、`fieldType`、`value` 和当前 `element`。
`declaringClass` 是字段真正声明所在类的全限定名称；`fieldType` 是声明的泛型类型字符串。

null、字符串、Number、Boolean 保持 JSON 类型；char、enum、集合、UI 对象及其他对象采用 `String.valueOf` 字符串描述。
因此复杂对象的 `value` 不承诺可以原样传回 `cui-set` 恢复。

### 5.4 `cui-set <JSON>`：反射修改字段

```bash
python3 Scripts/agent_client.py 'cui-set {"selector":".TextBox","field":"name","value":"search"}'
python3 Scripts/agent_client.py 'cui-get {"selector":"#search","field":"name"}'
python3 Scripts/agent_client.py 'cui-set {"selector":"#search","field":"name","value":null}'
```

| JSON 参数 | 类型 | 是否必填 | 含义 |
|---|---|---|---|
| `selector` | string | 是 | 目标元素 |
| `field` | string | 是 | Java 实例字段名，规则与 get 相同 |
| `value` | JSON value | 是 | 按字段声明类型反序列化的新值；null 也必须明确写出 |

返回 `field`、`declaringClass`、`before`、`after` 和修改后的 `element`。
查找和读取规则与 get 相同，`value` 使用 Gson 按字段声明的泛型类型转换。

常用值示例：

| Java 字段类型 | JSON 值例子 | 说明 |
|---|---|---|
| `String` | `"search"` | 字符串必须有 JSON 双引号 |
| `int` | `104` | 数值直接写数字 |
| `double` | `0.72` | 直接写小数 |
| `boolean` | `false` | 不是字符串 `"false"` |
| 引用类型 | `null` | 允许 null，实际业务字段是否能为空取决于组件实现 |
| `enum` | `"CONSTANT_NAME"` | 交给 Gson 按枚举声明转换 |
| 泛型集合/对象 | 数组或对象 | 按声明类型转换；接口、抽象类、回调通常不能靠普通 JSON 构造 |

不允许给 primitive 字段写 null。字段不可写、JSON 不适配类型或字段不存在时返回错误。

这是直接 `Field.set`，**不会调用 setter、重新布局、refresh、发送业务网络包或保存字段原值**。
例如反射设置 `width` 不会执行 `TextBox.setWidth()` 中附带的滚动处理；修改客户端字段也不能当作服务器业务状态已经改变。
修改后重新 query 检查实际状态；需要恢复时，使用事先记录的原值。

### 5.5 `cui-add <JSON>`：构造并添加子元素

下面是演示中实际使用的自动构造器匹配示例：

```bash
python3 Scripts/agent_client.py 'cui-add {"selector":".ResearchArchiveLayer","class":"com.teammoeg.chorda.client.cui.widgets.ProgressBar","args":[0.72,-13056135]}'
```

| JSON 参数 | 类型 | 是否必填 | 含义 |
|---|---|---|---|
| `selector` | string | 是 | 接收新子元素的父节点，当前必须是 UILayer 或其子类 |
| `class` | string | 是 | 新元素的 Java 全限定类名，必须继承 UIElement |
| `args` | array | 否 | 除首参父节点之外的构造参数，默认 `[]` |
| `parameterTypes` | string[] | 否 | 精确构造器签名，包含第一个父节点参数类型 |

工具自动把 selector 指定的父节点对象放到构造器第一个参数。`args` 不要再写父节点。
对本例而言，相当于调用 `new ProgressBar(parent, 0.72, -13056135)`，随后调用 `parent.add(child)`。
颜色参数是 Java 有符号 int，`-13056135` 对应 ARGB `0xFF38C779`；需要传颜色时按目标构造器实际类型给值。

**不指定 parameterTypes 时：**

1. 使用所选类自身的 `getDeclaredConstructors()`，包括非 public 构造器；不从父类继承构造器。
2. 按 `Constructor.toGenericString()` 的字典序排序，不按源码声明顺序或“最接近参数类型”排序。
3. 筛选参数个数为 `len(args)+1`，且第一个声明类型能接收选中父对象的构造器。
4. 除父节点外，按构造器每个参数的声明泛型类型，用 Gson 转换对应 JSON。
5. 依次尝试调用；转换或构造失败后继续下一个候选，第一个成功者被采用。
6. 全部失败时返回 `no usable constructor ... attempts=...`，包含尝试过的候选错误。

重载存在歧义时，使用显式签名：

```bash
python3 Scripts/agent_client.py 'cui-add {"selector":".ResearchArchiveLayer","class":"com.teammoeg.chorda.client.cui.widgets.ProgressBar","args":[0.35,-1665243],"parameterTypes":["com.teammoeg.chorda.client.cui.base.UIElement","double","int"]}'
```

这精确查找 `(UIElement, double, int)` 构造器。`parameterTypes` 包含父节点类型，而 `args` 不包含父节点，所以数组长度分别是 3 和 2。
第一个签名类型必须是**构造器声明的类型**：即使实际父对象是 ResearchArchiveLayer，此构造器首参声明为 UIElement，就必须写 UIElement，不是 ResearchArchiveLayer。

签名解析支持：

- 普通类全限定名，例如 `java.lang.String`。
- 基本类型 `boolean`、`byte`、`short`、`int`、`long`、`float`、`double`、`char`。
- Java 风格数组，例如 `int[]`、`java.lang.String[]`，也支持多维数组。
- 内部类二进制名，例如 `some.package.Outer$Inner`。

primitive `int` 与包装类 `java.lang.Integer` 是不同签名；泛型参数写擦除后的类，例如 `java.util.List`，不要写 `java.util.List<java.lang.String>`。
指定签名后不尝试其他重载。所有其余参数仍由 Gson 转换，不支持传 Java 表达式、lambda 或方法调用。

**成功返回：** `element` 为添加之后生成的实际 debug 信息，包含当前 selector、类信息、位置和尺寸。
不要在添加前猜测新元素的索引；下一次操作应直接使用返回的 `element.selector`。

**生命周期与实测限制：**

- 只有选中节点为 UILayer 才会尝试构造；它的 `add` 返回 false 时命令报错。
- 不自动调用 `refresh()`、`addUIElements()`、setter 或布局，新增容器也不会自动创建其声明式子元素。
- 新元素在 add 后就可能参与下一帧渲染。构造器的初始状态必须能渲染，不能假定能在后续命令中及时补齐必需属性。
- 本轮实测中，零尺寸的 `new TextBox(parent)` 加入研究页后，在 DrawDeskTheme 背景绘制触发除零崩溃；后续设置尺寸的命令来不及执行。演示改用默认尺寸有效的 ProgressBar。该问题尚未修复。
- 构造失败只代表未进入 add，不会撤销构造器自身已经产生的副作用；add 后读取 debug 失败也不代表该元素没有加入。

### 5.6 `cui-remove <selector>`：从直接父级移除

```bash
python3 Scripts/agent_client.py 'cui-remove #demo-progress'
python3 Scripts/agent_client.py 'cui-query #demo-progress'
```

通过任意深度 selector 定位目标，再从目标的**直接父 UILayer** 调用 `remove`。
不需要另外传父 selector；不能删除界面根，也不会向其他祖先容器尝试移除。

成功返回：

- `removed`：移除前保存的目标 debug 信息；其中 selector 是历史位置，不再是可回查的当前路径。
- `parent`：完成移除后的直接父节点 debug 信息。

`UILayer.remove` 要求父引用正确且元素确实在其直接列表里；移除后清理该元素对应的滚动条引用、失效内容尺寸缓存，并调用元素 `onClosed()`。
对容器移除后，其整棵子树从当前根的 selector 查询中消失。
不自动重排剩余元素，不自动重新创建默认内容，也不提供 undo。`onClosed` 抛异常时，元素可能已经从列表移除；重新 query 可确认实际状态。

## 6. 一次完整的添加、修改、读取、移除操作

前提：已打开研究档案界面。下面的 Python 示例会自动读取新增元素返回的 selector，再给它设置 name，避免手工猜测索引。
示例执行六条命令，创建一个进度条、修改 name 和 x、读取 x、移除并确认不存在。已有同名元素时请换用自己的 name。

```python
import json
import subprocess
import sys


def send(command):
    process = subprocess.run(
        [sys.executable, "Scripts/agent_client.py", command],
        capture_output=True, text=True,
    )
    if not process.stdout.strip():
        raise RuntimeError(process.stderr)
    result = json.loads(process.stdout)
    if process.returncode or not result.get("ok"):
        raise RuntimeError(result.get("error", process.stderr))
    return result


def data_command(verb, **payload):
    return send(verb + " " + json.dumps(payload, ensure_ascii=False))


added = data_command(
    "cui-add",
    selector=".ResearchArchiveLayer",
    **{
        "class": "com.teammoeg.chorda.client.cui.widgets.ProgressBar",
        "args": [0.72, -13056135],
    },
)
selector = added["element"]["selector"]
try:
    named = data_command("cui-set", selector=selector, field="name", value="guide-progress")
    selector = named["element"]["selector"]
    data_command("cui-set", selector=selector, field="x", value=104)
    read = data_command("cui-get", selector=selector, field="x")
    print("当前 x：", read["value"], "字段声明类：", read["declaringClass"])
finally:
    send("cui-remove " + selector)
assert send("cui-query #guide-progress")["element"] is None
```

修改已有元素时，先用 get 记录原值，结束后 set 回原值。不要把示例中的 `value=null` 当作通用恢复命令；它只适用于原值确实为 null 且允许 null 的引用字段。

## 7. 游戏内查看 debug selector

1. 打开 CUI 界面。
2. 按 **F12** 开启 CUI debug。
3. 按住 **Ctrl** 并把鼠标移到元素上，查看边框及 tooltip 中的 `Selector:`。
4. 复制或记录完整路径，通过 `cui-query` 回查；操作后关注返回的新路径。
5. 演示或检查结束，再按 F12 关闭。

debug tooltip 还显示类名、父元素、坐标、尺寸、启用与可见状态。当前实现不要求先打开原版 F3 界面。
F12 是通过当前界面的按键分发处理的；没有打开 CUI，或子控件先消费该按键时，不应假定开关已经切换。
控制桥的 `key f12` 可以发送一次按键，但 `key left.control` 只是按下再释放，不能代替持续按住 Ctrl 悬停。

可以先保存游戏原始截图：

```bash
python3 Scripts/agent_client.py 'screenshot cui-inspection'
```

它将下一批新渲染帧中的画面保存到 `run/agent/screenshots/` 并返回文件路径。
录屏视频里的中文命令面板是录制脚本额外叠加的说明，不是 `screenshot` 或 CUI 框架自带界面。

## 8. 常见结果与排查

| 返回或现象 | 含义与处理方式 |
|---|---|
| `the current screen is not a CUI screen` | 打开真正的 CUI Screen 后再执行；HUD overlay 不会被自动选作根 |
| `cui-query` 的 `element=null` | 查询正常完成但没匹配；检查名字大小写、路径、当前界面和中间层 |
| `no CUI element matches ...` / `no CUI parent matches selector` | get/set/remove/add 的 selector 未命中；先 query 确认 |
| `NoSuchFieldException` | 检查 Java 字段名；不要传 getter 名或 `parent.name` |
| `expected an instance field` | 找到了 static 字段；当前 get/set 只支持实例字段 |
| `null cannot be assigned to ...` | primitive 参数或字段不能写 null，改传正确类型值 |
| `ClassNotFoundException` | 检查类是否在客户端 classpath、全限定类名和内部类 `$` 写法 |
| `class must extend UIElement` | `class` 对应类型不是 UIElement 子类 |
| `no usable constructor ... attempts=...` | 对照真实构造器检查数量、父参兼容性、Gson 转换；有歧义时指定 parameterTypes |
| `NoSuchMethodException` | 显式签名不精确；特别检查父参声明类型、primitive/包装类和完整参数列表 |
| `selected element does not support addition` | 目标不是 UILayer；换用实际父容器 selector |
| `the direct parent does not support removal` | 目标没有可移除的直接父容器，例如界面根 |
| `parent refused addition/removal` | 容器拒绝操作，或元素父引用/成员关系不符合要求 |
| 修改了字段但界面没变化 | 可能改到隐藏元素、被父节点隐藏、字段不直接参与渲染，或需要 setter/布局/服务器逻辑 |
| 增删后旧 selector 失效 | 索引或类名唯一性变化，重新 query/debug 取得当前路径 |
| `ok=true` 后下一帧渲染崩溃 | 命令完成不等于组件初始状态可渲染；检查构造器默认尺寸、资源和主题要求 |
| `no reply within ...` | 核对启动方式、收发目录、加载状态、崩溃日志和未消费命令；超时不自动取消 |

当前没有事务、自动恢复布局、自动重放刷新后的修改或撤销命令。

## 9. Java 代码直接使用

选择器本身位于生产 CUI 基类，不依赖 devtools 控制桥启动。

```java
UIElement local = panel.querySelector(".TextBox");
UIElement current = ClientUtils.querySelector("#search");
if (current != null) {
    String path = current.getUniqueSelector();
}
```

`panel.querySelector` 查询 panel 及其子树；`ClientUtils.querySelector` 查询当前打开 Screen 的 CUI 主层，非 CUI Screen 返回 null。
在自定义控件中可用 `setName("search")` 设置 name；`getSelectorClasses()` 提供匹配列表。
自定义容器需要通过 `getSelectorChildren()` 暴露子节点；控制桥的枚举、添加和移除命令当前只认 UILayer 及其子类，不会反射查找其他类型的 add/remove 方法。

## 10. 演示对应位置与验证范围

| 视频时刻（约） | 内容 |
|---|---|
| 00:04 | 单层枚举、深层 selector、空格与 > |
| 00:22 | 反射读取和修改继承字段 |
| 00:45 | debug 的 name、类名、索引选择 |
| 00:58 | 自动匹配构造器并添加进度条 |
| 01:13 | 显式签名构造第二条进度条 |
| 01:33 | 未命中、不支持添加、字段不存在 |
| 01:43 | 移除、回查 null、恢复字段 |

视频使用真实研究界面，连续录制时长 131.67 秒，1280×1040、15 fps、H.264。
六条 CUI 命令均在客户端执行；包含 44 次实际命令结果（也包括悬停及 F12）。
视频完整解码通过，抽查了查询、debug、新增、恢复及结尾画面；录制后重新回查确认新增元素不存在、name 为 null、ghostText 为 Find。

本机生成证据在 `FrostedHeart/build/cui-tool-demo/`：`commands.json`、`chapters.json`、`summary.json`、`video-validation.json`、`cleanup-verification.json`。
这些属于运行生成物；生产源码与当前手册定义使用规则，视频不是每个组件/构造器都可用的承诺。
