# Scenario 使用指南

> 状态：Partial（源码核对，未做游戏实测）
> 核对日期：2026-11-14
> 范围：阅读剧情、编写文本与选择、控制画面和声音。
> 核对依据：ScenarioParser、ControlCommands、TextualCommands、ClientControl、ImageScreenDialog、FHKeyHandler。

## 1. 只想阅读剧情

剧情可能显示在游戏画面上，也可能打开独立剧情屏幕。看到可点击的选项文字时，直接点击文字；点击空白处不等于选择。

默认 Z 是“跳过对话”按键，可以在游戏的按键设置中更改。它发送继续请求；遇到必须选择、等待事件或等待动画的段落，不保证能跳过去。不要把它理解为跳过整章。独立剧情屏幕与普通游戏画面的输入方式可能不同，优先使用屏幕上的选项。关闭屏幕不等于完成剧情。

## 2. 写出第一段剧情

需要能编辑游戏文件，并在测试存档中使用管理员命令。多人游戏的脚本放在服务器侧。用纯文本编辑器建立 `config/fhscenario/hello.ks`，保存为 UTF-8；不要保存成 `hello.ks.txt`。

```text
@hudDialog show=1
欢迎来到营地。[r]
这是你的第一段剧情。[l]
@er
祝你旅途顺利。[l]
@s
```

进入游戏后输入 `/frostedheart scenario jump hello`。需要权限等级 2；聊天栏输入命令时带斜杠，文件内容不带斜杠。名字不带 `.ks`。`[l]` 是小写字母 L，表示等待读者继续；`[r]` 表示换行；`@er` 清除文字；`@s` 停止执行。

脚本修改后重新跳转到该脚本试读。若没有变化，检查是否有语言版本覆盖：系统优先找 `config/fhscenario/<语言代码>/hello.ks`，找不到才用上面的通用文件。

## 3. 语法只需记住这些

普通文字就是剧情内容。文件中的回车不应当作画面换行，需要换行时写 `[r]`。

- 单独一条指令：`@指令 参数=值`。
- 夹在文字里的指令：`[指令 参数=值]`。
- 有空格的参数加英文双引号，例如 `@showTitle t="第一章 营地"`。
- `#` 后是注释，不显示。要显示特殊字符，用反斜杠转义，例如 `\#`、`\[`、`\@`。
- 参数名按示例的大小写书写。指令建议统一小写，尤其是 if、eval、call 等。
- 不认识的指令可能直接显示成文字；看到指令原样出现，应先检查拼写。

## 4. 翻页、等待与结束

| 写法 | 效果 |
|---|---|
| `[r]` | 换行 |
| `[l]` | 等待继续，保留当前文字 |
| `[p]` | 等待继续后清除这一页；静默等情况下可能直接清除 |
| `@er` | 立即清文字 |
| `@delay t=20` | 等待计时；时间由游戏 tick 推进，正常 20 tick 约 1 秒 |
| `@nowait` / `@endnowait` | 开始／结束不等待文字模式 |
| `@s` | 停止脚本 |
| `@return` | 从 call 调用的段落返回，不是普通结尾的替代品 |

`@wt`、`@wa`、`@wr`、`@wtr` 分别涉及事件、动作、绘制或过渡等待。初学者不用这些来实现普通翻页；没有对应事件时可能一直等待。

## 5. 制作两个选择

标签是段落的名字。推荐用 `@label name=...`，跳转时照抄名字。

```text
@fullScreenDialog show=1
你要去哪里？[r]
[link l=camp]返回营地[endlink][r]
[link l=road]沿路前进[endlink]
@wc
@label name=camp
营地里升起炊烟。[l]
@s
@label name=road
你走进了风雪。[l]
@s
```

`link` 与 `endlink` 之间是可点击的文字；`@wc` 在这里等待选择。每个分支末尾加 `@s`，避免接着读到另一个分支。

跳到另一个文件用 `@jump s=文件名 l=段落名`；只跳本文件用 `@jump l=段落名`。需要读完后回来，用 `@call s=文件名 l=段落名`，被调用的段落末尾写 `@return`。`queue` 是排队执行，不等同于立即跳转。

另一种标签写法是 `*camp`，星号本身属于标签名字，目标也应写 `l=*camp`。不要混用两种名字。

## 6. 数字、文字和条件（按需阅读）

```text
@eval v=score exp="2+3"
分数：[emb exp="2+3" format="%.0f"][r]
@if exp="2+3>4"
条件成立。[r]
@else
条件不成立。[r]
@endif
```

`eval` 保存值；`v` 是存放位置，`exp` 是计算式。保存原样文字用 `@eval v=nickname s="旅人"`，显示保存的文字用 `[emb var=nickname]`。条件结果大于 0 才算成立；可插入 `@elsif exp="..."`，最后必须写 `@endif`。这不是 JavaScript，不要直接粘贴 JavaScript 程序。

## 7. 屏幕、文字速度与声音

| 写法 | 用途 |
|---|---|
| `@hudDialog show=1` | 在游戏画面显示对话 |
| `@hudDialog show=0` | 关闭当前对话 |
| `@fullScreenDialog show=1 x=0.1 y=0.7 w=0.8` | 打开独立剧情屏幕并指定文字区域 |
| `@fullScreenDialog show=0` | 关闭当前剧情对话 |
| `@speed v=1` | 设置文字速度；效果需试读调整 |
| `@showTitle t="第一章" st="出发" in=10 show=60 out=10` | 标题、副标题及淡入、停留、淡出时间 |
| `@bgm n=命名空间:声音名` | 播放背景音乐 |
| `@stopbgm` | 停止背景音乐 |
| `@sound n=命名空间:声音名 repeat=0` | 播放声音 |
| `@stopAllsounds` | 停止声音 |

文字区域的 x、y、w 使用相对屏幕比例。声音名是游戏注册的声音资源名，不是电脑上的 MP3 路径。使用资源包已有名称，缺少资源时先请资源作者确认。

## 8. 加图片与转场（进阶）

画面按“开始编辑一层—放内容—显示这一层”组织。先打开剧情屏幕，再成对使用 startLayer 与 showLayer：

```text
@fullScreenDialog show=1
@startLayer
@ImageLayer n=background s=example:textures/gui/story.png x=0 y=0 w=320 h=180 z=0 opacity=1
@showLayer trans=crossfade t=20 x=0 y=0 w=1 h=1
```

`example:textures/gui/story.png` 是占位资源，请替换成真实图片。ImageLayer 的 x/y/w/h 是绘制矩形，sx/sy/sw/sh 是图片截取矩形；showLayer 的位置和大小是相对屏幕比例，不能混为一谈。不同窗口大小下都要检查显示效果。

`opacity` 从 0（透明）到 1（不透明）；z 是前后顺序。转场名称包括 crossfade、fadein、fadeout、up、down、left、right。`t` 是过渡时间。添加文字可用 TextLayer，矩形用 FillRect，线条用 DrawLine；精确参数见开发指南。复杂画面建议由开发者先做可复制模板。

## 9. 常见问题与试读清单

没有文字：检查路径、扩展名、语言版本和显示对话指令。服务器日志中搜索 `Loading scenario`、`not found` 或 `Exception loading scenario`。

文字挤在一起：加 `[r]` 换行和 `[p]` 分页。选项点不到：点击文字本身，确认 link/endlink 成对，并在选项之后写 wc。一直停住：确认停在普通等待还是事件等待；用测试存档重新跳到对应段落，不要在正式进度中反复跳转。

试读时逐项检查：两种分支都能进入；每页都能继续；声音能停止；关闭画面后能继续游戏；重新进入世界后进度符合预期。本文示例已按源码核对，尚未在客户端实际运行。
