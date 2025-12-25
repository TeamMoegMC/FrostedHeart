package com.teammoeg.frostedheart.content.town.warehouse;

import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.content.town.AbstractTownWorkerBlockScreen;
import com.teammoeg.frostedheart.content.town.network.WarehouseC2SRequestPacket;
import com.teammoeg.frostedheart.content.town.network.WarehouseInteractPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.List;

import static com.teammoeg.chorda.client.CInputHelper.playClickSound;
import static net.minecraft.client.gui.screens.Screen.hasShiftDown;

public class VirtualItemGridWidget extends AbstractWidget implements AbstractTownWorkerBlockScreen.TabContentComponent {

    private final List<VirtualItemStack> itemList; // 数据源
    private float scrollOffset = 0.0f;
    private boolean isScrolling = false;


    private final int cols = 9;
    private final int rows = 5; // 假设显示5行
    private final int slotSize = 18;


    private final int scrollBarX;
    private final int scrollBarWidth = 12;
    private final int scrollBarHeight;

    public VirtualItemGridWidget(int x, int y, List<VirtualItemStack> itemList) {
        super(x, y, 9 * 18 + 14, 5 * 18, Component.empty());
        this.itemList = itemList;
        this.scrollBarHeight = height;
        this.scrollBarX = x + (cols * slotSize) + 2;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int sx = getX() + c * slotSize;
                int sy = getY() + r * slotSize;
                guiGraphics.fill(sx, sy, sx + slotSize - 1, sy + slotSize - 1, 0xFF8B8B8B);

                guiGraphics.fill(sx, sy, sx + slotSize -1, sy + 1, 0xFF373737); // Top
                guiGraphics.fill(sx, sy, sx + 1, sy + slotSize -1, 0xFF373737); // Left
            }
        }

        // 计算滚动
        int totalRows = (int) Math.ceil((double) itemList.size() / cols);
        int invisibleRows = Math.max(0, totalRows - rows);
        int startIndex = (int) (scrollOffset * invisibleRows) * cols;

        // 渲染物品
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 100); // 提升物品层级

        Minecraft mc = Minecraft.getInstance();
        VirtualItemStack tooltipStack = null;

        for (int i = 0; i < rows * cols; i++) {
            int index = startIndex + i;
            if (index >= itemList.size()) break;

            VirtualItemStack vStack = itemList.get(index);
            int sx = getX() + (i % cols) * slotSize;
            int sy = getY() + (i / cols) * slotSize;

            // 渲染物品
            guiGraphics.renderItem(vStack.getStack(), sx + 1, sy + 1);

            // 渲染数量
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, 200);
            String amountStr = formatAmount(vStack.getAmount());
            int strWidth = mc.font.width(amountStr);
            // 右下角对齐
            guiGraphics.drawString(mc.font, amountStr, sx + 17 - strWidth, sy + 9, 0xFFFFFF, true);
            guiGraphics.pose().popPose();

            // 悬停Tooltip
            if (isHovered && mouseX >= sx && mouseX < sx + slotSize && mouseY >= sy && mouseY < sy + slotSize) {
                guiGraphics.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0x80FFFFFF); // 高亮
                tooltipStack = vStack;
            }
        }

        guiGraphics.pose().popPose();

        // 4. 渲染滚动条
        renderScrollBar(guiGraphics);

        // 5. 渲染 Tooltip (最后画)
        if (tooltipStack != null) {
            guiGraphics.renderTooltip(mc.font, tooltipStack.getStack(), mouseX, mouseY);
        }
    }

    private void renderScrollBar(GuiGraphics guiGraphics) {
        // 简单的滚动条背景
        guiGraphics.fill(scrollBarX, getY(), scrollBarX + scrollBarWidth, getY() + scrollBarHeight, 0xFF000000);

        int barHeight = (itemList.size() <= rows * cols) ? scrollBarHeight : (int)((float)scrollBarHeight / (itemList.size() / cols) * rows);
        if (barHeight < 10) barHeight = 10;
        if (barHeight > scrollBarHeight) barHeight = scrollBarHeight;

        int barTop = getY() + (int)(scrollOffset * (scrollBarHeight - barHeight));

        // 滚动滑块
        guiGraphics.fill(scrollBarX + 1, barTop, scrollBarX + scrollBarWidth - 1, barTop + barHeight, 0xFF808080);
    }

    // 处理数值 (k, M)
    private String formatAmount(long amount) {
        if (amount < 1000) return String.valueOf(amount);
        if (amount < 1000000) return String.format("%.1fk", amount / 1000.0);
        return String.format("%.1fM", amount / 1000000.0);
    }

    //交互逻辑
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.visible) return false;

        // 检查是否点击滚动条
        if (mouseX >= scrollBarX && mouseX <= scrollBarX + scrollBarWidth && mouseY >= getY() && mouseY <= getY() + scrollBarHeight) {
            this.isScrolling = true;
            return true;
        }

        // 检查是否点击物品
        double relX = mouseX - getX();
        double relY = mouseY - getY();
        if (relX >= 0 && relY >= 0 && relX < cols * slotSize && relY < rows * slotSize) {
            int col = (int)((mouseX - getX()) / slotSize);
            int row = (int)((mouseY - getY()) / slotSize);
                ItemStack carried = Minecraft.getInstance().player.containerMenu.getCarried();
                // 存入 (Insert)
                if (!carried.isEmpty()) {
                    FHNetwork.INSTANCE.sendToServer(new WarehouseInteractPacket(WarehouseInteractPacket.Action.INSERT,hasShiftDown(),button,ItemStack.EMPTY));
                    playClickSound();
                    return true;
                }
                else {
                    // 取出 (EXTRACT)
                    int index = getIndexAt(row, col);
                    if (index >= 0 && index < itemList.size()) {
                        VirtualItemStack clickedVStack = this.itemList.get(index);
                        FHNetwork.INSTANCE.sendToServer(new WarehouseInteractPacket(WarehouseInteractPacket.Action.EXTRACT,hasShiftDown(),button,clickedVStack.getStack()));
                    }
                    playClickSound();
                    return true;
                }
            }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.isScrolling = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.isScrolling) {
            int totalRows = (int) Math.ceil((double) itemList.size() / cols);
            if (totalRows <= rows) return false;

            float range = scrollBarHeight - 10; // 假设最小滑块高度10
            float delta = (float) (dragY / range);
            this.scrollOffset = Mth.clamp(this.scrollOffset + delta, 0.0f, 1.0f);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (isHovered) {
            int totalRows = (int) Math.ceil((double) itemList.size() / cols);
            if (totalRows <= rows) return false;

            float step = 1.0f / (totalRows - rows);
            this.scrollOffset = Mth.clamp(this.scrollOffset - (float)delta * step, 0.0f, 1.0f);
            return true;
        }
        return false;
    }

    private int getIndexAt(int row, int col) {
        int totalRows = (int) Math.ceil((double) itemList.size() / cols);
        int invisibleRows = Math.max(0, totalRows - rows);
        int startRow = (int) (scrollOffset * invisibleRows);
        return (startRow + row) * cols + col;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) { }
}