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

package com.teammoeg.chorda.client.ui;

import com.teammoeg.chorda.math.Point;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * 带纹理资源的UV类。将纹理资源位置与UV坐标绑定，简化绘制调用。
 * <p>
 * Textured UV class. Binds a texture resource location with UV coordinates to simplify blit calls.
 */
public class TexturedUV extends UV {
    /** 纹理资源位置 / Texture resource location */
    ResourceLocation texture;

    /**
     * 构造一个带纹理的UV，使用默认纹理尺寸（256x256）。
     * <p>
     * Constructs a textured UV with default texture dimensions (256x256).
     *
     * @param texture 纹理资源位置 / Texture resource location
     * @param x 纹理源X坐标 / Source X coordinate in texture
     * @param y 纹理源Y坐标 / Source Y coordinate in texture
     * @param w 宽度 / Width
     * @param h 高度 / Height
     */
    public TexturedUV(ResourceLocation texture, int x, int y, int w, int h) {
        super(x, y, w, h);
        this.texture = texture;
    }
    /**
     * 构造一个带纹理的UV，使用自定义纹理尺寸。
     * <p>
     * Constructs a textured UV with custom texture dimensions.
     *
     * @param texture 纹理资源位置 / Texture resource location
     * @param x 纹理源X坐标 / Source X coordinate in texture
     * @param y 纹理源Y坐标 / Source Y coordinate in texture
     * @param w 宽度 / Width
     * @param h 高度 / Height
     * @param tw 纹理总宽度 / Total texture width
     * @param th 纹理总高度 / Total texture height
     */
    public TexturedUV(ResourceLocation texture, int x, int y, int w, int h, int tw, int th) {
        super(x, y, w, h, tw, th);
        this.texture = texture;
    }
    /**
     * 从已有的UV对象构造一个带纹理的UV。
     * <p>
     * Constructs a textured UV from an existing UV object.
     *
     * @param texture 纹理资源位置 / Texture resource location
     * @param uv 源UV对象 / Source UV object
     */
    public TexturedUV(ResourceLocation texture, UV uv) {
        super(uv);
        this.texture = texture;
    }
	/**
	 * 使用自定义源宽高绘制纹理。
	 * <p>
	 * Draws the texture with custom source width and height.
	 *
	 * @param s 图形上下文 / Graphics context
	 * @param targetX 目标X坐标 / Target X coordinate
	 * @param targetY 目标Y坐标 / Target Y coordinate
	 * @param sourceWidth 源宽度 / Source width
	 * @param sourceHeight 源高度 / Source height
	 */
	public void blit(GuiGraphics s, int targetX, int targetY, int sourceWidth, int sourceHeight) {
		super.blit(s,texture, targetX, targetY, sourceWidth, sourceHeight);
	}

	/**
	 * 使用自定义源宽度绘制纹理。
	 * <p>
	 * Draws the texture with custom source width.
	 *
	 * @param s 图形上下文 / Graphics context
	 * @param targetX 目标X坐标 / Target X coordinate
	 * @param targetY 目标Y坐标 / Target Y coordinate
	 * @param sourceWidth 源宽度 / Source width
	 */
	public void blit(GuiGraphics s, int targetX, int targetY, int sourceWidth) {
		super.blit(s,texture, targetX, targetY, sourceWidth);
	}

	/**
	 * 在目标位置绘制完整纹理。
	 * <p>
	 * Draws the full texture at the target position.
	 *
	 * @param s 图形上下文 / Graphics context
	 * @param targetX 目标X坐标 / Target X coordinate
	 * @param targetY 目标Y坐标 / Target Y coordinate
	 */
	public void blit(GuiGraphics s, int targetX, int targetY) {
		super.blit(s,texture, targetX, targetY);
	}

	/**
	 * 使用图集网格坐标绘制纹理。
	 * <p>
	 * Draws the texture using atlas grid coordinates.
	 *
	 * @param s 图形上下文 / Graphics context
	 * @param targetX 目标X坐标 / Target X coordinate
	 * @param targetY 目标Y坐标 / Target Y coordinate
	 * @param gridX 网格X索引 / Grid X index
	 * @param gridY 网格Y索引 / Grid Y index
	 */
	public void blitAtlas(GuiGraphics s, int targetX, int targetY, int gridX, int gridY) {
		super.blitAtlas(s,texture, targetX, targetY, gridX, gridY);
	}

	/**
	 * 在带有点偏移的位置使用自定义宽度绘制纹理。
	 * <p>
	 * Draws the texture with custom width at a position with point offset.
	 *
	 * @param s 图形上下文 / Graphics context
	 * @param targetX 目标X坐标 / Target X coordinate
	 * @param targetY 目标Y坐标 / Target Y coordinate
	 * @param loc 位置偏移 / Position offset
	 * @param sourceWidth 源宽度 / Source width
	 */
	public void blit(GuiGraphics s, int targetX, int targetY, Point loc, int sourceWidth) {
		super.blit(s,texture, targetX, targetY, loc, sourceWidth);
	}

	/**
	 * 在带有点偏移的位置绘制纹理。
	 * <p>
	 * Draws the texture at a position with point offset.
	 *
	 * @param s 图形上下文 / Graphics context
	 * @param targetX 目标X坐标 / Target X coordinate
	 * @param targetY 目标Y坐标 / Target Y coordinate
	 * @param loc 位置偏移 / Position offset
	 */
	public void blitAt(GuiGraphics s, int targetX, int targetY, Point loc) {
		super.blitAt(s,texture, targetX, targetY, loc);
	}

	/**
	 * 使用图集网格坐标和点偏移绘制纹理。
	 * <p>
	 * Draws the texture using atlas grid coordinates with point offset.
	 *
	 * @param s 图形上下文 / Graphics context
	 * @param targetX 目标X坐标 / Target X coordinate
	 * @param targetY 目标Y坐标 / Target Y coordinate
	 * @param loc 位置偏移 / Position offset
	 * @param gridX 网格X索引 / Grid X index
	 * @param gridY 网格Y索引 / Grid Y index
	 */
	public void blitAtlas(GuiGraphics s, int targetX, int targetY, Point loc, int gridX, int gridY) {
		super.blitAtlas(s,texture, targetX, targetY, loc, gridX, gridY);
	}

	/**
	 * 使用过渡动画和点偏移绘制纹理。
	 * <p>
	 * Draws the texture with transition animation and point offset.
	 *
	 * @param s 图形上下文 / Graphics context
	 * @param targetX 目标X坐标 / Target X coordinate
	 * @param targetY 目标Y坐标 / Target Y coordinate
	 * @param loc 位置偏移 / Position offset
	 * @param direction 过渡方向 / Transition direction
	 * @param progress 过渡进度（0.0-1.0） / Transition progress (0.0-1.0)
	 */
	public void blit(GuiGraphics s, int targetX, int targetY, Point loc, Transition direction, double progress) {
		super.blit(s,texture, targetX, targetY, loc, direction, progress);
	}

	/**
	 * 使用过渡动画绘制纹理。
	 * <p>
	 * Draws the texture with transition animation.
	 *
	 * @param s 图形上下文 / Graphics context
	 * @param targetX 目标X坐标 / Target X coordinate
	 * @param targetY 目标Y坐标 / Target Y coordinate
	 * @param direction 过渡方向 / Transition direction
	 * @param progress 过渡进度（0.0-1.0） / Transition progress (0.0-1.0)
	 */
	public void blit(GuiGraphics s, int targetX, int targetY, Transition direction, double progress) {
		super.blit(s,texture, targetX, targetY, direction, progress);
	}

	/**
	 * 围绕指定中心点旋转绘制纹理。
	 * <p>
	 * Draws the texture rotated around the specified center point.
	 *
	 * @param graphics 图形上下文 / Graphics context
	 * @param targetX 目标X坐标 / Target X coordinate
	 * @param targetY 目标Y坐标 / Target Y coordinate
	 * @param centerX 旋转中心X偏移 / Rotation center X offset
	 * @param centerY 旋转中心Y偏移 / Rotation center Y offset
	 * @param degrees 旋转角度（度） / Rotation angle in degrees
	 */
	public void blitRotated(GuiGraphics graphics,int targetX, int targetY, int centerX, int centerY, float degrees) {
		super.blitRotated(graphics, texture, targetX, targetY, centerX, centerY, degrees);
	}

	/**
	 * 围绕指定中心点旋转绘制纹理，带点偏移。
	 * <p>
	 * Draws the texture rotated around the specified center point with point offset.
	 *
	 * @param matrixStack 图形上下文 / Graphics context
	 * @param targetX 目标X坐标 / Target X coordinate
	 * @param targetY 目标Y坐标 / Target Y coordinate
	 * @param loc 位置偏移 / Position offset
	 * @param centerX 旋转中心X偏移 / Rotation center X offset
	 * @param centerY 旋转中心Y偏移 / Rotation center Y offset
	 * @param degrees 旋转角度（度） / Rotation angle in degrees
	 */
	public void blitRotated(GuiGraphics matrixStack, int targetX, int targetY, Point loc, int centerX, int centerY, float degrees) {
		super.blitRotated(matrixStack, texture, targetX, targetY, loc, centerX, centerY, degrees);
	}

	/**
	 * 以中心点为基准绘制纹理。
	 * <p>
	 * Draws the texture centered at the specified point.
	 *
	 * @param s 图形上下文 / Graphics context
	 * @param centerX 中心X坐标 / Center X coordinate
	 * @param centerY 中心Y坐标 / Center Y coordinate
	 */
	public void blitCenter(GuiGraphics s,  int centerX, int centerY) {
		super.blitCenter(s, texture, centerX, centerY);
	}

	/**
	 * 以中心点为基准绘制纹理，带点偏移。
	 * <p>
	 * Draws the texture centered at the specified point with point offset.
	 *
	 * @param s 图形上下文 / Graphics context
	 * @param centerX 中心X坐标 / Center X coordinate
	 * @param centerY 中心Y坐标 / Center Y coordinate
	 * @param loc 位置偏移 / Position offset
	 */
	public void blitCenter(GuiGraphics s,  int centerX, int centerY, Point loc) {
		super.blitCenter(s, texture, centerX, centerY, loc);
	}

	/**
	 * 在带有点偏移的位置绘制纹理。
	 * <p>
	 * Draws the texture at a position with point offset.
	 *
	 * @param s 图形上下文 / Graphics context
	 * @param targetX 目标X坐标 / Target X coordinate
	 * @param targetY 目标Y坐标 / Target Y coordinate
	 * @param loc 位置偏移 / Position offset
	 */
	public void blit(GuiGraphics s, int targetX, int targetY, Point loc) {
		super.blit(s, texture, targetX, targetY, loc);
	}

    /**
     * 通过两点坐标差和自定义纹理尺寸创建TexturedUV。
     * <p>
     * Creates a TexturedUV from coordinate deltas with custom texture dimensions.
     *
     * @param texture 纹理资源位置 / Texture resource location
     * @param x1 起始X坐标 / Start X coordinate
     * @param y1 起始Y坐标 / Start Y coordinate
     * @param x2 结束X坐标 / End X coordinate
     * @param y2 结束Y坐标 / End Y coordinate
     * @param tw 纹理总宽度 / Total texture width
     * @param th 纹理总高度 / Total texture height
     * @return 新的TexturedUV实例 / New TexturedUV instance
     */
    public static TexturedUV deltaWH(ResourceLocation texture,int x1, int y1, int x2, int y2,int tw,int th) {
        return new TexturedUV(texture,UV.deltaWH(x1, y1, x2, y2, tw, th));
    }
}
