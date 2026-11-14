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

package com.teammoeg.frostedheart.content.town.buildings.warehouse;

import java.util.List;
import java.util.function.Supplier;

import com.mojang.blaze3d.systems.RenderSystem;
import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.TooltipBuilder;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.content.town.network.WarehouseInteractPacket;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public class VirtualItemGridElement extends UIElement{
    private final Supplier<List<VirtualItemStack>> itemSource; // 数据源
    private float scrollOffset = 0.0f;
    private boolean isScrolling = false;

    private final int cols;
    private final int rows;
    private final int slotSize = 18;

    private final int scrollBarWidth = 12;

    public VirtualItemGridElement(UIElement parent, int x, int y, Supplier<List<VirtualItemStack>> itemSource) {
        this(parent, x, y, 9, 5, itemSource);
    }

    /** A grid of {@code cols} × {@code rows} cells; slots and the scroll bar are drawn by the screen's theme. */
    public VirtualItemGridElement(UIElement parent, int x, int y, int cols, int rows, Supplier<List<VirtualItemStack>> itemSource) {
        super(parent);
        this.itemSource = itemSource;
        this.cols = Math.max(1, cols);
        this.rows = Math.max(1, rows);
        this.setPos(x, y);
        this.setSize(this.cols * slotSize + 2 + scrollBarWidth, this.rows * slotSize);
    }

    /** Width of a grid with {@code cols} columns, scroll bar included. */
    public static int widthFor(int cols) {
        return cols * 18 + 2 + 12;
    }

    private static final int MIN_RULED_ROWS = 3;
    private boolean compactRows;

    /** Draws cells only for the rows in use plus a spare one, instead of the whole grid. */
    public VirtualItemGridElement compactRows() {
        compactRows = true;
        return this;
    }

    /** Scrolls back to the top, e.g. when the search changes. */
    public void resetScroll() {
        scrollOffset = 0;
    }


    @Override
    public void render(GuiGraphics guiGraphics, int x, int y, int w, int h, RenderingHint hint) {
        //参数x, y 是屏幕绝对坐标
        //槽位背景绘制
        List<VirtualItemStack> itemList = itemSource.get();
        // Rule only the rows in use and one spare, like entries in a ledger; the rest of the page stays blank.
        int usedRows = itemList == null ? 0 : (int) Math.ceil((double) itemList.size() / cols);
        int ruled = compactRows ? Math.min(rows, Math.max(MIN_RULED_ROWS, usedRows + 1)) : rows;
        for (int r = 0; r < ruled; r++) {
            for (int c = 0; c < cols; c++) {
                hint.theme(this).drawSlot(guiGraphics, x + c * slotSize + 1, y + r * slotSize + 1, 16, 16);
            }
        }

        if (itemList == null) return;

        //计算滚动索引（列表缩短时钳制，避免保留的滚动位置越界导致空白网格）
        int totalRows = (int) Math.ceil((double) itemList.size() / cols);
        int invisibleRows = Math.max(0, totalRows - rows);
        int startRow = Math.min((int) (scrollOffset * invisibleRows), invisibleRows);
        int startIndex = startRow * cols;


        guiGraphics.pose().pushPose();
//        guiGraphics.pose().translate(0, 0, 100);
        Minecraft mc = ClientUtils.getMc();
        // 获取鼠标相对坐标用于高亮判断
        double relMouseX = getMouseX();
        double relMouseY = getMouseY();

        for (int i = 0; i < rows * cols; i++) {
            int index = startIndex + i;
            if (index >= itemList.size()) break;

            VirtualItemStack vStack = itemList.get(index);
            // 计算当前槽位在屏幕上的绝对位置
            int col = i % cols;
            int row = i / cols;
            int sx = x + col * slotSize;
            int sy = y + row * slotSize;

            // 绘制物品
            guiGraphics.renderItem(vStack.getDisplayStack(), sx + 1, sy + 1);
            //绘制耐久条
            guiGraphics.renderItemDecorations(mc.font, vStack.getDisplayStack(), sx + 1, sy + 1, null);

            // 绘制数量
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, 200);
            String amountStr = formatAmount(vStack.getAmount());
            int strWidth = mc.font.width(amountStr);
            // 右下角对齐；白字描一圈深色边，压在浅色物品图标和纸色底上也看得清。
            // Bottom-right, white with a dark outline all round, so it reads over pale icons on the paper as well.
            mc.font.drawInBatch8xOutline(Component.literal(amountStr).getVisualOrderText(), sx + 17 - strWidth, sy + 9,
                    0xFFFFFFFF, 0xFF2C2C2A, guiGraphics.pose().last().pose(), guiGraphics.bufferSource(),
                    net.minecraft.client.renderer.LightTexture.FULL_BRIGHT);
            guiGraphics.flush();
            guiGraphics.pose().popPose();

            // 悬停高亮 (判断相对坐标)
            int relSx = col * slotSize;
            int relSy = row * slotSize;
            if (isMouseOver() && relMouseX >= relSx && relMouseX < relSx + slotSize && relMouseY >= relSy && relMouseY < relSy + slotSize) {
                RenderSystem.disableDepthTest();
                guiGraphics.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0x80FFFFFF);
                RenderSystem.enableDepthTest();
            }
        }

        guiGraphics.pose().popPose();

        //渲染滑块
        renderScrollBar(guiGraphics, x, y, h, hint);
    }

    private void renderScrollBar(GuiGraphics guiGraphics, int x, int y, int h, RenderingHint hint) {
        int barX = x + getScrollBarXRelative(); // 屏幕绝对X
        int trackH = h;
        int thumbHeight = getThumbHeight();

        hint.theme(this).drawSliderBackground(guiGraphics, barX, y, scrollBarWidth, trackH, false);
        List<VirtualItemStack> list = itemSource.get();
        int totalItems = list != null ? list.size() : 0;
        int totalRows = (int) Math.ceil((double) totalItems / cols);

        if (totalRows <= rows) return;

        int thumbY = y + (int) (scrollOffset * (trackH - thumbHeight));
        hint.theme(this).drawSliderBar(guiGraphics, barX + 1, thumbY, scrollBarWidth - 2, thumbHeight, isScrolling);
    }

    @Override
    public void getTooltip(TooltipBuilder tooltip) {
        if (!isMouseOver()) return;

        // 计算鼠标指向的槽位
        int col = (int) (getMouseX() / slotSize);
        int row = (int) (getMouseY() / slotSize);

        if (col >= 0 && col < cols && row >= 0 && row < rows) {
            int index = getIndexAt(row, col);
            List<VirtualItemStack> list = itemSource.get();
            if (list != null && index >= 0 && index < list.size()) {
                VirtualItemStack vStack = list.get(index);
                List<Component> itemTooltip = Screen.getTooltipFromItem(Minecraft.getInstance(), vStack.getDisplayStack());
                for (Component c : itemTooltip) {
                    tooltip.accept(c);
                }
                if (vStack.getAmount() >= 1000)
                    tooltip.accept(Component.translatable("gui.frostedheart.warehouse.exact_amount", vStack.getAmount())
                            .withStyle(net.minecraft.ChatFormatting.GRAY));
            }
        }
    }

    @Override
    public boolean onMousePressed(MouseButton button) {
        //鼠标相对坐标
        if (!isMouseOver()) return false;
        double mx = getMouseX();
        double my = getMouseY();

        //滚动条点击检测
        int barX = getScrollBarXRelative();
        if (mx >= barX && mx <= barX + scrollBarWidth && my >= 0 && my <= getHeight()) {
            this.isScrolling = true;
            return true;
        }

        //物品点击检测
        if (mx >= 0 && mx < cols * slotSize && my >= 0 && my < rows * slotSize) {
            int col = (int) (mx / slotSize);
            int row = (int) (my / slotSize);

            int btnId = button.ordinal();

            ItemStack carried = ItemStack.EMPTY;
            if (Minecraft.getInstance().player != null) {
                carried = Minecraft.getInstance().player.containerMenu.getCarried();
            }
            boolean shiftDown = Screen.hasShiftDown();

            if (!carried.isEmpty()) {
                // 存入逻辑(INSERT)
                FHNetwork.INSTANCE.sendToServer(new WarehouseInteractPacket(WarehouseInteractPacket.Action.INSERT, shiftDown, btnId, ItemStack.EMPTY));
                playClickSound();
                return true;
            } else {
                // 取出逻辑(EXTRACT)
                int index = getIndexAt(row, col);
                List<VirtualItemStack> list = itemSource.get();
                if (list != null && index >= 0 && index < list.size()) {
                    VirtualItemStack clickedVStack = list.get(index);
                    FHNetwork.INSTANCE.sendToServer(new WarehouseInteractPacket(WarehouseInteractPacket.Action.EXTRACT, shiftDown, btnId, clickedVStack.getDisplayStack()));
                    playClickSound();
                    return true;
                }
            }
        }

        return super.onMousePressed(button);
    }

    @Override
    public void onMouseReleased(MouseButton button) {
        this.isScrolling = false;
        super.onMouseReleased(button);
    }

    @Override
    public boolean onMouseDragged(MouseButton button, double dragX, double dragY) {
        //鼠标相对坐标
        if (this.isScrolling) {
            double my = getMouseY();

            List<VirtualItemStack> list = itemSource.get();
            int totalItems = list != null ? list.size() : 0;
            int totalRows = (int) Math.ceil((double) totalItems / cols);

            if (totalRows <= rows) return false;

            int trackHeight = getHeight();
            int thumbHeight = getThumbHeight();
            float range = trackHeight - thumbHeight;

            if (range > 0) {
                double relativeY = my - (thumbHeight / 2.0);
                float newOffset = (float) (relativeY / range);
                this.scrollOffset = Mth.clamp(newOffset, 0.0f, 1.0f);
            }
            return true;
        }
        return super.onMouseDragged(button, dragX, dragY);
    }

    @Override
    public boolean onMouseScrolled(double scroll) {
        if (isMouseOver()) {
            List<VirtualItemStack> list = itemSource.get();
            int totalRows = (int) Math.ceil((double) (list != null ? list.size() : 0) / cols);
            if (totalRows <= rows) return false;

            float step = 1.0f / (totalRows - rows);
            this.scrollOffset = Mth.clamp(this.scrollOffset - (float) scroll * step, 0.0f, 1.0f);
            return true;
        }
        return false;
    }

    private int getIndexAt(int row, int col) {
        List<VirtualItemStack> list = itemSource.get();
        if (list == null) return -1;
        int totalRows = (int) Math.ceil((double) list.size() / cols);
        int invisibleRows = Math.max(0, totalRows - rows);
        int startRow = Math.min((int) (scrollOffset * invisibleRows), invisibleRows);
        return (startRow + row) * cols + col;
    }

    private int getScrollBarXRelative() {
        return (cols * slotSize) + 2;
    }

    private int getThumbHeight() {
        List<VirtualItemStack> list = itemSource.get();
        int totalItems = list != null ? list.size() : 0;
        if (totalItems == 0) return getHeight();

        int totalRows = (int) Math.ceil((double) totalItems / cols);
        if (totalRows <= rows) return getHeight();

        int trackH = getHeight();
        int thumbHeight = (int) ((float) rows / totalRows * trackH);
        return Math.max(thumbHeight, 10);
    }

    //数量字符(k,M)，不出现小数；准确数量在悬停提示里
    private String formatAmount(long amount) {
        return com.teammoeg.frostedheart.content.town.ui.TownFormat.compact(amount);
    }

    private void playClickSound() {
         Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

}