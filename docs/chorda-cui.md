# Chorda UI (CUI) 指南

- Status: `Current`
- Last verified: `2026-10-19`
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
- 内置主题：`VanillaTheme.INSTANCE`（默认）、`SimpleTechTheme.INSTANCE`；档案界面还有自己的 `ArchiveTheme`。

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
