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

import com.mojang.blaze3d.vertex.PoseStack;
import com.teammoeg.chorda.client.TesselateHelper.TextureTesselator;
import com.teammoeg.chorda.math.Point;
import com.teammoeg.chorda.math.Rect;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.joml.AxisAngle4f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.Objects;

/**
 * UV纹理坐标类。封装纹理的源区域坐标及纹理尺寸，提供多种方式绘制纹理到GUI上，
 * 包括普通绘制、过渡动画、旋转绘制、居中绘制和图集绘制。
 * <p>
 * UV texture coordinate class. Encapsulates the source region coordinates and texture dimensions,
 * providing various methods to blit textures onto the GUI, including normal blit, transition animation,
 * rotated blit, centered blit, and atlas-based blit.
 */
public class UV extends Rect {

	/**
	 * 过渡方向枚举。用于控制纹理绘制的渐变方向。
	 * <p>
	 * Transition direction enum. Controls the direction of texture drawing transition.
	 */
	public enum Transition{
		/** 从下向上过渡 / Transition from bottom to top */
		UP,
		/** 从上向下过渡 / Transition from top to bottom */
		DOWN,
		/** 从右向左过渡 / Transition from right to left */
		LEFT,
		/** 从左向右过渡 / Transition from left to right */
		RIGHT;
	}

	/** 纹理总宽度 / Total texture width */
	@Getter
	final int textureW;
	/** 纹理总高度 / Total texture height */
	@Getter
	final int textureH;

    /**
     * 通过两点坐标差创建UV。
     * <p>
     * Creates a UV from coordinate deltas.
     *
     * @param x1 起始X坐标 / Start X coordinate
     * @param y1 起始Y坐标 / Start Y coordinate
     * @param x2 结束X坐标 / End X coordinate
     * @param y2 结束Y坐标 / End Y coordinate
     * @return 新的UV实例 / New UV instance
     */
    public static UV delta(int x1, int y1, int x2, int y2) {
        return new UV(Rect.delta(x1, y1, x2, y2));
    }
    /**
     * 通过两点坐标差和自定义纹理尺寸创建UV。
     * <p>
     * Creates a UV from coordinate deltas with custom texture dimensions.
     *
     * @param x1 起始X坐标 / Start X coordinate
     * @param y1 起始Y坐标 / Start Y coordinate
     * @param x2 结束X坐标 / End X coordinate
     * @param y2 结束Y坐标 / End Y coordinate
     * @param tw 纹理总宽度 / Total texture width
     * @param th 纹理总高度 / Total texture height
     * @return 新的UV实例 / New UV instance
     */
    public static UV deltaWH(int x1, int y1, int x2, int y2,int tw,int th) {
        return new UV(Rect.delta(x1, y1, x2, y2), tw, th);
    }
    /**
     * 构造UV，使用默认纹理尺寸（256x256）。
     * <p>
     * Constructs a UV with default texture dimensions (256x256).
     *
     * @param x 纹理源X坐标 / Source X coordinate in texture
     * @param y 纹理源Y坐标 / Source Y coordinate in texture
     * @param w 宽度 / Width
     * @param h 高度 / Height
     */
    public UV(int x, int y, int w, int h) {
        super(x, y, w, h);
        textureW=256;
        textureH=256;
    }

    /**
     * 构造UV，使用自定义纹理尺寸。
     * <p>
     * Constructs a UV with custom texture dimensions.
     *
     * @param x 纹理源X坐标 / Source X coordinate in texture
     * @param y 纹理源Y坐标 / Source Y coordinate in texture
     * @param w 宽度 / Width
     * @param h 高度 / Height
     * @param textureW 纹理总宽度 / Total texture width
     * @param textureH 纹理总高度 / Total texture height
     */
    public UV(int x, int y, int w, int h, int textureW, int textureH) {
		super(x, y, w, h);
		this.textureW = textureW;
		this.textureH = textureH;
	}

	/**
	 * 从矩形构造UV，使用默认纹理尺寸（256x256）。
	 * <p>
	 * Constructs a UV from a rectangle with default texture dimensions (256x256).
	 *
	 * @param r 源矩形 / Source rectangle
	 */
	public UV(Rect r) {
        super(r);
        textureW=256;
        textureH=256;
    }

    /**
     * 从矩形构造UV，使用自定义纹理尺寸。
     * <p>
     * Constructs a UV from a rectangle with custom texture dimensions.
     *
     * @param r 源矩形 / Source rectangle
     * @param textureW 纹理总宽度 / Total texture width
     * @param textureH 纹理总高度 / Total texture height
     */
    public UV(Rect r, int textureW, int textureH) {
		super(r);
		this.textureW = textureW;
		this.textureH = textureH;
	}

	/**
	 * 复制构造UV。
	 * <p>
	 * Copy constructs a UV.
	 *
	 * @param uv 源UV对象 / Source UV object
	 */
	public UV(UV uv) {
        this(uv.x, uv.y, uv.w, uv.h,uv.textureW,uv.textureH);
    }
	/**
	 * 围绕指定中心点旋转绘制纹理。通过矩阵变换实现旋转效果。
	 * <p>
	 * Draws the texture rotated around the specified center point. Uses matrix transformation for rotation.
	 *
	 * @param graphics 图形上下文 / Graphics context
	 * @param texture 纹理资源位置 / Texture resource location
	 * @param targetX 目标X坐标 / Target X coordinate
	 * @param targetY 目标Y坐标 / Target Y coordinate
	 * @param centerX 旋转中心X偏移 / Rotation center X offset
	 * @param centerY 旋转中心Y偏移 / Rotation center Y offset
	 * @param degrees 旋转角度（度） / Rotation angle in degrees
	 */
	public void blitRotated(GuiGraphics graphics,ResourceLocation texture, int targetX, int targetY,int centerX,int centerY,float degrees) {
		PoseStack matrixStack=graphics.pose();
		matrixStack.pushPose();
		matrixStack.translate(targetX + centerX, targetY + centerY, 0);//move to gauge center
		matrixStack.mulPose(new Quaternionf(new AxisAngle4f((float)(degrees/180*Math.PI),0f,0f,1f)));//rotate around Z
		graphics.blit(texture,-centerX,-centerY, w, h, x, y, w, h, textureW, textureH);//draw with center offset
		matrixStack.popPose();
	}
    /**
     * 围绕指定中心点旋转绘制纹理，带点偏移。
     * <p>
     * Draws the texture rotated around the specified center point with point offset.
     *
     * @param matrixStack 图形上下文 / Graphics context
     * @param texture 纹理资源位置 / Texture resource location
     * @param targetX 目标X坐标 / Target X coordinate
     * @param targetY 目标Y坐标 / Target Y coordinate
     * @param loc 位置偏移 / Position offset
     * @param centerX 旋转中心X偏移 / Rotation center X offset
     * @param centerY 旋转中心Y偏移 / Rotation center Y offset
     * @param degrees 旋转角度（度） / Rotation angle in degrees
     */
    public void blitRotated(GuiGraphics matrixStack,ResourceLocation texture, int targetX, int targetY,Point loc,int centerX,int centerY,float degrees) {
    	blitRotated(matrixStack,texture, targetX + loc.getX(), targetY + loc.getY(), centerX, centerY, degrees);
    }
    /**
     * 使用自定义源宽高绘制纹理。
     * <p>
     * Draws the texture with custom source width and height.
     *
     * @param s 图形上下文 / Graphics context
     * @param texture 纹理资源位置 / Texture resource location
     * @param targetX 目标X坐标 / Target X coordinate
     * @param targetY 目标Y坐标 / Target Y coordinate
     * @param sourceWidth 源宽度 / Source width
     * @param sourceHeight 源高度 / Source height
     */
    public void blit(GuiGraphics s,ResourceLocation texture, int targetX, int targetY, int sourceWidth, int sourceHeight) {
        s.blit(texture, targetX, targetY, x, y, sourceWidth, sourceHeight, textureW, textureH);
    }
    
    /**
     * 使用自定义源宽度绘制纹理（高度使用默认值）。
     * <p>
     * Draws the texture with custom source width (height uses default).
     *
     * @param s 图形上下文 / Graphics context
     * @param texture 纹理资源位置 / Texture resource location
     * @param targetX 目标X坐标 / Target X coordinate
     * @param targetY 目标Y坐标 / Target Y coordinate
     * @param sourceWidth 源宽度 / Source width
     */
    public void blit(GuiGraphics s,ResourceLocation texture, int targetX, int targetY, int sourceWidth) {
        s.blit(texture, targetX, targetY, x, y, sourceWidth, h, textureW, textureH);
    }

    /**
     * 在目标位置绘制完整纹理。
     * <p>
     * Draws the full texture at the target position.
     *
     * @param s 图形上下文 / Graphics context
     * @param texture 纹理资源位置 / Texture resource location
     * @param targetX 目标X坐标 / Target X coordinate
     * @param targetY 目标Y坐标 / Target Y coordinate
     */
    public void blit(GuiGraphics s,ResourceLocation texture, int targetX, int targetY) {
        s.blit(texture, targetX, targetY, x, y, w, h, textureW, textureH);
    }

	public void blitColored(GuiGraphics s,ResourceLocation texture, int targetX, int targetY, int color) {
        CGuiHelper.bindTexture(texture);
		CGuiHelper.blitColored(s.pose(), targetX, targetY, w, h, x, y, w, h, textureW, textureH, color);
    }
    
    /**
     * 以中心点为基准绘制纹理。
     * <p>
     * Draws the texture centered at the specified point.
     *
     * @param s 图形上下文 / Graphics context
     * @param texture 纹理资源位置 / Texture resource location
     * @param centerX 中心X坐标 / Center X coordinate
     * @param centerY 中心Y坐标 / Center Y coordinate
     */
    public void blitCenter(GuiGraphics s,ResourceLocation texture, int centerX, int centerY) {
        s.blit(texture, centerX - w / 2, centerY - h / 2, x, y, w, h, textureW, textureH);
    }
    /**
     * 以中心点为基准绘制纹理，带点偏移。
     * <p>
     * Draws the texture centered at the specified point with point offset.
     *
     * @param s 图形上下文 / Graphics context
     * @param texture 纹理资源位置 / Texture resource location
     * @param centerX 中心X坐标 / Center X coordinate
     * @param centerY 中心Y坐标 / Center Y coordinate
     * @param loc 位置偏移 / Position offset
     */
    public void blitCenter(GuiGraphics s,ResourceLocation texture, int centerX, int centerY, Point loc) {
    	blitCenter(s,texture, centerX + loc.getX(), centerY + loc.getY());
    }
    /**
     * 使用图集网格坐标绘制纹理。根据网格索引偏移源坐标。
     * <p>
     * Draws the texture using atlas grid coordinates. Offsets the source coordinates based on grid indices.
     *
     * @param s 图形上下文 / Graphics context
     * @param texture 纹理资源位置 / Texture resource location
     * @param targetX 目标X坐标 / Target X coordinate
     * @param targetY 目标Y坐标 / Target Y coordinate
     * @param gridX 网格X索引 / Grid X index
     * @param gridY 网格Y索引 / Grid Y index
     */
    public void blitAtlas(GuiGraphics s,ResourceLocation texture, int targetX, int targetY, int gridX, int gridY) {
    	s.blit(texture, targetX, targetY, x + gridX * w, y + gridY * h, w, h, textureW, textureH);
    }
    /**
     * 在带有点偏移的位置绘制纹理。
     * <p>
     * Draws the texture at a position with point offset.
     *
     * @param s 图形上下文 / Graphics context
     * @param texture 纹理资源位置 / Texture resource location
     * @param targetX 目标X坐标 / Target X coordinate
     * @param targetY 目标Y坐标 / Target Y coordinate
     * @param loc 位置偏移 / Position offset
     */
    public void blit(GuiGraphics s,ResourceLocation texture, int targetX, int targetY, Point loc) {
        blit(s,texture, targetX + loc.getX(), targetY + loc.getY());
    }
    /**
     * 在带有点偏移的位置使用自定义宽度绘制纹理。
     * <p>
     * Draws the texture with custom width at a position with point offset.
     *
     * @param s 图形上下文 / Graphics context
     * @param texture 纹理资源位置 / Texture resource location
     * @param targetX 目标X坐标 / Target X coordinate
     * @param targetY 目标Y坐标 / Target Y coordinate
     * @param loc 位置偏移 / Position offset
     * @param sourceWidth 源宽度 / Source width
     */
    public void blit(GuiGraphics s,ResourceLocation texture,  int targetX, int targetY, Point loc, int sourceWidth) {
        blit(s,texture, targetX + loc.getX(), targetY + loc.getY(), sourceWidth);
    }

    /**
     * 使用过渡动画和点偏移绘制纹理。
     * <p>
     * Draws the texture with transition animation and point offset.
     *
     * @param s 图形上下文 / Graphics context
     * @param texture 纹理资源位置 / Texture resource location
     * @param targetX 目标X坐标 / Target X coordinate
     * @param targetY 目标Y坐标 / Target Y coordinate
     * @param loc 位置偏移 / Position offset
     * @param direction 过渡方向 / Transition direction
     * @param progress 过渡进度（0.0-1.0） / Transition progress (0.0-1.0)
     */
    public void blit(GuiGraphics s,ResourceLocation texture, int targetX, int targetY, Point loc, Transition direction,double progress) {
    	blit(s, texture, targetX + loc.getX(), targetY + loc.getY(), direction, progress);
    }
    /**
     * 使用过渡动画绘制纹理。根据方向和进度裁剪显示区域。
     * <p>
     * Draws the texture with transition animation. Clips the display area based on direction and progress.
     *
     * @param s 图形上下文 / Graphics context
     * @param texture 纹理资源位置 / Texture resource location
     * @param targetX 目标X坐标 / Target X coordinate
     * @param targetY 目标Y坐标 / Target Y coordinate
     * @param direction 过渡方向 / Transition direction
     * @param progress 过渡进度（0.0-1.0） / Transition progress (0.0-1.0)
     */
    public void blit(GuiGraphics s,ResourceLocation texture,  int targetX, int targetY, Transition direction,double progress) {
    	if(progress<0)
    		return;
    	if(progress>1)
    		progress=1;
    	switch(direction) {
    	case UP   :blit(s,texture, targetX, targetY +(int)(h*(1-progress)), w, (int)(h*progress));return;
    	case LEFT :blit(s,texture, targetX +(int)(w*(1-progress)), targetY, (int)(w*progress), h);return;
    	case DOWN :blit(s,texture, targetX, targetY, w, (int)(h*progress));return;
    	case RIGHT:blit(s,texture, targetX, targetY, (int)(w*progress), h);return;
    	}
    }
    //normal blit add point with custom texture size
    public void blitAt(GuiGraphics s,ResourceLocation texture, int targetX, int targetY, Point loc) {
        blit(s,texture, targetX + loc.getX(), targetY + loc.getY());
    }

    //blit with atlas and add point
    public void blitAtlas(GuiGraphics s,ResourceLocation texture, int targetX, int targetY, Point loc, int gridX, int gridY) {
        blitAtlas(s,texture, targetX + loc.getX(), targetY + loc.getY(), gridX, gridY);
    }
    
    
    
	/**
	 * 围绕指定中心点旋转绘制纹理。通过矩阵变换实现旋转效果。
	 * <p>
	 * Draws the texture rotated around the specified center point. Uses matrix transformation for rotation.
	 *
	 * @param graphics 图形上下文 / Graphics context
	 * @param texture 纹理资源位置 / Texture resource location
	 * @param targetX 目标X坐标 / Target X coordinate
	 * @param targetY 目标Y坐标 / Target Y coordinate
	 * @param centerX 旋转中心X偏移 / Rotation center X offset
	 * @param centerY 旋转中心Y偏移 / Rotation center Y offset
	 * @param degrees 旋转角度（度） / Rotation angle in degrees
	 */
	public void tesselateRotated(TextureTesselator texture,GuiGraphics graphics, int targetX, int targetY,int centerX,int centerY,float degrees) {
		Matrix4f pos=new Matrix4f(graphics.pose().last().pose());
		pos.translate(targetX + centerX, targetY + centerY, 0);//move to gauge center
		pos.rotate(new Quaternionf(new AxisAngle4f((float)(degrees/180*Math.PI),0f,0f,1f)));//rotate around Z
		texture.blit(pos,-centerX,-centerY, w, h, x, y, w, h, textureW, textureH);//draw with center offset

	}
    /**
     * 围绕指定中心点旋转绘制纹理，带点偏移。
     * <p>
     * Draws the texture rotated around the specified center point with point offset.
     *
     * @param matrixStack 图形上下文 / Graphics context
     * @param texture 纹理资源位置 / Texture resource location
     * @param targetX 目标X坐标 / Target X coordinate
     * @param targetY 目标Y坐标 / Target Y coordinate
     * @param loc 位置偏移 / Position offset
     * @param centerX 旋转中心X偏移 / Rotation center X offset
     * @param centerY 旋转中心Y偏移 / Rotation center Y offset
     * @param degrees 旋转角度（度） / Rotation angle in degrees
     */
    public void tesselateRotated(TextureTesselator texture,GuiGraphics graphics, int targetX, int targetY,Point loc,int centerX,int centerY,float degrees) {
    	tesselateRotated(texture,graphics, targetX + loc.getX(), targetY + loc.getY(), centerX, centerY, degrees);
    }
    /**
     * 使用自定义源宽高绘制纹理。
     * <p>
     * Draws the texture with custom source width and height.
     *
     * @param s 图形上下文 / Graphics context
     * @param texture 纹理资源位置 / Texture resource location
     * @param targetX 目标X坐标 / Target X coordinate
     * @param targetY 目标Y坐标 / Target Y coordinate
     * @param sourceWidth 源宽度 / Source width
     * @param sourceHeight 源高度 / Source height
     */
    public void tesselate(TextureTesselator texture,GuiGraphics graphics,  int targetX, int targetY, int sourceWidth, int sourceHeight) {
        texture.blit(graphics.pose().last().pose(), targetX, targetY, x, y, sourceWidth, sourceHeight, textureW, textureH);
    }
    
    /**
     * 使用自定义源宽度绘制纹理（高度使用默认值）。
     * <p>
     * Draws the texture with custom source width (height uses default).
     *
     * @param s 图形上下文 / Graphics context
     * @param texture 纹理资源位置 / Texture resource location
     * @param targetX 目标X坐标 / Target X coordinate
     * @param targetY 目标Y坐标 / Target Y coordinate
     * @param sourceWidth 源宽度 / Source width
     */
    public void tesselate(TextureTesselator texture,GuiGraphics graphics, int targetX, int targetY, int sourceWidth) {
        texture.blit(graphics.pose().last().pose(), targetX, targetY, x, y, sourceWidth, h, textureW, textureH);
    }

    /**
     * 在目标位置绘制完整纹理。
     * <p>
     * Draws the full texture at the target position.
     *
     * @param s 图形上下文 / Graphics context
     * @param texture 纹理资源位置 / Texture resource location
     * @param targetX 目标X坐标 / Target X coordinate
     * @param targetY 目标Y坐标 / Target Y coordinate
     */
    public void tesselate(TextureTesselator texture,GuiGraphics graphics, int targetX, int targetY) {
    	texture.blit(graphics.pose().last().pose(), targetX, targetY, x, y, w, h, textureW, textureH);
    }
    
    /**
     * 以中心点为基准绘制纹理。
     * <p>
     * Draws the texture centered at the specified point.
     *
     * @param s 图形上下文 / Graphics context
     * @param texture 纹理资源位置 / Texture resource location
     * @param centerX 中心X坐标 / Center X coordinate
     * @param centerY 中心Y坐标 / Center Y coordinate
     */
    public void tesselateCenter(TextureTesselator texture,GuiGraphics graphics, int centerX, int centerY) {
    	texture.blit(graphics.pose().last().pose(), centerX - w / 2, centerY - h / 2, x, y, w, h, textureW, textureH);
    }
    /**
     * 以中心点为基准绘制纹理，带点偏移。
     * <p>
     * Draws the texture centered at the specified point with point offset.
     *
     * @param s 图形上下文 / Graphics context
     * @param texture 纹理资源位置 / Texture resource location
     * @param centerX 中心X坐标 / Center X coordinate
     * @param centerY 中心Y坐标 / Center Y coordinate
     * @param loc 位置偏移 / Position offset
     */
    public void tesselateCenter(TextureTesselator texture,GuiGraphics graphics, int centerX, int centerY, Point loc) {
    	tesselateCenter(texture, graphics, centerX + loc.getX(), centerY + loc.getY());
    }
    /**
     * 使用图集网格坐标绘制纹理。根据网格索引偏移源坐标。
     * <p>
     * Draws the texture using atlas grid coordinates. Offsets the source coordinates based on grid indices.
     *
     * @param s 图形上下文 / Graphics context
     * @param texture 纹理资源位置 / Texture resource location
     * @param targetX 目标X坐标 / Target X coordinate
     * @param targetY 目标Y坐标 / Target Y coordinate
     * @param gridX 网格X索引 / Grid X index
     * @param gridY 网格Y索引 / Grid Y index
     */
    public void tesselateAtlas(TextureTesselator texture,GuiGraphics graphics, int targetX, int targetY, int gridX, int gridY) {
    	texture.blit(graphics.pose().last().pose(), targetX, targetY, x + gridX * w, y + gridY * h, w, h, textureW, textureH);
    }
    /**
     * 在带有点偏移的位置绘制纹理。
     * <p>
     * Draws the texture at a position with point offset.
     *
     * @param s 图形上下文 / Graphics context
     * @param texture 纹理资源位置 / Texture resource location
     * @param targetX 目标X坐标 / Target X coordinate
     * @param targetY 目标Y坐标 / Target Y coordinate
     * @param loc 位置偏移 / Position offset
     */
    public void tesselate(TextureTesselator texture,GuiGraphics graphics, int targetX, int targetY, Point loc) {
        tesselate(texture, graphics, targetX + loc.getX(), targetY + loc.getY());
    }
    /**
     * 在带有点偏移的位置使用自定义宽度绘制纹理。
     * <p>
     * Draws the texture with custom width at a position with point offset.
     *
     * @param s 图形上下文 / Graphics context
     * @param texture 纹理资源位置 / Texture resource location
     * @param targetX 目标X坐标 / Target X coordinate
     * @param targetY 目标Y坐标 / Target Y coordinate
     * @param loc 位置偏移 / Position offset
     * @param sourceWidth 源宽度 / Source width
     */
    public void tesselate(TextureTesselator texture,GuiGraphics graphics, int targetX, int targetY, Point loc, int sourceWidth) {
        tesselate(texture, graphics, targetX + loc.getX(), targetY + loc.getY(), sourceWidth);
    }

    /**
     * 使用过渡动画和点偏移绘制纹理。
     * <p>
     * Draws the texture with transition animation and point offset.
     *
     * @param s 图形上下文 / Graphics context
     * @param texture 纹理资源位置 / Texture resource location
     * @param targetX 目标X坐标 / Target X coordinate
     * @param targetY 目标Y坐标 / Target Y coordinate
     * @param loc 位置偏移 / Position offset
     * @param direction 过渡方向 / Transition direction
     * @param progress 过渡进度（0.0-1.0） / Transition progress (0.0-1.0)
     */
    public void tesselate(TextureTesselator texture,GuiGraphics graphics, int targetX, int targetY, Point loc, Transition direction,double progress) {
    	tesselate(texture, graphics, targetX + loc.getX(), targetY + loc.getY(), direction, progress);
    }
    /**
     * 使用过渡动画绘制纹理。根据方向和进度裁剪显示区域。
     * <p>
     * Draws the texture with transition animation. Clips the display area based on direction and progress.
     *
     * @param s 图形上下文 / Graphics context
     * @param texture 纹理资源位置 / Texture resource location
     * @param targetX 目标X坐标 / Target X coordinate
     * @param targetY 目标Y坐标 / Target Y coordinate
     * @param direction 过渡方向 / Transition direction
     * @param progress 过渡进度（0.0-1.0） / Transition progress (0.0-1.0)
     */
    public void tesselate(TextureTesselator texture,GuiGraphics graphics, int targetX, int targetY, Transition direction,double progress) {
    	if(progress<0)
    		return;
    	if(progress>1)
    		progress=1;
    	switch(direction) {
    	case UP   :tesselate(texture, graphics, targetX, targetY +(int)(h*(1-progress)), w, (int)(h*progress));return;
    	case LEFT :tesselate(texture, graphics, targetX +(int)(w*(1-progress)), targetY, (int)(w*progress), h);return;
    	case DOWN :tesselate(texture, graphics, targetX, targetY, w, (int)(h*progress));return;
    	case RIGHT:tesselate(texture, graphics, targetX, targetY, (int)(w*progress), h);return;
    	}
    }
    //normal tesselate add point with custom texture size
    public void tesselateAt(TextureTesselator texture,GuiGraphics graphics, int targetX, int targetY, Point loc) {
        tesselate(texture, graphics, targetX + loc.getX(), targetY + loc.getY());
    }

    //tesselate with atlas and add point
    public void tesselateAtlas(TextureTesselator texture,GuiGraphics graphics, int targetX, int targetY, Point loc, int gridX, int gridY) {
        tesselateAtlas(texture, graphics, targetX + loc.getX(), targetY + loc.getY(), gridX, gridY);
    }

	@Override
	public String toString() {
		return "UV{" +
				"textureW=" + textureW +
				", textureH=" + textureH +
				", w=" + w +
				", h=" + h +
				", x=" + x +
				", y=" + y +
				'}';
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(textureH, textureW);
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!super.equals(obj)) return false;
		if (getClass() != obj.getClass()) return false;
		UV other = (UV) obj;
		return textureH == other.textureH && textureW == other.textureW;
	}
}
