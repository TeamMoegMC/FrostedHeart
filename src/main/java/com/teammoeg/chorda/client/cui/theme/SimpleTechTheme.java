package com.teammoeg.chorda.client.cui.theme;

import net.minecraft.client.gui.GuiGraphics;

/**
 * 简约科技风格主题。使用线框矩形和青色调配色，呈现简洁的科技感UI风格。
 * <p>
 * Simple tech style theme. Uses wireframe rectangles and cyan-toned colors to present a clean tech UI style.
 */
public class SimpleTechTheme implements Theme{
	/** 单例实例 / Singleton instance */
	public static final SimpleTechTheme INSTANCE=new SimpleTechTheme();

	/**
	 * 受保护的构造函数，使用{@link #INSTANCE}获取实例。
	 * <p>
	 * Protected constructor, use {@link #INSTANCE} to get the instance.
	 */
	protected SimpleTechTheme() {
	}

	/** {@inheritDoc} */
	@Override
	public void drawButton(GuiGraphics graphics, int x, int y, int w, int h, boolean isHighlight, boolean enabled) {
		if (isHighlight) {
			int color = !enabled ? 0xff002439 : 0xffe4eff0;
			drawRect(graphics,x,y,w-1,h-1, color);
		}
		graphics.fill(x+1, y+1, x+w-1, y+h-1, isHighlight&&enabled ? 0x50e4eff0 : 0x50002439);
	}

	@Override
	public void drawSliderBackground(GuiGraphics graphics, int x, int y, int w, int h, boolean isHighlight) {
		drawRect(graphics,x+1,y+1,w-3,h-3,0xffe4eff0);
	}

	@Override
	public void drawTextboxBackground(GuiGraphics graphics, int x, int y, int w, int h, boolean focused) {
		graphics.fill(x, y, x+w, y+h, 0x50002439);
		drawRect(graphics,x,y,w-2,h-1, focused ? 0xffe4eff0 : 0x50e4eff0);
	}

	@Override
	public void drawSliderBar(GuiGraphics graphics, int x, int y, int w, int h, boolean isHighlight) {
		graphics.fill(x+1, y+1, x + w - 1, y + h - 1,0xffe4eff0);
	}

	@Override
	public void drawPanel(GuiGraphics graphics, int x, int y, int w, int h) {
		drawRect(graphics,x,y,w,h, 0xffe4eff0);
	}

	@Override
	public void drawSlot(GuiGraphics graphics, int x, int y, int w, int h) {
		drawRect(graphics,x,y,w,h, 0xffe4eff0);
	}
	/**
	 * 绘制线框矩形。
	 * <p>
	 * Draws a wireframe rectangle.
	 *
	 * @param graphics 图形上下文 / the graphics context
	 * @param x X坐标 / the x position
	 * @param y Y坐标 / the y position
	 * @param w 宽度 / the width
	 * @param h 高度 / the height
	 * @param color 颜色值 / the color value
	 */
	protected void drawRect(GuiGraphics graphics, int x, int y, int w, int h,int color) {
		graphics.hLine(x, x+w, y, color);
		graphics.hLine(x, x+w, y+h, color);
		graphics.vLine(x, y, y+h, color);
		graphics.vLine(x+w, y, y+h, color);
	}
	@Override
	public void drawUIBackground(GuiGraphics graphics, int x, int y, int w, int h) {
		graphics.fill(x, y, x+w, y+h, UIBGColor());
		drawRect(graphics,x,y,w,h, UIBGBorderColor());
	}
	@Override
	public boolean isUITextShadow() {
		return false;
	}
	
	@Override
	public boolean isButtonTextShadow() {
		return false;
	}
	
	@Override
	public int UITextColor() {
		return 0xFFe4eff0;
	}

	@Override
	public int UIAltTextColor() {
		return 0xFFe4eff0;
	}

	@Override
	public int UIBGColor() {
		return 0xaa005066;
	}

	@Override
	public int UIBGBorderColor() {
		return 0xffe4eff0;
	}

	@Override
	public int errorColor() {
		return 0xFFAA9999;
	}

	@Override
	public int successColor() {
		return 0xFF99AA99;
	}


	@Override
	public int buttonTextColor() {
		return 0xFFe4eff0;
	}
	@Override
	public int buttonTextOverColor() {
		return 0xFFFFFFFF;
	}

	@Override
	public int buttonTextDisabledColor() {
		return 0xFF002439;
	}

}
