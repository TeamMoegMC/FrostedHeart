# Chorda UI (CUI) 指南

- Status: `Current`
- Last verified: `2026-11-26`（选择器章节；其余章节沿用原核验）
- Scope: `com.teammoeg.chorda.client.cui` 的架构、生命周期、布局、输入、主题、适配器、控件库、内容面板、平移缩放视口与编辑器框架
- Code anchors: [`UIElement`](../src/main/java/com/teammoeg/chorda/client/cui/base/UIElement.java), [`UILayer`](../src/main/java/com/teammoeg/chorda/client/cui/base/UILayer.java), [`PrimaryLayer`](../src/main/java/com/teammoeg/chorda/client/cui/base/PrimaryLayer.java), [`CUIScreenWrapper`](../src/main/java/com/teammoeg/chorda/client/cui/screenadapter/CUIScreenWrapper.java), [`CUIMenuScreenWrapper`](../src/main/java/com/teammoeg/chorda/client/cui/screenadapter/CUIMenuScreenWrapper.java), [`CUIOverlay`](../src/main/java/com/teammoeg/chorda/client/cui/screenadapter/CUIOverlay.java), [`ContentPanel`](../src/main/java/com/teammoeg/chorda/client/cui/contentpanel/ContentPanel.java), [`Theme`](../src/main/java/com/teammoeg/chorda/client/cui/theme/Theme.java), [`PanZoomViewport`](../src/main/java/com/teammoeg/chorda/client/cui/base/PanZoomViewport.java), [`Editor`](../src/main/java/com/teammoeg/chorda/client/cui/editor/Editor.java)

本文说明当前实现，源码优先。CUI 是 Chorda 自带的声明式 UI 框架，游戏内界面（城镇、研究、知识、交易、物流等）
以及 HUD 覆盖层都建立在它之上。

## 1. 心智模型

- **一棵元素树**。`UIElement` 是根类；`UILayer` 是“容器 + 布局 + 滚动 + 输入分发”的抽象层；`PrimaryLayer`
  是每个界面的顶层，负责与 Minecraft `Screen` 交接。元素位置相对父元素，最终通过 `getScreenX/getScreenY` 落到屏幕坐标。
- **声明式重建**。`UILayer.refresh()` 会清空子元素，再调用 `addUIElements()` 重建、`alignWidgets()` 定位。
  界面状态应保存在字段里，而不是“改某个子元素的坐标然后指望它一直存在”。
- **主题延迟取色**。绘制不写死颜色，而是通过 `Coloring` 在渲染时向 `RenderingHint` 要当前 `Theme`，因此
  局部 `setTheme(...)` 能整体换皮。
- **输入自顶向下、后加先收**。`UILayer` 逆序遍历子元素分发事件；`onMousePressed` 返回 `true` 即消费。

## 2. 包结构

| 包 | 职责 |
|---|---|
| `cui.base` | 元素、图层、主层、焦点、工具提示、验证器、平移缩放视口 |
| `cui.widgets` | 按钮、复选框、文本框、滚动条、面板、物品槽等控件 |
| `cui.contentpanel` | 富文本内容面板与文本/图片/物品/分割线等行元素 |
| `cui.category` | 可折叠分类树（`Category`/`Entry`），档案类界面用 |
| `cui.editor` | 通用数据编辑器与编辑对话框 |
| `cui.menu` | 与容器菜单集成、可停用的槽位 |
| `cui.screenadapter` | Screen/菜单 Screen/覆盖层适配器与位置锚点 |
| `cui.theme` | 主题、颜色提供者、内置 `VanillaTheme` 与 `SimpleTechTheme` |

## 3. 生命周期

### 3.1 打开

纯 UI 界面：

```java
CUIScreenWrapper.open(new ExampleScreen());
```

容器菜单界面（背包槽位、服务端菜单）：

```java
// 注册一次
FHScreens.registerCUIScreen(FHMenuTypes.EXAMPLE.get(), ExampleMenuLayer::new);
// 服务端打开菜单后，客户端自动创建 CUIMenuScreenWrapper<ExampleMenu>
```

HUD 覆盖层（无 Screen）：

```java
// RegisterGuiOverlaysEvent 里注册；第 3 个参数决定何时允许交互
event.registerAboveAll("example_overlay", new CUIOverlay(ExampleOverlayLayer.INSTANCE, true));
event.registerAboveAll("example_tip", new CUIOverlay(TipLayer.INSTANCE, true, CUIOverlay.whenScreenOpened));
```

### 3.2 初始化与刷新

`PrimaryLayer.initGui()` 的顺序固定为：

1. `onInit()`：返回 `false` 可放弃初始化；通常在这里 `setSize(w, h)`。
2. `refresh()`：`clearElement()` → `addUIElements()`（先加对话框，再 `addChildUIElements()`）→ 逐个
   `element.refresh()` → `alignWidgets()`。
3. 之后每次 `refreshElements()` 只是打标记，下一帧渲染前由 `onBeforeRender()` 执行 `refresh()`。

`add()` 只接受 `getParent() == this` 的元素（构造函数传对父元素），否则记日志并拒绝。

### 3.3 每帧渲染

`CUIScreenWrapper`/`CUIMenuScreenWrapper`/`CUIOverlay` 都做同一套流程：

1. `onBeforeRender()`（处理延迟刷新）
2. `updateGui(originX, originY, mouseX, mouseY, partialTick)` → `updateRenderInfo` → `updateMouseOver()`
3. `render(graphics, x, y, w, h, hint)`：背景、子元素（`UILayer` 默认用 stencil 裁剪）、`drawForeground`
4. 收集 `getTooltip(builder)` 并绘制；更新鼠标指针 `getCursor()`

### 3.4 关闭

- `closeGui(openPrevScreen)`：关闭容器、按需回到上一屏、调用 `onClosed()`；`back()` 等价于 `closeGui(true)`。
- ESC/关闭键走 `shouldCloseOnEsc()` → `onCloseQuery()`；返回 `false` 可拦截。
- 鼠标侧键 4 与 `backspace` 在适配器里默认触发返回。

## 4. 坐标与元素 API

- `getX()/getY()` 相对父元素；`getScreenX()/getScreenY()` 逐级累加，`UILayer` 还会加上 `offsetX/offsetY`。
- `setPos(x, y)`、`setSize(w, h)`、`setPosAndSize(...)`；宽高最小为 0。
- 渲染入口 `render(graphics, x, y, w, h, hint)`，`x/y` 是父级换算后的屏幕坐标。
- 事件：`onMousePressed/DoubleClicked/Released/Scrolled/Dragged`、`onKeyPressed/Release`、`onIMEInput`。
- 提示：`getTitle()` + `getTooltip(TooltipBuilder)`，默认只在 `enabled && visible && mouseOver` 时收集。
- 其他钩子：`tick()`、`refresh()`、`onClosed()`、`getCursor()`、`getContentX/Y()`、`renderDebug(...)`。

## 5. 布局

`UILayer` 提供现成的线性/流式布局：

| 方法 | 说明 |
|---|---|
| `align(boolean horizontal)` | 依次排布，间距 1 |
| `align(int lineSpace, boolean horizontal)` | 依次排布，指定间距 |
| `align(int start, int lineSpace, boolean horizontal, UIElement... skip)` | 指定起始偏移并跳过元素 |
| `flow(boolean horizontalFirst)` | 按当前宽/高换行（网格） |

`Panel` 是带标题的层：构建时把子元素统一 `+5, +12` 偏移，高度留 12 像素边距；
`ContentPanel` 用 `align(4, false)` 纵向排列行元素。手工界面通常在 `alignWidgets()` 里显式
`setPosAndSize`，例如 `HeatStatScreen`。

## 6. 输入、焦点与滚动

- 分发顺序：`UILayer` 从最后添加的元素开始（顶层优先），`enabled && visible` 才参与；返回 `true` 停止传播。
- 焦点：实现 `Focusable`（`isFocused/setFocused`）的元素由 `PrimaryLayer.focusOn(element)` 管理，
  `TextBox` 在点击时请求焦点，`PrimaryLayer` 键盘事件优先发给当前焦点元素。
  - 焦点元素若已在 `refreshElements()` 重建中被移出元素树，`PrimaryLayer` 会先清除它的焦点，按键、滚轮和输入法字符
    不会再落到看不见的旧元素上。
  - 界面定时重建时若要保留输入框的焦点，需要在新输入框上调用 `setFocused(true)` 接过焦点。仓库的搜索框就是这样做的。
- 鼠标坐标：`getMouseX/Y` 在每帧渲染时更新。点击、松开、拖动和滚轮在分发前也会先按当前指针更新坐标和悬停状态
  （`isMouseOver`），所以事件里读到的是指针此刻的位置。以前拖动读的是上一帧的位置，快速拖动会少算一段：拖到第一行的
  条目会落在第二行。
- 侧栏按钮：铺满窗口的主层实现 `HidesSidebarButtons`，打开时其他模组画在屏幕边上的侧栏按钮（FTB Library）不画。
- 中文断行：原版 `Font.split` 总在空格处断行，中文句子里数字两边的空格常让一行提前折断。城镇界面用
  `TownTextLayout.breakLines(font, text, width)` 逐字排满并守避头尾；别的界面需要中文排版时也可以用它。
- 输入框（`TextBox`）：
  - `selectAll()` 选中全部文字，光标停在末尾，接着输入就整段替换。Ctrl+A 用的也是它。
  - `ghostText` 默认和原版一样，聚焦后就隐藏。一打开就聚焦的输入框可以把 `showGhostWhenFocused` 设为 `true`，
    清空时提示照样显示。布告板的改名框（`RenameBox`）打开时全选旧名字，清空后提示留空会恢复成什么。
- 滚动条：
  - `ScrollBar(parent, isVertical, thumbSize)`：通用滚动条，支持拖拽、点击轨道翻页、滚轮。
  - `LayerScrollBar(parent, affectedLayer)`：绑定到某个 `UILayer`，自动计算 `max/page/thumb`，
    滚动时设置层的 `offsetY`（或 `offsetX`），并开启平滑滚动。
  - `LevelSpinLayer`：离散档位选择器（左右箭头 + 档位）。
- 注意：`UILayer.onMouseScrolled` 里“无滚动条时自动滚动”的分支当前是注释掉的，因此**要让层响应滚轮，
  需要放一个 `LayerScrollBar`**（或自己重写 `onMouseScrolled`）。

## 7. 主题

`Theme` 定义按钮/面板/输入框/槽位/工具提示的绘制与文字颜色。绘制时通过
`hint.theme(element)` 解析：`themeOverride` → 当前渲染中的 `currentTheme`（由外层 `UILayer` 设置）→ `themeDefault`。
因此：

- 给某个 `UILayer`（含 `PrimaryLayer`）`setTheme(theme)`，其整棵子树都换主题；
- 颜色一律用 `UIColors.*`（`Coloring` 实现）或自定义 `Coloring`，不要在控件里写死 ARGB；
- 内置主题：`VanillaTheme.INSTANCE`（默认）、`SimpleTechTheme.INSTANCE`；档案界面还有自己的 `ArchiveTheme`，城镇界面用 `NoticeBoardTheme`（木板底加布告纸，`content/town/client/board`）。
  - `NoticeBoardTheme` 也重写了 `drawSlot`（压在纸上的格子）、`drawSliderBackground` 与 `drawSliderBar`（纸槽与纸签），
    所以用主题画格子和滚动条的控件（例如仓库的 `VirtualItemGridElement`）放到布告板上会自动换成纸面样式。

### 7.1 变形

`UILayer` 可以像 CSS 那样整体倾斜、旋转或缩放，内部控件照常响应鼠标：

```java
card.setTransform(CSSStylingUtil.rotate(0.5f));      // 也有 skewX/skewY/scale/rotateX/rotateY/perspective
card.setTransformOrigin(new Vector2f(.5f, .5f));      // 变形中心，按层宽高的比例，默认居中
card.mulTransform(CSSStylingUtil.skewX(-2f));         // 叠加
```

- 渲染时变形作用于整棵子树和裁剪模板；`updateRenderInfo` 用逆矩阵把鼠标换回层内坐标，所以按钮、文本框在倾斜的层里照样能点。
- 变形中心取层自己的屏幕位置（2026-11-10 修正：此前鼠标换算漏加了层的 `x/y`，只有位于父层原点的层才命中准确）。
- 参考：`ArchiveScreen` 的翻页倾斜、城镇布告板的 `PaperCard`。

### 7.2 进度条与染色贴图

- `ProgressBar(parent, progress, color)`：每帧从供应器读 0–1 的进度和颜色，由主题的 `drawProgressBar` 绘制。
  `Theme.drawProgressBar` 有纯色默认实现，主题可以换成贴图（例如 `NoticeBoardTheme` 的纸槽）。
- `CTextureIcon.drawTinted(graphics, x, y, w, h, argb)`：乘上颜色后绘制，九宫格同样适用；白色贴图可以染成任意颜色（印戳、进度填充）。
  普通 `draw` 会先 `resetGuiDrawing()` 把着色器颜色复位，所以不能靠事先 `setShaderColor` 染色。
- `CInputHelper.playUISound(sound, pitch)`：播放与位置无关的界面音效（翻纸、盖章）。

## 8. 适配器与位置

| 适配器 | 用途 |
|---|---|
| `CUIScreenWrapper` | 纯 UI 界面（无容器菜单），`open(layer)` |
| `CUIMenuScreenWrapper<T>` | 容器菜单界面，配合 `FHScreens.registerCUIScreen` / `CUIScreenFactory` |
| `CUIOverlay` | HUD 覆盖层；`renderAboveScreen` 决定是否画在 Screen 之上；`whenVisibleAndEnabled`/`whenScreenOpened`/`whenScreenClosed` 控制交互；自动注册 JEI 排除区域；支持鼠标捕获与虚拟指针 |
| `OverlayPositioner` | 9 宫格锚点与屏幕边界避让，返回 `Point` |
| `MenuPrimaryLayer<T>` | 菜单主层基类，自动把鼠标下槽位的物品提示并入 tooltip |

覆盖层的状态提醒：`CUIOverlay` 自己负责 tick、滚轮/键盘/鼠标事件分发和窗口尺寸变化时的 `initGui()`；
`renderAboveScreen=true` 的覆盖层在 `ScreenEvent.Render.Post` 里绘制。

## 9. 控件库

| 控件 | 关键点 |
|---|---|
| `Button` | 抽象基类，`onClicked(MouseButton)`；`setTitle/setIcon` 自动 `fitSize`；默认 `Cursor.HAND` |
| `TextButton` | 文本按钮；`create(parent, title, icon, callback, tooltips...)`、`accept(...)`、`cancel(...)`；文本过长自动截断并给 tooltip |
| `ImageButton` | 抽象三态图按钮（normal/over/pressed） |
| `FlatIconButton` | 内容面板里的紧凑图标按钮（`FlatIcon`） |
| `CheckBox` | `CheckBox(parent, uncheckedIcon, checkedIcon, initial)`，`onSwitched()` |
| `TristateCheckBox` | 正常/悬停/锁定三态，锁定不可交互 |
| `TextBox` | 完整文本编辑（选择、复制粘贴、撤销、校验、占位文本）；**父元素必须是 `UILayer`**；`setFilter(Verifier)`、`onTextChanged()`、`onEnterPressed()`、`onTabPressed()` |
| `LimitedTextField` | 定宽只读文本，超出省略号 + 悬停全文 |
| `TextField` | 只读富文本显示，支持换行、缩放、行数限制、阴影、颜色 |
| `ItemSlot` | 显示 `ItemStack`（多物品按秒轮播）；左键/右键/R/U 接 JEI |
| `OverlayItemSlot` | 物品槽 + 角标图标 + 自定义提示，常用于“配方结果/条件” |
| `VanillaItemSlot` | 原版外观的槽位背景 |
| `AbstractFilterGhostSlot` | 过滤用幽灵槽基类（带滚轮步进） |
| `Panel` | 带标题面板，子元素自动内缩 |
| `SimpleLayer` | 空白层，纯手工布局 |
| `ToolTipElement` | 不可见元素，只为悬停提示 |
| `ScrollBar` / `LayerScrollBar` | 见第 6 节 |
| `TabImageButtonElement` | 标签页按钮（激活/未激活图标） |
| `LevelSpinLayer` | 档位选择器 |
| `ProgressBar` | 进度条，颜色与进度来自供应器，外观由主题决定 |

菜单集成：`CUISlot`（`Container` 槽位）、`CUISlotItemHandler`（`IItemHandler` 槽位）实现
`DeactivatableSlot`，可运行时启停。

## 10. 内容面板与行元素

`ContentPanel` 是列表/说明页的主力：默认尺寸为屏幕高度的 80% × 4:3，自动管理滚动条。

```java
ContentPanel panel = new ContentPanel(this).builder()
        .text(Component.translatable("example.title"), t -> t.alignment(Alignment.CENTER).title(UIColors.UI_BORDER, 2))
        .br()
        .text("普通文本，自动换行")
        .items(new ItemStack(Items.IRON_INGOT, 4), new ItemStack(Items.COPPER_INGOT))
        .space(8)
        .wrap(TextButton.accept(this, b -> { }))
        .build();
```

- `Builder` 提供 `text/img/items/space/br/wrap/add`，最终 `build()` 触发 `refresh()`。
- 行元素：`TextLine`（`scale/title/quote/shadow/button`）、`ImageLine`、`ItemRow`、`EmptyLine`、
  `BreakLine`、`UIEleWrapperLine`；没有 Builder 时可用 `LineHelper.text/img/items/space/br/warp`。
- 动态内容用 `fillContent(Collection<UIElement>)` 或 `addLine(Line<?>)`；内容高度变化会重置滚动位置。
- 面板的 `scrollBar` 是独立元素，必须和面板一起加到父层（`setParent(layer)` 会自动加，或手动
  `add(panel); add(panel.scrollBar)`）。

## 11. 平移缩放视口

`PanZoomViewport` 是地图/节点图/大画布的通用视口：

- 相机 `Camera(x, y, zoom)`；`centerOn`、`fitToBounds(bounds, padding)`、`zoomAt(...)`、`zoomByWheel(...)`。
- 世界 ↔ 局部坐标：`worldToLocalX/Y`、`screenToWorldX/Y`、`visibleWorldMin/Max`、`isWorldRectVisible`。
- 子类重写：`drawViewportBackground`、`drawViewportContent`（在裁剪内绘制虚拟内容）、`drawViewportOverlay`、
  `constrainCamera`、`onCameraChanged`、`isPanStartBlocked`、`canPanWith`。
- 默认左键/中键拖拽平移，滚轮按 `zoomStep` 指数缩放；`setScissorEnabled(false)`，裁剪由自身 `enableScissor` 完成。
- 参考实现：`ResearchGraphViewport`、`KnowledgeGraphViewport`。

## 12. 编辑器框架

`Editor<T>` 是函数式接口：`open(parent, label, previousValue, onCommit)`，可组合
`xmap`（类型映射）、`withDefault`、`withValue`、`addOnChangeAction`、`beforeOpen`。

- 预置：`Editors.CONFIRM_DIALOG`、`TEXT_PROMPT`、`INT_PROMPT`、`LONG_PROMPT`、`REAL_PROMPT`、
  `EDITOR_FULL_ITEM`/`EDITOR_SIMPLE_ITEM`、`EDITOR_BLOCK`、`STACK_LIST`/`BLOCK_LIST`、`EDITOR_ENTITY`、
  `EDITOR_ADVANCEMENT` 等。
- 对话框：`EditPrompt`、`EditPromptWithSelect`、`SelectDialog`、`SelectStackDialog`、`EditListDialog`、
  `EditorFieldsDialog`；`PrimaryLayer` 通过 `openDialog(dialog, refresh)` / `closeDialog(quit)` 管理。
- 没有现成界面时：`EditUtils.edit(editor, title, oldValue, onChange)`（内部 `openEditorScreen()`）。
- 表单字段工厂：`Editors.INT/DOUBLE/STRING/BOOLEAN/RESOURCELOCATION/...` 与 `openDialog(...)` 可直接嵌进
  `EditorFieldsDialog`。

## 13. 调试

- `F12` 切换 CUI 调试模式（`PrimaryLayer` 拦截）。
- 调试模式：把鼠标移到元素上，`Alt` 隐藏框线，`Ctrl` 显示元素信息（标题、类名、父类、局部/屏幕坐标、
  Enable/Visible），`Shift` 显示隐藏子元素。
- 代码入口：`CUIDebugHelper.toggleDebug()`、`UILayer.hoveredEle`（调试渲染时写入）。

## 14. 最小起手式

```java
public final class ExampleScreen extends PrimaryLayer {
    private final ContentPanel panel;

    public ExampleScreen() {
        setTheme(VanillaTheme.INSTANCE);          // 可省略，默认就是 Vanilla
        panel = new ContentPanel(this).builder()
                .text(Component.literal("Hello CUI"), t -> t.alignment(Alignment.CENTER))
                .wrap(TextButton.create(this, Component.literal("关闭"), FlatIcon.LEAVE.toCIcon(),
                        b -> closeGui(true)))
                .build();
    }

    @Override
    public void addChildUIElements() {
        panel.setParent(this);                    // 加面板 + 滚动条
    }

    public static void open() {
        CUIScreenWrapper.open(new ExampleScreen());
    }
}
```

`PrimaryLayer.alignWidgets()` 默认用内容尺寸决定窗口大小，所以尺寸通常由内容自然决定。要固定尺寸就在构造里
`setSize(w, h)` 并重写空的 `alignWidgets()`（参考 `WarehouseInterfaceScreen`）；只是想在 `onInit` 里设尺寸
不够，因为随后的 `alignWidgets()` 会覆盖它。

菜单版把 `extends PrimaryLayer` 换成 `extends MenuPrimaryLayer<ExampleMenu>`，用
`FHScreens.registerCUIScreen(...)` 注册工厂即可。

## 15. 常见坑

1. **父元素必须正确**：`new Widget(otherElement)` 后用 `layer.add(widget)` 会被拒绝（日志 warn）。
2. **`refresh()` 是重建**：`addUIElements()` 里重新 `new` 元素；不要缓存元素引用后指望刷新后还有效。
3. **运行期改结构用 `refreshElements()`**：在事件回调里直接 `refresh()` 会丢掉滚动、焦点等即时状态。
4. **`LayerScrollBar` 不支持手动 `setMaxValue/setPageStep`**（会抛 `UnsupportedOperationException`），
   大小由目标层内容决定。
5. **没有滚动条就没有滚轮**：`UILayer` 的默认滚轮分支已注释，请放 `LayerScrollBar` 或自行处理。
6. **`ContentPanel.scrollBar` 要单独入父层**，否则滚动条不显示；`refresh()` 里会重算面板宽度。
7. **不要写死颜色**：用 `UIColors`/`Coloring`，否则换主题后局部颜色不跟随。
8. **`fitToBounds` 需要已有尺寸**：`PanZoomViewport` 宽高为 0 时返回 `false`，先 `setSize`。
9. **裁剪**：`PrimaryLayer` 关闭了 stencil 裁剪；子层默认开启。若在视口里自绘虚拟内容，请自己
    `enableScissor` 或继承 `PanZoomViewport`。

## 16. 参考实现

| 场景 | 类 |
|---|---|
| 纯 UI 界面 + 内容面板 | `ConfigScreen`、`KeyHintConfigScreen` |
| 档案/分类树 + 双面板 | `ArchiveScreen`、`Category` |
| 菜单界面 | `TradeScreen`、`WarehouseInterfaceScreen`、`TownManagerScreen` |
| HUD 覆盖层 | `TipOverlay`、`KeyHintOverlay`、`DialogueOverlay`、`PopupOverlay` |
| 平移缩放视口 | `ResearchGraphViewport`、`KnowledgeGraphViewport` |
| 编辑器 | `EditPrompt`、`SelectStackDialog`、`EditorFieldsDialog` |

## 17. 最小验证

```text
./gradlew compileJava
./gradlew test --tests "com.teammoeg.frostedresearch.*"
```

CUI 是客户端框架，行为验证以实机界面为主；改动后至少确认：界面能打开、滚动条可用、ESC/侧键能返回、
容器菜单槽位正常、HUD 覆盖层交互条件符合预期。

## 元素选择器

完整命令参数、返回格式、构造器规则及操作示例见 [选择器与客户端调试工具使用手册](chorda-cui-tools.md)。

`UIElement.setName/getName` 提供可选名称，`#name` 匹配名称；`.Button` 等 class
来自 `getSelectorClasses()`：当前类名（匿名类略过）、当前类直接接口、父类名、父类直接接口，
依次到 UIElement；不递归接口继承。名称和类名区分大小写，支持 Java 标识符字符及连字符。

`UIElement.querySelector(String)` 在当前子树（包括自身）按子元素顺序深度优先查询，返回首个匹配，可选择任意深度的元素。
空选择器、无匹配返回 null。支持 `*`、`.Button#confirm` 复合匹配、空格后代路径、
`>` 直接子节点路径及从 1 开始的 `[n]`。不实现完整浏览器 CSS 语法。
空格匹配任意深度后代，`>` 仅匹配直接子节点；支持完整 debug selector 回查。
UILayer 通过子节点钩子逐层委派；叶子匹配完成即返回自身，无后续子层时返回 null。
`ClientUtils.querySelector(String)` 查询当前打开 Screen 的 CUI 主层，非 CUI Screen 返回 null。

`UIElement.getUniqueSelector()` 生成从根开始、以 ` > ` 连接的唯一路径，debug tooltip 显示
`Selector:`。每一层按以下优先级选择片段：可解析且在同级唯一的 `#name`；同级唯一的
`.当前类名`；同级唯一的 `.直接父类名`；最后使用从 1 开始的 `*[n]`。
匿名类没有当前类名，不以接口或更深的父类名作为候选。唯一性按查询器的实际匹配规则
检查，包括兄弟节点通过继承或接口匹配到同一类名的情况；name 重复或包含不支持字符时回退。
例如 `.DrawDeskScreen > .ResearchArchiveLayer > #search`。
未加入父容器的元素返回 null。名称、类型、树结构及子节点顺序不变时可回查同一元素；
界面 refresh 重建或这些属性变化后应重新取得路径。

### 开发客户端控制桥

使用 `Scripts/dev_client_bridge.init.gradle` 启动客户端后，可用 `Scripts/agent_client.py` 发送：

- `cui-elements`：当前 CUI 根的直接子元素一层，按子元素顺序返回 `elements` 数组，包含隐藏和禁用元素，不包含根。
- `cui-query <selector>`：在当前根及所有下级元素中返回首个匹配的 `element`，不受枚举的一层限制，未命中明确返回 JSON null。
- `cui-get {"selector":".TextBox","field":"name"}`：反射读取字段，查找规则与 set 相同，返回 field、declaringClass、fieldType、value 和元素 debug 信息；null 值保留为 JSON null。
- `cui-set {"selector":".TextBox","field":"name","value":"search"}`：沿匹配元素的类继承链查找字段（优先最近声明，支持 private），按字段声明类型从 JSON 转换后反射赋值。

元素 debug 数据包含 selector、name、type、classes、parentSelector、title、局部 rect、
与 UIElement.getScreenX/Y 一致的 screenRect、enabled、visible；不递归嵌套 children。
非 CUI 界面返回命令错误；set 未匹配、字段不存在、类型不兼容或不可写也返回错误。
只修改实例字段，不修改 static 字段。set 返回 declaringClass、before、after 和修改后的元素信息；
标量保持 JSON 类型，其他对象以字符串描述，避免展开 UI 对象图。
value 使用 Gson 对声明类型的转换，常用值为字符串、数字、布尔、枚举名及引用类型的 null。
这是直接字段赋值，不调用 setter、不刷新或重建 UI；修改 name 后应使用返回的新 selector 继续查询。

### 动态增加与移除子元素

`cui-remove <selector>` 从匹配元素的直接父 UILayer 调用 `remove(UIElement)`，
返回移除前的 `removed` debug 信息和更新后的 `parent` 信息。根节点、非容器父节点、
未命中或容器拒绝操作均返回命令错误。UILayer.remove 清除滚动条引用、使内容尺寸缓存失效并调用 onClosed。

`cui-add {"selector":".ResearchArchiveLayer","class":"com.teammoeg.chorda.client.cui.widgets.TextField","args":[]}`
在匹配的 UILayer 内构造并添加元素，返回添加后的 `element` debug 信息。
`class` 必须是 UIElement 子类的全限定名称；构造器第一个参数自动传入选中的父元素，
`args` 只提供其余参数（可省略，默认为空数组），使用 Gson 按声明的泛型参数类型反序列化。
不指定签名时，按构造器签名字典序依次尝试参数数量为 args 数量加一、首参兼容父元素的构造器，
转换或构造失败后继续尝试，全部失败返回各候选失败原因；构造器中的副作用不会自动撤销。

可选 `parameterTypes` 指定完整构造器参数类型列表，**包含父元素参数类型**，例如
`["com.teammoeg.chorda.client.cui.base.UIElement"]`；此时精确查找，不尝试其他重载。
类型使用全限定名称或 int、boolean 等基本类型名，支持 `[]` 数组和 `$` 嵌套类名。
支持非 public 构造器。仅在构造成功后调用容器 add；父元素不支持添加或拒绝 add 时返回错误。
不会自动 refresh 或重新布局，以免重建界面并覆盖动态修改；新元素使用构造后的实际尺寸与位置。
增删后索引和类名唯一性可能变化，继续操作前应重新取得 selector。
