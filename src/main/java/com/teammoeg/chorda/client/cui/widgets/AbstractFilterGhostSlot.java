package com.teammoeg.chorda.client.cui.widgets;

import com.teammoeg.chorda.client.CInputHelper;
import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.ScrollTracker;
import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.TooltipBuilder;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.icon.FlatIcon;
import com.teammoeg.chorda.client.ui.CGuiHelper;
import com.teammoeg.chorda.math.Colors;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public abstract class AbstractFilterGhostSlot extends UIElement {
    protected final ScrollTracker scrollTracker = new ScrollTracker();

    public AbstractFilterGhostSlot(UIElement parent) {
        super(parent);
    }

    // ---------- 子类必须实现 ----------

    /** 提供要绘制的物品栈，返回 ItemStack.EMPTY 表示无物品 */
    protected abstract ItemStack getDisplayStack();

    /** 当前数值（如请求数量、阈值） */
    protected abstract int getCurrentValue();

    /** 设置数值 */
    protected abstract void setValue(int newValue);

    /** 从手持物品设置过滤 */
    protected abstract void setFilterFromCarried();

    /** 清除过滤 */
    protected abstract void clearFilter();

    /** 获取菜单中玩家手持的物品 */
    protected abstract ItemStack getMenuCarried();

    // ---------- 可选覆盖的钩子 ----------

    /** 调整步长，支持 Shift/Ctrl 组合 */
    protected int getAdjustIncrement() {
        boolean shift = CInputHelper.isShiftKeyDown();
        boolean ctrl = CInputHelper.isCtrlKeyDown();
        return shift && ctrl ? 576 : (shift ? 16 : (ctrl ? 64 : 1));
    }

    /** 数值下限 */
    protected int getMinValue() { return 1; }

    /** 数值上限 */
    protected int getMaxValue() { return 1728; }

    /** 是否在物品图标上绘制数量文本 */
    protected boolean shouldDrawCount() { return true; }

    protected int getEffectiveValueForScroll() {
        int current = getCurrentValue();
        if (current == 1 && (CInputHelper.isShiftKeyDown() || CInputHelper.isCtrlKeyDown())) {
            return 0;
        }
        return current;
    }

    /** 左键点击且手上无物品、但槽位已有物品时的额外行为（如打开子层） */
    protected void onLeftClickWithoutCarried() {}

    /** 绘制背景（在物品之前绘制），默认空实现 */
    protected void renderBackground(GuiGraphics graphics, int x, int y, int w, int h, RenderingHint hint) {}

    // ---------- 核心渲染 ----------

    @Override
    public void render(GuiGraphics graphics, int x, int y, int w, int h, RenderingHint hint) {
        renderBackground(graphics, x, y, w, h, hint);

        ItemStack stack = getDisplayStack();
        if (!stack.isEmpty()) {
            CGuiHelper.drawItem(graphics, stack, x, y, 0, true, null);
            if (shouldDrawCount()) {
                int count = getCurrentValue();
                if (count != 1) {
                    {
                        drawCount(graphics, x, y, getCurrentValue());
                    }
                }
            }
        }

        super.render(graphics, x, y, w, h, hint);

        if (isMouseOver()) {
            graphics.fill(x, y, x + w, y + h, 300, Colors.setAlpha(Colors.WHITE, 0.25F));
        }
    }

    /** 绘制数量文本（右下角），可根据需要重写 */
    protected void drawCount(GuiGraphics graphics, int x, int y, int value) {
        String s = Integer.toString(value);
        Font font = getFont();
        int width = font.width(s);
        graphics.pose().pushPose();
        if (width > 16) {
            width = (int) (width * 0.8);
            graphics.pose().translate(x + 18 - width, y + 11, 350.0F);
            graphics.pose().scale(0.8f, 0.8f, 1f);
            graphics.drawString(font, s, 0, 0, 16777215, true);
        } else {
            graphics.pose().translate(0, 0, 350.0F);
            graphics.drawString(font, s, x + 18 - width, y + 9, 16777215, true);
        }
        graphics.pose().popPose();
    }

    // ---------- 交互逻辑 ----------

    @Override
    public boolean onMousePressed(MouseButton button) {
        if (!isMouseOver()) return super.onMousePressed(button);

        if (button == MouseButton.RIGHT) {
            if (getDisplayStack().isEmpty()) {
                setFilterFromCarried();
                setValue(1);
            } else {
                clearFilter();
            }
            return true;
        }

        if (button == MouseButton.LEFT) {
            if (!getMenuCarried().isEmpty()) {
                setFilterFromCarried();
            } else if (!getDisplayStack().isEmpty()) {
                onLeftClickWithoutCarried();
            }
            return true;
        }
        return super.onMousePressed(button);
    }

    @Override
    public boolean onMouseScrolled(double scroll) {
        if (!isMouseOver() || getDisplayStack().isEmpty()) return super.onMouseScrolled(scroll);

        scrollTracker.addScroll(scroll);
        int delta = scrollTracker.getScroll();
        if (delta != 0) {
            int old = getEffectiveValueForScroll();
            int newVal = Mth.clamp(old + delta * getAdjustIncrement(), getMinValue(), getMaxValue());
            setValue(newVal);
        }
        return true;
    }

    // -------- 默认 tooltip 实现
    @Override
    public void getTooltip(TooltipBuilder tooltip) {
        ItemStack stack = getDisplayStack();
        if (!stack.isEmpty()) {
            tooltip.add(Component.empty()
                    .append(stack.getHoverName())
                    .append(" x" + getCurrentValue()));
            tooltip.add(Component.empty()
                    .append(FlatIcon.INFO.toCTextIcon())
                    .append(" ")
                    .append(Component.translatable("gui.frostedheart.scroll_to_adjust"))
                    .append(Component.literal("±" + getAdjustIncrement()))
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("gui.frostedheart.adjust_increment")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }else {
            tooltip.add(Component.translatable("gui.frostedheart.empty_slot")
                    .withStyle(ChatFormatting.GRAY));
        }
        super.getTooltip(tooltip);
    }
}