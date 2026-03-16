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
 * 可拖拽、可滚轮控制的垂直滚动条组件。适用于Screen，支持自定义轨道和滑块纹理。
 * 提供鼠标拖拽、轨道点击跳转、鼠标滚轮滚动等交互方式，并支持行数变化回调。
 * <p>
 * A draggable, scroll-wheel-controllable vertical scrollbar widget. Designed for use
 * in Screen, supports custom track and thumb textures. Provides mouse dragging, track
 * click-to-jump, and mouse wheel scrolling interactions, with a row change callback.
 */
public class ScrollBarWidget extends AbstractWidget {
    /** 当前行索引 / Current row index */
    @Getter
    private int currentRow;

    /** 最大行数 / Maximum number of rows */
    @Getter
    private int maxRows;

    /** 滑块宽度 / Thumb width */
    private int thumbWidth;
    /** 滑块高度 / Thumb height */
    private int thumbHeight;

    /** 轨道自定义纹理 / Custom track texture */
    @Nullable
    private ResourceLocation trackTexture;
    /** 滑块自定义纹理 / Custom thumb texture */
    @Nullable
    private ResourceLocation thumbTexture;

    /** 轨道纹理水平偏移量 / Track texture horizontal offset */
    private int trackUOffset = 0;
    /** 轨道纹理垂直偏移量 / Track texture vertical offset */
    private int trackVOffset = 0;
    /** 滑块纹理水平偏移量 / Thumb texture horizontal offset */
    private int thumbUOffset = 0;
    /** 滑块纹理垂直偏移量 / Thumb texture vertical offset */
    private int thumbVOffset = 0;

    /** 轨道纹理文件总宽度 / Track texture file total width */
    private int trackTextureWidth = 256;
    /** 轨道纹理文件总高度 / Track texture file total height */
    private int trackTextureHeight = 256;
    /** 滑块纹理文件总宽度 / Thumb texture file total width */
    private int thumbTextureWidth = 256;
    /** 滑块纹理文件总高度 / Thumb texture file total height */
    private int thumbTextureHeight = 256;

    /** 是否正在拖拽滑块 / Whether the thumb is being dragged */
    private boolean dragging;

    /** 行数变化回调 / Row change callback */
    @Nullable
    private BiConsumer<Integer, Integer> onRowChanged;

    /**
     * 创建滚动条。滑块高度根据最大行数自动计算。
     * <p>
     * Creates a scrollbar. The thumb height is automatically calculated based on the max rows.
     *
     * @param x 滚动条X坐标 / Scrollbar X coordinate
     * @param y 滚动条Y坐标 / Scrollbar Y coordinate
     * @param width 滚动条宽度 / Scrollbar width
     * @param height 滚动条高度（长度） / Scrollbar height (length)
     * @param maxRows 最大行数 / Maximum number of rows
     */
    public ScrollBarWidget(int x, int y, int width, int height, int maxRows) {
        super(x, y, width, height, Component.empty());
        this.currentRow = 0;
        this.maxRows = Math.max(1, maxRows);
        this.thumbWidth = width;
        this.thumbHeight = Math.max(10, height / Math.max(1, maxRows)); // 滑块高度根据行数自动计算
        this.dragging = false;
    }

    /**
     * 设置滑块尺寸。
     * <p>
     * Sets the thumb dimensions.
     *
     * @param width 滑块宽度 / Thumb width
     * @param height 滑块高度 / Thumb height
     * @return 当前实例，用于链式调用 / This instance for method chaining
     */
    public ScrollBarWidget setThumbSize(int width, int height) {
        this.thumbWidth = width;
        this.thumbHeight = height;
        return this;
    }

    /**
     * 设置轨道纹理。
     * <p>
     * Sets the track texture.
     *
     * @param texture 纹理资源位置，null则使用默认样式 / Texture resource location, null for default style
     * @return 当前实例，用于链式调用 / This instance for method chaining
     */
    public ScrollBarWidget setTrackTexture(@Nullable ResourceLocation texture) {
        this.trackTexture = texture;
        return this;
    }
    
    /**
     * 设置轨道纹理及其在纹理文件中的位置。
     * <p>
     * Sets the track texture and its position within the texture file.
     *
     * @param texture 纹理资源位置 / Texture resource location
     * @param uOffset 水平偏移量（像素） / Horizontal offset in pixels
     * @param vOffset 垂直偏移量（像素） / Vertical offset in pixels
     * @return 当前实例，用于链式调用 / This instance for method chaining
     */
    public ScrollBarWidget setTrackTexture(@Nullable ResourceLocation texture, int uOffset, int vOffset) {
        this.trackTexture = texture;
        this.trackUOffset = uOffset;
        this.trackVOffset = vOffset;
        return this;
    }
    
    /**
     * 设置轨道纹理及其在纹理文件中的位置和纹理文件总尺寸。
     * <p>
     * Sets the track texture, its position within the texture file, and the texture file dimensions.
     *
     * @param texture 纹理资源位置 / Texture resource location
     * @param uOffset 水平偏移量（像素） / Horizontal offset in pixels
     * @param vOffset 垂直偏移量（像素） / Vertical offset in pixels
     * @param textureWidth 纹理文件总宽度 / Total texture file width
     * @param textureHeight 纹理文件总高度 / Total texture file height
     * @return 当前实例，用于链式调用 / This instance for method chaining
     */
    public ScrollBarWidget setTrackTexture(@Nullable ResourceLocation texture, int uOffset, int vOffset, int textureWidth, int textureHeight) {
        this.trackTexture = texture;
        this.trackUOffset = uOffset;
        this.trackVOffset = vOffset;
        this.trackTextureWidth = textureWidth;
        this.trackTextureHeight = textureHeight;
        return this;
    }

    /**
     * 设置滑块纹理。
     * <p>
     * Sets the thumb texture.
     *
     * @param texture 纹理资源位置，null则使用默认样式 / Texture resource location, null for default style
     * @return 当前实例，用于链式调用 / This instance for method chaining
     */
    public ScrollBarWidget setThumbTexture(@Nullable ResourceLocation texture) {
        this.thumbTexture = texture;
        return this;
    }
    
    /**
     * 设置滑块纹理及其在纹理文件中的位置。
     * <p>
     * Sets the thumb texture and its position within the texture file.
     *
     * @param texture 纹理资源位置 / Texture resource location
     * @param uOffset 水平偏移量（像素） / Horizontal offset in pixels
     * @param vOffset 垂直偏移量（像素） / Vertical offset in pixels
     * @return 当前实例，用于链式调用 / This instance for method chaining
     */
    public ScrollBarWidget setThumbTexture(@Nullable ResourceLocation texture, int uOffset, int vOffset) {
        this.thumbTexture = texture;
        this.thumbUOffset = uOffset;
        this.thumbVOffset = vOffset;
        return this;
    }
    
    /**
     * 设置滑块纹理及其在纹理文件中的位置和纹理文件总尺寸。
     * <p>
     * Sets the thumb texture, its position within the texture file, and the texture file dimensions.
     *
     * @param texture 纹理资源位置 / Texture resource location
     * @param uOffset 水平偏移量（像素） / Horizontal offset in pixels
     * @param vOffset 垂直偏移量（像素） / Vertical offset in pixels
     * @param textureWidth 纹理文件总宽度 / Total texture file width
     * @param textureHeight 纹理文件总高度 / Total texture file height
     * @return 当前实例，用于链式调用 / This instance for method chaining
     */
    public ScrollBarWidget setThumbTexture(@Nullable ResourceLocation texture, int uOffset, int vOffset, int textureWidth, int textureHeight) {
        this.thumbTexture = texture;
        this.thumbUOffset = uOffset;
        this.thumbVOffset = vOffset;
        this.thumbTextureWidth = textureWidth;
        this.thumbTextureHeight = textureHeight;
        return this;
    }

    /**
     * 设置行数变化回调。
     * <p>
     * Sets the row change callback.
     *
     * @param callback 回调函数，参数为(旧行数, 新行数) / Callback function with parameters (oldRow, newRow)
     * @return 当前实例，用于链式调用 / This instance for method chaining
     */
    public ScrollBarWidget setOnRowChanged(@Nullable BiConsumer<Integer, Integer> callback) {
        this.onRowChanged = callback;
        return this;
    }

    /**
     * 设置当前行数。值会被限制在有效范围内，变化时触发回调。
     * <p>
     * Sets the current row. The value is clamped to the valid range,
     * and the callback is triggered on change.
     *
     * @param row 目标行数 / Target row number
     */
    public void setCurrentRow(int row) {
        int oldRow = this.currentRow;
        this.currentRow = clampRow(row);
        if (oldRow != this.currentRow && onRowChanged != null) {
            onRowChanged.accept(oldRow, this.currentRow);
        }
    }

    /**
     * 设置最大行数。同时重新计算滑块高度并校正当前行数。
     * <p>
     * Sets the maximum number of rows. Also recalculates the thumb height
     * and clamps the current row.
     *
     * @param maxRows 最大行数，最小为1 / Maximum number of rows, minimum 1
     */
    public void setMaxRows(int maxRows) {
        this.maxRows = Math.max(1, maxRows);
        this.currentRow = clampRow(this.currentRow);
        // 重新计算滑块高度
        this.thumbHeight = Math.max(10, this.height / Math.max(1, this.maxRows));
    }

    /**
     * 将行数限制在有效范围[0, maxRows-1]内。
     * <p>
     * Clamps the row number to the valid range [0, maxRows-1].
     *
     * @param row 待限制的行数 / Row number to clamp
     * @return 限制后的行数 / Clamped row number
     */
    private int clampRow(int row) {
        return Math.max(0, Math.min(row, maxRows - 1));
    }

    /**
     * 根据当前行数计算滑块的Y坐标。
     * <p>
     * Calculates the thumb's Y coordinate based on the current row.
     *
     * @return 滑块的Y坐标 / The thumb's Y coordinate
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
     * 根据鼠标Y坐标计算对应的行数。
     * <p>
     * Calculates the corresponding row number from the mouse Y coordinate.
     *
     * @param mouseY 鼠标Y坐标 / Mouse Y coordinate
     * @return 计算得到的行数 / Calculated row number
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

    /**
     * 渲染滚动条控件，包括轨道和滑块。
     * <p>
     * Renders the scrollbar widget, including the track and thumb.
     *
     * @param guiGraphics 图形上下文 / Graphics context
     * @param mouseX 鼠标X坐标 / Mouse X coordinate
     * @param mouseY 鼠标Y坐标 / Mouse Y coordinate
     * @param partialTick 渲染插值时间 / Partial tick time for rendering interpolation
     */
    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 渲染轨道（条）
        renderTrack(guiGraphics);
        // 渲染滑块
        renderThumb(guiGraphics, mouseX, mouseY);
    }

    /**
     * 渲染轨道。使用自定义纹理或默认的灰色背景样式。
     * <p>
     * Renders the track. Uses a custom texture or a default gray background style.
     *
     * @param guiGraphics 图形上下文 / Graphics context
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
     * 渲染滑块。使用自定义纹理或根据悬停/拖拽状态改变颜色的默认样式。
     * <p>
     * Renders the thumb. Uses a custom texture or a default style that changes
     * color based on hover/drag state.
     *
     * @param guiGraphics 图形上下文 / Graphics context
     * @param mouseX 鼠标X坐标 / Mouse X coordinate
     * @param mouseY 鼠标Y坐标 / Mouse Y coordinate
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
     * 检查鼠标是否悬停在滑块上。
     * <p>
     * Checks whether the mouse is hovering over the thumb.
     *
     * @param mouseX 鼠标X坐标 / Mouse X coordinate
     * @param mouseY 鼠标Y坐标 / Mouse Y coordinate
     * @return 鼠标是否在滑块区域内 / Whether the mouse is within the thumb area
     */
    private boolean isThumbHovered(int mouseX, int mouseY) {
        int thumbY = getThumbY();
        int thumbX = getX() + (width - thumbWidth) / 2;
        return mouseX >= thumbX && mouseX < thumbX + thumbWidth
                && mouseY >= thumbY && mouseY < thumbY + thumbHeight;
    }

    /**
     * 处理鼠标滚轮事件。向上滚动减少行数，向下滚动增加行数。
     * <p>
     * Handles mouse scroll events. Scrolling up decreases the row, scrolling down increases it.
     *
     * @param mouseX 鼠标X坐标 / Mouse X coordinate
     * @param mouseY 鼠标Y坐标 / Mouse Y coordinate
     * @param delta 滚轮滚动量 / Scroll wheel delta
     * @return 始终返回true表示事件已处理 / Always returns true indicating the event was handled
     */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // 鼠标滚轮控制：向上滚动减少行数，向下滚动增加行数
        setCurrentRow(currentRow - (int) Math.signum(delta));
        return true;
    }

    /**
     * 处理鼠标点击事件。点击滑块开始拖拽，点击轨道直接跳转到对应行。
     * <p>
     * Handles mouse click events. Clicking the thumb starts dragging,
     * clicking the track jumps directly to the corresponding row.
     *
     * @param mouseX 鼠标X坐标 / Mouse X coordinate
     * @param mouseY 鼠标Y坐标 / Mouse Y coordinate
     */
    @Override
    public void onClick(double mouseX, double mouseY) {
        if (isThumbHovered((int) mouseX, (int) mouseY)) {
            this.dragging = true;
        } else {
            // 点击轨道直接跳转
            setCurrentRow(getRowFromMouseY(mouseY));
        }
    }

    /**
     * 处理鼠标拖拽事件。拖拽时根据鼠标Y坐标更新当前行。
     * <p>
     * Handles mouse drag events. Updates the current row based on the mouse Y coordinate
     * during dragging.
     *
     * @param mouseX 鼠标X坐标 / Mouse X coordinate
     * @param mouseY 鼠标Y坐标 / Mouse Y coordinate
     * @param dragX X方向拖拽增量 / X-axis drag delta
     * @param dragY Y方向拖拽增量 / Y-axis drag delta
     */
    @Override
    protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
        if (dragging) {
            setCurrentRow(getRowFromMouseY(mouseY));
        }
    }

    /**
     * 处理鼠标释放事件。结束拖拽状态。
     * <p>
     * Handles mouse release events. Ends the dragging state.
     *
     * @param mouseX 鼠标X坐标 / Mouse X coordinate
     * @param mouseY 鼠标Y坐标 / Mouse Y coordinate
     */
    @Override
    public void onRelease(double mouseX, double mouseY) {
        this.dragging = false;
    }

    /**
     * 更新控件的无障碍叙述信息。
     * <p>
     * Updates the widget's accessibility narration information.
     *
     * @param narrationElementOutput 叙述输出 / Narration element output
     */
    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput);
    }
}
