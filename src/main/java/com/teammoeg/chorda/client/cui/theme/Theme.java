package com.teammoeg.chorda.client.cui.theme;

import com.teammoeg.chorda.client.ClientUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Optional;

/**
 * UI主题接口。定义CUI系统中所有UI组件的绘制方法和颜色配置。
 * <p>
 * UI theme interface. Defines drawing methods and color configurations for all UI components in the CUI system.
 */
public interface Theme {

	/**
	 * 绘制按钮。
	 * <p>
	 * Draws a button.
	 *
	 * @param graphics 图形上下文 / the graphics context
	 * @param x X坐标 / the x position
	 * @param y Y坐标 / the y position
	 * @param w 宽度 / the width
	 * @param h 高度 / the height
	 * @param isHighlight 是否高亮（鼠标悬停） / whether highlighted (mouse hover)
	 * @param enabled 是否启用 / whether enabled
	 */
	void drawButton(GuiGraphics graphics, int x, int y, int w, int h, boolean isHighlight, boolean enabled);

	/**
	 * 绘制滑块背景。
	 * <p>
	 * Draws a slider background.
	 *
	 * @param graphics 图形上下文 / the graphics context
	 * @param x X坐标 / the x position
	 * @param y Y坐标 / the y position
	 * @param w 宽度 / the width
	 * @param h 高度 / the height
	 * @param isHighlight 是否高亮 / whether highlighted
	 */
	void drawSliderBackground(GuiGraphics graphics, int x, int y, int w, int h, boolean isHighlight);

	/**
	 * 绘制文本框背景。
	 * <p>
	 * Draws a text box background.
	 *
	 * @param graphics 图形上下文 / the graphics context
	 * @param x X坐标 / the x position
	 * @param y Y坐标 / the y position
	 * @param w 宽度 / the width
	 * @param h 高度 / the height
	 * @param focused 是否获得焦点 / whether focused
	 */
	void drawTextboxBackground(GuiGraphics graphics, int x, int y, int w, int h, boolean focused);

	/**
	 * 绘制滑块滑块条。
	 * <p>
	 * Draws a slider bar (the draggable part).
	 *
	 * @param graphics 图形上下文 / the graphics context
	 * @param x X坐标 / the x position
	 * @param y Y坐标 / the y position
	 * @param w 宽度 / the width
	 * @param h 高度 / the height
	 * @param isHighlight 是否高亮 / whether highlighted
	 */
	void drawSliderBar(GuiGraphics graphics, int x, int y, int w, int h, boolean isHighlight);

	/**
	 * 绘制面板背景。
	 * <p>
	 * Draws a panel background.
	 *
	 * @param graphics 图形上下文 / the graphics context
	 * @param x X坐标 / the x position
	 * @param y Y坐标 / the y position
	 * @param w 宽度 / the width
	 * @param h 高度 / the height
	 */
	void drawPanel(GuiGraphics graphics, int x, int y, int w, int h);

	/**
	 * 绘制物品槽位背景。
	 * <p>
	 * Draws an item slot background.
	 *
	 * @param graphics 图形上下文 / the graphics context
	 * @param x X坐标 / the x position
	 * @param y Y坐标 / the y position
	 * @param w 宽度 / the width
	 * @param h 高度 / the height
	 */
	void drawSlot(GuiGraphics graphics, int x, int y, int w, int h);

	/**
	 * 绘制UI整体背景。
	 * <p>
	 * Draws the overall UI background.
	 *
	 * @param graphics 图形上下文 / the graphics context
	 * @param x X坐标 / the x position
	 * @param y Y坐标 / the y position
	 * @param w 宽度 / the width
	 * @param h 高度 / the height
	 */
	void drawUIBackground(GuiGraphics graphics, int x, int y, int w, int h);

	/**
	 * 绘制工具提示。使用半透明效果渲染提示文本列表。
	 * <p>
	 * Draws a tooltip. Renders the tooltip text list with semi-transparent effect.
	 *
	 * @param graphics 图形上下文 / the graphics context
	 * @param tooltipLines 提示文本行 / the tooltip text lines
	 * @param mouseX 鼠标X坐标 / the mouse x position
	 * @param mouseY 鼠标Y坐标 / the mouse y position
	 * @param zOffset Z轴偏移 / the z-axis offset
	 */
	default void drawTooltip(GuiGraphics graphics, List<Component> tooltipLines, int mouseX, int mouseY, int zOffset) {
		graphics.pose().pushPose();
		graphics.pose().translate(0, 0, zOffset);
		graphics.setColor(1f, 1f, 1f, 0.8f);
		graphics.renderTooltip(ClientUtils.getMc().font, tooltipLines, Optional.empty(), mouseX, Math.max(mouseY, 18));
		graphics.setColor(1f, 1f, 1f, 1f);
		graphics.pose().popPose();
	}

	/**
	 * UI主文本颜色。
	 * <p>
	 * UI primary text color.
	 *
	 * @return ARGB颜色值 / the ARGB color value
	 */
	int UITextColor();

	/**
	 * UI次要文本颜色，用于和主文本区分，一般会更淡。
	 * <p>
	 * UI alternate text color, used to differentiate from primary text, usually lighter.
	 *
	 * @return ARGB颜色值 / the ARGB color value
	 */
	int UIAltTextColor();

	/**
	 * UI背景颜色。
	 * <p>
	 * UI background color.
	 *
	 * @return ARGB颜色值 / the ARGB color value
	 */
	int UIBGColor();

	/**
	 * UI背景边框颜色。
	 * <p>
	 * UI background border color.
	 *
	 * @return ARGB颜色值 / the ARGB color value
	 */
	int UIBGBorderColor();

	/**
	 * 按钮文本颜色。
	 * <p>
	 * Button text color.
	 *
	 * @return ARGB颜色值 / the ARGB color value
	 */
	int buttonTextColor();

	/**
	 * 按钮悬停时的文本颜色。
	 * <p>
	 * Button text color when hovered.
	 *
	 * @return ARGB颜色值 / the ARGB color value
	 */
	int buttonTextOverColor();

	/**
	 * 按钮禁用时的文本颜色。
	 * <p>
	 * Button text color when disabled.
	 *
	 * @return ARGB颜色值 / the ARGB color value
	 */
	int buttonTextDisabledColor();

	/**
	 * 错误状态颜色。
	 * <p>
	 * Error state color.
	 *
	 * @return ARGB颜色值 / the ARGB color value
	 */
	int errorColor();

	/**
	 * 成功状态颜色。
	 * <p>
	 * Success state color.
	 *
	 * @return ARGB颜色值 / the ARGB color value
	 */
	int successColor();

	/**
	 * UI文本是否渲染阴影。
	 * <p>
	 * Whether UI text renders with shadow.
	 *
	 * @return 是否启用阴影 / whether shadow is enabled
	 */
	boolean isUITextShadow();

	/**
	 * 按钮文本是否渲染阴影。
	 * <p>
	 * Whether button text renders with shadow.
	 *
	 * @return 是否启用阴影 / whether shadow is enabled
	 */
	boolean isButtonTextShadow();
}