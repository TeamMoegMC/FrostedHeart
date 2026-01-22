package com.teammoeg.frostedheart.content.town.warehouse;

import com.mojang.blaze3d.systems.RenderSystem;
import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.cui.MouseButton;
import com.teammoeg.chorda.client.cui.TooltipBuilder;
import com.teammoeg.chorda.client.cui.UIElement;
import com.teammoeg.chorda.client.cui.UILayer;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.content.town.AbstractTownWorkerBlockScreen;
import com.teammoeg.frostedheart.content.town.network.WarehouseInteractPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Supplier;

import static com.teammoeg.chorda.client.CInputHelper.playClickSound;
import static net.minecraft.client.gui.screens.Screen.hasShiftDown;

public class VirtualItemGridElement extends UIElement{
    private final Supplier<List<VirtualItemStack>> itemSource; // 数据源
    private float scrollOffset = 0.0f;
    private boolean isScrolling = false;

    private final int cols = 9;
    private final int rows = 5;
    private final int slotSize = 18;

    private final int scrollBarWidth = 12;

    public VirtualItemGridElement(UIElement parent, int x, int y, Supplier<List<VirtualItemStack>> itemSource) {
        super(parent);
        this.itemSource = itemSource;
        this.setPos(x, y);
        this.setSize(cols * slotSize + 2 + scrollBarWidth, rows * slotSize);
    }


    @Override
    public void render(GuiGraphics guiGraphics, int x, int y, int w, int h) {
        //参数x, y 是屏幕绝对坐标
        //槽位背景绘制
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int sx = x + c * slotSize;
                int sy = y + r * slotSize;
                guiGraphics.fill(sx, sy, sx + slotSize - 1, sy + slotSize - 1, 0xFF8B8B8B);
                guiGraphics.fill(sx, sy, sx + slotSize - 1, sy + 1, 0xFF373737); // Top
                guiGraphics.fill(sx, sy, sx + 1, sy + slotSize - 1, 0xFF373737); // Left
            }
        }

        List<VirtualItemStack> itemList = itemSource.get();
        if (itemList == null) return;

        //计算滚动索引
        int totalRows = (int) Math.ceil((double) itemList.size() / cols);
        int invisibleRows = Math.max(0, totalRows - rows);
        int startIndex = (int) (scrollOffset * invisibleRows) * cols;


        guiGraphics.pose().pushPose();
//        guiGraphics.pose().translate(0, 0, 100);
        Minecraft mc = ClientUtils.getMc();
        // 获取鼠标绝对坐标用于高亮判断
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
            guiGraphics.renderItem(vStack.getStack(), sx + 1, sy + 1);
            //绘制耐久条
            guiGraphics.renderItemDecorations(mc.font, vStack.getStack(), sx + 1, sy + 1, null);

            // 绘制数量
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, 200);
            String amountStr = formatAmount(vStack.getAmount());
            int strWidth = mc.font.width(amountStr);
            // 右下角对齐
            guiGraphics.drawString(mc.font, amountStr, sx + 17 - strWidth, sy + 9, 0xFFFFFF, true);
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
        renderScrollBar(guiGraphics, x, y, h);
    }

    private void renderScrollBar(GuiGraphics guiGraphics, int x, int y, int h) {
        int barX = x + getScrollBarXRelative(); // 屏幕绝对X
        int trackH = h;
        int thumbHeight = getThumbHeight();

        guiGraphics.fill(barX, y, barX + scrollBarWidth, y + trackH, 0xFF000000);
        List<VirtualItemStack> list = itemSource.get();
        int totalItems = list != null ? list.size() : 0;
        int totalRows = (int) Math.ceil((double) totalItems / cols);

        if (totalRows <= rows) {
            guiGraphics.fill(barX + 1, y, barX + scrollBarWidth - 1, y + trackH, 0xFF555555);
            return;
        }

        int thumbY = y + (int) (scrollOffset * (trackH - thumbHeight));
        int color = isScrolling ? 0xFFAAAAAA : 0xFF808080;

        guiGraphics.fill(barX + 1, thumbY, barX + scrollBarWidth - 1, thumbY + thumbHeight, color);
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
                List<Component> itemTooltip = Screen.getTooltipFromItem(Minecraft.getInstance(), vStack.getStack());
                for (Component c : itemTooltip) {
                    tooltip.accept(c);
                }
            }
        }
    }

    @Override
    public boolean onMousePressed(MouseButton button) {
        //鼠标绝对坐标
        if (!isMouseOver()) return false;
        double rawMx = getMouseX();
        double rawMy = getMouseY();

        var window = net.minecraft.client.Minecraft.getInstance().getWindow();
        int guiLeft = (window.getGuiScaledWidth() - 176) / 2;
        int guiTop = (window.getGuiScaledHeight() - 222) / 2;
        //计算相对坐标
        double mx = rawMx - guiLeft;
        double my = rawMy - guiTop;

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
                    FHNetwork.INSTANCE.sendToServer(new WarehouseInteractPacket(WarehouseInteractPacket.Action.EXTRACT, shiftDown, btnId, clickedVStack.getStack()));
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
        //鼠标绝对坐标
        if (this.isScrolling) {
            double rawMy = getMouseY();
            var window = net.minecraft.client.Minecraft.getInstance().getWindow();
            int guiTop = (window.getGuiScaledHeight() - 222) / 2;
            double my = rawMy - guiTop;

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
        int startRow = (int) (scrollOffset * invisibleRows);
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

    //数量字符(k,M)
    private String formatAmount(long amount) {
        if (amount < 1000) return String.valueOf(amount);
        if (amount < 1000000) return String.format("%.1fk", amount / 1000.0);
        return String.format("%.1fM", amount / 1000000.0);
    }

    private void playClickSound() {
         Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

}