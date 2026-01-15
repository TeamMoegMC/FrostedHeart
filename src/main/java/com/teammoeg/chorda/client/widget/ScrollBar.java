package com.teammoeg.chorda.client.widget;

import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.function.BiConsumer;

import javax.annotation.Nullable;

/**
 * 可拖拽、可滚轮控制的垂直滚动条组件
 * 适用于Screen
 */
public class ScrollBar extends AbstractWidget {
    // 当前行数和最大行数
    @Getter
    private int currentRow;

    @Getter
    private int maxRows;

    // 滑块尺寸
    private int thumbWidth;
    private int thumbHeight;

    // 自定义材质
    @Nullable
    private ResourceLocation trackTexture;
    @Nullable
    private ResourceLocation thumbTexture;
    
    // 材质偏移量（用于选择材质文件中的特定区域）
    private int trackUOffset = 0;
    private int trackVOffset = 0;
    private int thumbUOffset = 0;
    private int thumbVOffset = 0;
    
    // 材质文件的总尺寸
    private int trackTextureWidth = 256;
    private int trackTextureHeight = 256;
    private int thumbTextureWidth = 256;
    private int thumbTextureHeight = 256;

    // 是否正在拖拽
    private boolean dragging;

    // 行数变化回调
    @Nullable
    private BiConsumer<Integer, Integer> onRowChanged;

    /**
     * 创建滚动条
     * @param x 条的X坐标
     * @param y 条的Y坐标
     * @param width 条的宽度
     * @param height 条的高度（长度）
     * @param maxRows 最大行数
     */
    public ScrollBar(int x, int y, int width, int height, int maxRows) {
        super(x, y, width, height, Component.empty());
        this.currentRow = 0;
        this.maxRows = Math.max(1, maxRows);
        this.thumbWidth = width;
        this.thumbHeight = Math.max(10, height / Math.max(1, maxRows)); // 滑块高度根据行数自动计算
        this.dragging = false;
    }

    /**
     * 设置滑块尺寸
     */
    public ScrollBar setThumbSize(int width, int height) {
        this.thumbWidth = width;
        this.thumbHeight = height;
        return this;
    }

    /**
     * 设置轨道材质
     */
    public ScrollBar setTrackTexture(@Nullable ResourceLocation texture) {
        this.trackTexture = texture;
        return this;
    }
    
    /**
     * 设置轨道材质及其在材质文件中的位置
     * @param texture 材质资源位置
     * @param uOffset 水平偏移量（像素）
     * @param vOffset 垂直偏移量（像素）
     */
    public ScrollBar setTrackTexture(@Nullable ResourceLocation texture, int uOffset, int vOffset) {
        this.trackTexture = texture;
        this.trackUOffset = uOffset;
        this.trackVOffset = vOffset;
        return this;
    }
    
    /**
     * 设置轨道材质及其在材质文件中的位置和材质文件总尺寸
     * @param texture 材质资源位置
     * @param uOffset 水平偏移量（像素）
     * @param vOffset 垂直偏移量（像素）
     * @param textureWidth 材质文件总宽度
     * @param textureHeight 材质文件总高度
     */
    public ScrollBar setTrackTexture(@Nullable ResourceLocation texture, int uOffset, int vOffset, int textureWidth, int textureHeight) {
        this.trackTexture = texture;
        this.trackUOffset = uOffset;
        this.trackVOffset = vOffset;
        this.trackTextureWidth = textureWidth;
        this.trackTextureHeight = textureHeight;
        return this;
    }

    /**
     * 设置滑块材质
     */
    public ScrollBar setThumbTexture(@Nullable ResourceLocation texture) {
        this.thumbTexture = texture;
        return this;
    }
    
    /**
     * 设置滑块材质及其在材质文件中的位置
     * @param texture 材质资源位置
     * @param uOffset 水平偏移量（像素）
     * @param vOffset 垂直偏移量（像素）
     */
    public ScrollBar setThumbTexture(@Nullable ResourceLocation texture, int uOffset, int vOffset) {
        this.thumbTexture = texture;
        this.thumbUOffset = uOffset;
        this.thumbVOffset = vOffset;
        return this;
    }
    
    /**
     * 设置滑块材质及其在材质文件中的位置和材质文件总尺寸
     * @param texture 材质资源位置
     * @param uOffset 水平偏移量（像素）
     * @param vOffset 垂直偏移量（像素）
     * @param textureWidth 材质文件总宽度
     * @param textureHeight 材质文件总高度
     */
    public ScrollBar setThumbTexture(@Nullable ResourceLocation texture, int uOffset, int vOffset, int textureWidth, int textureHeight) {
        this.thumbTexture = texture;
        this.thumbUOffset = uOffset;
        this.thumbVOffset = vOffset;
        this.thumbTextureWidth = textureWidth;
        this.thumbTextureHeight = textureHeight;
        return this;
    }

    /**
     * 设置行数变化回调
     * @param callback 回调函数，参数为 (旧行数, 新行数)
     */
    public ScrollBar setOnRowChanged(@Nullable BiConsumer<Integer, Integer> callback) {
        this.onRowChanged = callback;
        return this;
    }

    /**
     * 设置当前行数
     */
    public void setCurrentRow(int row) {
        int oldRow = this.currentRow;
        this.currentRow = clampRow(row);
        if (oldRow != this.currentRow && onRowChanged != null) {
            onRowChanged.accept(oldRow, this.currentRow);
        }
    }

    /**
     * 设置最大行数
     */
    public void setMaxRows(int maxRows) {
        this.maxRows = Math.max(1, maxRows);
        this.currentRow = clampRow(this.currentRow);
        // 重新计算滑块高度
        this.thumbHeight = Math.max(10, this.height / Math.max(1, this.maxRows));
    }

    private int clampRow(int row) {
        return Math.max(0, Math.min(row, maxRows - 1));
    }

    /**
     * 计算滑块的Y坐标
     */
    private int getThumbY() {
        if (maxRows <= 1) {
            return getY();
        }
        int trackHeight = this.height - this.thumbHeight;
        float progress = (float) currentRow / (maxRows - 1);
        return getY() + (int) (trackHeight * progress);
    }

    /**
     * 根据鼠标Y坐标计算对应的行数
     */
    private int getRowFromMouseY(double mouseY) {
        if (maxRows <= 1) {
            return 0;
        }
        int trackHeight = this.height - this.thumbHeight;
        double relativeY = mouseY - getY() - (thumbHeight / 2.0);
        float progress = (float) (relativeY / trackHeight);
        progress = Math.max(0, Math.min(1, progress));
        return Math.round(progress * (maxRows - 1));
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 渲染轨道（条）
        renderTrack(guiGraphics);
        // 渲染滑块
        renderThumb(guiGraphics, mouseX, mouseY);
    }

    /**
     * 渲染轨道
     */
    protected void renderTrack(GuiGraphics guiGraphics) {
        if (trackTexture != null) {
            guiGraphics.blit(trackTexture, getX(), getY(), trackUOffset, trackVOffset, width, height, trackTextureWidth, trackTextureHeight);
        } else {
            // 默认样式：灰色背景
            guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, 0xFF3C3C3C);
            // 内边框
            guiGraphics.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, 0xFF1E1E1E);
        }
    }

    /**
     * 渲染滑块
     */
    protected void renderThumb(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int thumbY = getThumbY();
        int thumbX = getX() + (width - thumbWidth) / 2;

        boolean hovered = isThumbHovered(mouseX, mouseY);

        if (thumbTexture != null) {
            guiGraphics.blit(thumbTexture, thumbX, thumbY, thumbUOffset, thumbVOffset, thumbWidth, thumbHeight, thumbTextureWidth, thumbTextureHeight);
        } else {
            // 默认样式：根据悬停状态改变颜色
            int color = dragging ? 0xFFFFFFFF : (hovered ? 0xFFCCCCCC : 0xFF8B8B8B);
            guiGraphics.fill(thumbX, thumbY, thumbX + thumbWidth, thumbY + thumbHeight, color);
        }
    }

    /**
     * 检查鼠标是否在滑块上
     */
    private boolean isThumbHovered(int mouseX, int mouseY) {
        int thumbY = getThumbY();
        int thumbX = getX() + (width - thumbWidth) / 2;
        return mouseX >= thumbX && mouseX < thumbX + thumbWidth
                && mouseY >= thumbY && mouseY < thumbY + thumbHeight;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // 鼠标滚轮控制：向上滚动减少行数，向下滚动增加行数
        setCurrentRow(currentRow - (int) Math.signum(delta));
        return true;
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (isThumbHovered((int) mouseX, (int) mouseY)) {
            this.dragging = true;
        } else {
            // 点击轨道直接跳转
            setCurrentRow(getRowFromMouseY(mouseY));
        }
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
        if (dragging) {
            setCurrentRow(getRowFromMouseY(mouseY));
        }
    }

    @Override
    public void onRelease(double mouseX, double mouseY) {
        this.dragging = false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput);
    }
}
