/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.chorda.client.cui.base;

import com.teammoeg.chorda.client.CInputHelper.Cursor;
import com.teammoeg.chorda.client.MouseHelper;
import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.screenadapter.CUIScreen;
import com.teammoeg.chorda.client.cui.theme.Theme;
import com.teammoeg.chorda.client.cui.theme.VanillaTheme;
import com.teammoeg.chorda.math.Rect;
import com.teammoeg.chorda.text.Components;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
/**
 * Chorda UI框架的抽象UI元素基类，所有UI组件的根类。
 * 提供位置、尺寸、鼠标交互、键盘事件、工具提示、主题和渲染等基础功能。
 * 每个元素都有一个父元素引用，形成树形层级结构。
 * <p>
 * Abstract base class for all UI elements in the CUI framework.
 * Provides fundamental capabilities including position, size, mouse interaction,
 * keyboard events, tooltips, theming, and rendering. Each element holds a parent
 * reference, forming a tree-based hierarchy.
 */
public class UIElement {
    /**
     * 获取父元素。
     * @return 当前元素的父节点，可能为 null（顶层元素）
     */
    @Getter
    protected UIElement parent;

    /**
     * 获取/设置元素在父坐标系中的 X 坐标（像素）。
     * 通过 getX() / setX() 方法访问。
     */
    @Getter
    @Setter
    private int x;

    /**
     * 获取/设置元素在父坐标系中的 Y 坐标（像素）。
     * 通过 getY() / setY() 方法访问。
     */
    @Getter
    @Setter
    private int y;

    /**
     * 获取元素宽度（像素）。
     * 通过 getWidth() 方法访问。
     */
    @Getter
    protected int width;

    /**
     * 获取元素高度（像素）。
     * 通过 getHeight() 方法访问。
     */
    @Getter
    protected int height;

    protected boolean isMouseOver;
    private LayerHolder layerholderCache;

    /**
     * 获取鼠标在元素局部坐标系中的 X 坐标。
     * 通过 getMouseX() 方法访问。
     */
    @Getter
    private double mouseX;

    /**
     * 获取鼠标在元素局部坐标系中的 Y 坐标。
     * 通过 getMouseY() 方法访问。
     */
    @Getter
    private double mouseY;

    /**
     * 获取渲染时使用的部分刻度（用于插值动画）。
     * 通过 getPartialTick() 方法访问。
     */
    @Getter
    private float partialTick;

    /**
     * 设置/获取元素的可见性。
     * 通过 setVisible(boolean) / isVisible() 方法访问。
     */
    @Setter
    @Getter
    private boolean visible=true;

    /**
     * 设置/获取元素的启用状态（是否响应用户交互）。
     * 通过 setEnabled(boolean) / isEnabled() 方法访问。
     */
    @Setter
    @Getter
    private boolean enabled=true;

    /**
     * 设置元素的主题（由 Lombok 生成 setter）。
     * 通过 setTheme(Theme) 方法访问。
     */
    @Setter
    private Theme theme = VanillaTheme.INSTANCE;

    /**
     * 获取当前主题（自定义 getter 方法，非 Lombok 生成）。
     * @return 当前主题实例
     */
    public Theme theme() {
        return theme;
    }

    // ==================== 构造函数 ====================
    /**
     * 构造一个 UI 元素，并指定父节点。
     * @param parent 父元素，如果为 null 则表示顶层元素
     */
    public UIElement(UIElement parent) {
        this.parent = parent;
        if (parent != null)
            this.theme = parent.theme();
        //CUIDebugHelper.registerUIObject(this);
    }

    // ==================== 层级与位置相关方法 ====================
    /**
     * 设置父元素（内部使用）。
     * @param parent 新的父元素
     */
    protected void setParent(UIElement parent) {
        this.parent = parent;
        if (parent != null) {
            this.theme = parent.theme();
            layerholderCache = null;
        }
    }

    /**
     * 获取当前元素所在的图层持有者（LayerHolder）。
     * 通过父元素链向上查找，缓存结果。
     * @return 图层持有者实例
     */
    public LayerHolder getLayerHolder() {
        if (layerholderCache == null)
            layerholderCache = parent.getLayerHolder();
        return layerholderCache;
    }

    /**
     * 设置元素宽度，最小值限制为 0。
     * @param v 宽度值（像素）
     */
    public void setWidth(int v) {
        width = Math.max(v, 0);
    }

    /**
     * 设置元素高度，最小值限制为 0。
     * @param v 高度值（像素）
     */
    public void setHeight(int v) {
        height = Math.max(v, 0);
    }

    /**
     * 设置元素在父坐标系中的位置。
     * @param x X 坐标
     * @param y Y 坐标
     */
    public void setPos(int x, int y) {
        setX(x);
        setY(y);
    }

    /**
     * 设置元素尺寸。
     * @param w 宽度
     * @param h 高度
     */
    public void setSize(int w, int h) {
        setWidth(w);
        setHeight(h);
    }

    /**
     * 链式调用：同时设置位置和尺寸。
     * @param x X 坐标
     * @param y Y 坐标
     * @param w 宽度
     * @param h 高度
     * @return 当前元素自身，便于链式调用
     */
    public UIElement setPosAndSize(int x, int y, int w, int h) {
        setX(x);
        setY(y);
        setWidth(w);
        setHeight(h);
        return this;
    }

    /**
     * 获取元素在屏幕坐标系中的 X 坐标。
     * @return 屏幕 X 坐标
     */
    public int getScreenX() {
        int x = parent.getScreenX() + this.x;
        if (parent instanceof UILayer layer) {
            x += layer.getOffsetX();
        }
        return x;
    }

    /**
     * 获取元素在屏幕坐标系中的 Y 坐标。
     * @return 屏幕 Y 坐标
     */
    public int getScreenY() {
        int y = parent.getScreenY() + this.y;
        if (parent instanceof UILayer layer) {
            y += layer.getOffsetY();
        }
        return y;
    }

    // ==================== 工具提示相关 ====================
    /**
     * 获取元素的标题组件（用于工具提示）。
     * 默认返回空组件，子类可覆盖。
     * @return 标题组件
     */
    public Component getTitle() {
        return Components.immutableEmpty();
    }

    /**
     * 构建工具提示内容。
     * 如果标题非空，则将其添加到 tooltip 构建器中。
     * @param tooltip 工具提示构建器
     */
    public void getTooltip(TooltipBuilder tooltip) {
        Component title = getTitle();

        if (!Components.isEmpty(title)) {
            tooltip.accept(title);
        }
    }

    // ==================== 鼠标交互相关 ====================
    /**
     * 获取鼠标是否悬停在该元素上（仅表示状态，需调用 updateMouseOver 刷新）。
     * @return true 表示鼠标在当前元素内
     */
    public final boolean isMouseOver() {
        return isMouseOver;
    }

    /**
     * 更新鼠标悬停状态。根据父元素悬停状态及鼠标相对于当前元素的局部坐标判断。
     * 应在每帧鼠标位置更新后调用。
     */
    public void updateMouseOver() {
        if (parent == null) {
            isMouseOver = true;
            return;
        } else if (!parent.isMouseOver()) {
            isMouseOver = false;
            return;
        }

        isMouseOver = MouseHelper.isMouseIn(this.getMouseX(), this.getMouseY(), 0, 0, this.getWidth(), this.getHeight());
    }

    /**
     * 获取当前元素使用的字体（从图层持有者获取）。
     * @return 字体实例
     */
    public Font getFont() {
        return getLayerHolder().getFont();
    }

    /**
     * 判断当前元素是否应该显示工具提示。
     * 默认条件：元素启用、可见且鼠标悬停。
     * @return true 表示应显示工具提示
     */
    public boolean hasTooltip() {
        return isEnabled() && isVisible() && isMouseOver();
    }

    // ==================== 渲染方法 ====================
    /**
     * 渲染元素内容。子类需重写此方法实现自定义绘制。
     * @param graphics GuiGraphics 对象，用于绘图操作
     * @param x        元素在屏幕上的 X 坐标（左上角）
     * @param y        元素在屏幕上的 Y 坐标（左上角）
     * @param w        元素宽度
     * @param h        元素高度
     * @param hint     渲染提示，包含其他不常修改的渲染参数
     */
    public void render(GuiGraphics graphics, int x, int y, int w, int h, RenderingHint hint) {
    }

    // ==================== 事件处理 ====================
    /**
     * 鼠标按下事件。
     * @param button 按下的鼠标按钮
     * @return true 表示事件已处理，不再向下传递
     */
    public boolean onMousePressed(MouseButton button) {
        return false;
    }

    /**
     * 鼠标双击事件。
     * @param button 双击的鼠标按钮
     * @return true 表示事件已处理
     */
    public boolean onMouseDoubleClicked(MouseButton button) {
        return false;
    }

    /**
     * 鼠标释放事件。
     * @param button 释放的鼠标按钮
     */
    public void onMouseReleased(MouseButton button) {
    }

    /**
     * 鼠标滚轮滚动事件。
     * @param scroll 滚动量（正值向上，负值向下）
     * @return true 表示事件已处理
     */
    public boolean onMouseScrolled(double scroll) {
        return false;
    }

    /**
     * 鼠标拖拽事件（在鼠标按下并移动时触发）。
     * @param button 按下的鼠标按钮
     * @param dragX  拖拽的 X 偏移量
     * @param dragY  拖拽的 Y 偏移量
     * @return true 表示事件已处理
     */
    public boolean onMouseDragged(MouseButton button, double dragX, double dragY) {
        return false;
    }

    /**
     * 键盘按键按下事件。
     * @param keyCode   按键代码
     * @param scanCode  扫描码
     * @param modifier  修饰符（Ctrl、Alt、Shift 等）
     * @return true 表示事件已处理
     */
    public boolean onKeyPressed(int keyCode, int scanCode, int modifier) {
        return false;
    }

    /**
     * 键盘按键释放事件。
     * @param keyCode   按键代码
     * @param scanCode  扫描码
     * @param modifier  修饰符
     * @return true 表示事件已处理
     */
    public boolean onKeyRelease(int keyCode, int scanCode, int modifier) {
        return false;
    }

    /**
     * IME 输入事件（用于中文等输入法）。
     * @param c        输入的字符
     * @param modifier 修饰符
     * @return true 表示事件已处理
     */
    public boolean onIMEInput(char c, int modifier) {
        return false;
    }

    // ==================== 更新与生命周期 ====================
    /**
     * 更新渲染相关信息：鼠标坐标、部分刻度。
     * @param x  元素在屏幕上的 X 坐标（由父级提供）
     * @param y  元素在屏幕上的 Y 坐标
     * @param mx 全局鼠标 X 坐标
     * @param my 全局鼠标 Y 坐标
     * @param pt 部分刻度（用于动画插值）
     */
    public void updateRenderInfo(int x, int y, double mx, double my, float pt) {
        this.mouseX = mx - this.getX() - x;
        this.mouseY = my - this.getY() - y;
        if (pt > 0)
            this.partialTick = pt;
    }

    /**
     * 元素关闭时调用的回调（如从界面移除）。
     */
    public void onClosed() {
    }

    /**
     * 每帧更新的逻辑（用于非渲染的状态更新）。
     */
    public void tick() {
        //layerholderCache=null;
    }

    /**
     * 获取元素内容区域的 X 坐标（默认为屏幕坐标，子类可重写以实现内边距等）。
     * @return 内容区域 X 坐标
     */
    public int getContentX() {
        return getScreenX();
    }

    /**
     * 获取元素内容区域的 Y 坐标。
     * @return 内容区域 Y 坐标
     */
    public int getContentY() {
        return getScreenY();
    }

    /**
     * 获取鼠标悬停时应显示的鼠标指针样式。
     * @return 鼠标指针类型，null 表示使用默认
     */
    public Cursor getCursor() {
        return null;
    }

    /**
     * 刷新元素状态（如重新计算布局）。
     */
    public void refresh() {

    }

    /**
     * 获取当前元素所属的 CUIScreen（顶层管理器）。
     * @return CUIScreen 实例
     */
    public CUIScreen getManager() {
        return parent.getManager();
    }

    /**
     * 获取元素在屏幕上的边界矩形。
     * @return 矩形对象，包含屏幕坐标和尺寸
     */
    public Rect getBounds() {
        return new Rect(getScreenX(), getScreenY(), getWidth(), getHeight());
    }
}