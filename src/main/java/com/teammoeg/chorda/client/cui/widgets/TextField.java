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

package com.teammoeg.chorda.client.cui.widgets;

import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.TooltipBuilder;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.text.Components;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;

import java.util.List;
import java.util.Optional;



/**
 * 文本显示字段控件。用于显示格式化文本的只读UI元素，支持自动换行、居中对齐、
 * 缩放、行数限制和截断提示等功能。
 * <p>
 * Text display field widget. A read-only UI element for displaying formatted text,
 * supporting auto-wrapping, center alignment, scaling, line count limits,
 * and truncation tooltips.
 */
public class TextField extends UIElement {
	/** 水平居中标志位 / Horizontal centering flag */
	public static final int H_CENTER = 4;
	/** 垂直居中标志位 / Vertical centering flag */
	public static final int V_CENTER = 32;
	/** 文本阴影标志位 / Text shadow flag */
	public static final int SHADOW = 2;
	/** 格式化后的文本行列表 / List of formatted text lines */
	private List<FormattedText> formattedText = List.of();
	/** 原始文本组件 / Original text component */
	private Component component = Components.immutableEmpty();
	/** 文本样式标志位组合 / Text style flags combination */
	public int textFlags = 0;
	/** 最小宽度 / Minimum width */
	public int minWidth = 0;
	/** 最大宽度（用于自动换行） / Maximum width (for auto-wrapping) */
	public int maxWidth = 5000;
	/** 最大显示行数 / Maximum number of lines to display */
	public int maxLines=Integer.MAX_VALUE;
	/** 最小显示行数 / Minimum number of lines to display */
	public int minLines=1;
	/** 行间距（像素） / Line spacing in pixels */
	public int textSpacing = 10;
	/** 文本缩放比例 / Text scale factor */
	public float scale = 1.0F;
	/** 文本颜色 / Text color */
	public int textColor;
	/** 是否裁剪文本 / Whether to trim text */
	public boolean trim = false;
	/** 是否在多行文本时显示完整内容提示框 / Whether to show full content tooltip for multiline text */
	private boolean tooltip = false;

	/**
	 * 创建文本显示字段。
	 * <p>
	 * Creates a text display field.
	 *
	 * @param parent 父级UI元素 / Parent UI element
	 */
	public TextField(UIElement parent) {
		super(parent);
		textColor= theme().buttonTextColor();
		if(theme().isUITextShadow())
			shadow();
	}

	/**
	 * 添加文本样式标志位。
	 * <p>
	 * Adds text style flags.
	 *
	 * @param flags 标志位 / Flags to add
	 * @return 当前实例（链式调用） / This instance (for chaining)
	 */
	public TextField addFlags(int flags) {
		textFlags |= flags;
		return this;
	}

	/**
	 * 删除文本样式标志位。
	 * <p>
	 * Removes text style flags.
	 *
	 * @param flags 标志位 / Flags to remove
	 * @return 当前实例（链式调用） / This instance (for chaining)
	 */
	public TextField deleteFlags(int flags) {
		textFlags ^= flags;
		return this;
	}

	/**
	 * 设置文本水平居中。
	 * <p>
	 * Sets text to be horizontally centered.
	 *
	 * @return 当前实例（链式调用） / This instance (for chaining)
	 */
	public TextField centerH() {
		return addFlags(H_CENTER);
	}

	/**
	 * 设置文本垂直居中。
	 * <p>
	 * Sets text to be vertically centered.
	 *
	 * @return 当前实例（链式调用） / This instance (for chaining)
	 */
	public TextField centerV() {
		return addFlags(V_CENTER);
	}

	/**
	 * 启用文本阴影。
	 * <p>
	 * Enables text shadow.
	 *
	 * @return 当前实例（链式调用） / This instance (for chaining)
	 */
	public TextField shadow() {
		return addFlags(SHADOW);
	}

	/**
	 * 禁用文本阴影。
	 * <p>
	 * Disables text shadow.
	 *
	 * @return 当前实例（链式调用） / This instance (for chaining)
	 */
	public TextField noShadow() {
		return deleteFlags(SHADOW);
	}

	/**
	 * 设置最小宽度。
	 * <p>
	 * Sets the minimum width.
	 *
	 * @param width 最小宽度 / Minimum width
	 * @return 当前实例（链式调用） / This instance (for chaining)
	 */
	public TextField setMinWidth(int width) {
		minWidth = width;
		return this;
	}

	/**
	 * 设置最大宽度（用于自动换行）。
	 * <p>
	 * Sets the maximum width (used for auto-wrapping).
	 *
	 * @param width 最大宽度 / Maximum width
	 * @return 当前实例（链式调用） / This instance (for chaining)
	 */
	public TextField setMaxWidth(int width) {
		maxWidth = width;
		return this;
	}

	/**
	 * 设置最小显示行数。
	 * <p>
	 * Sets the minimum number of display lines.
	 *
	 * @param lines 最小行数 / Minimum lines
	 * @return 当前实例（链式调用） / This instance (for chaining)
	 */
	public TextField setMinLines(int lines) {
		minLines = lines;
		return this;
	}

	/**
	 * 设置最大显示行数。
	 * <p>
	 * Sets the maximum number of display lines.
	 *
	 * @param lines 最大行数 / Maximum lines
	 * @return 当前实例（链式调用） / This instance (for chaining)
	 */
	public TextField setMaxLines(int lines) {
		maxLines = lines;
		return this;
	}

	/**
	 * 设置文本颜色。
	 * <p>
	 * Sets the text color.
	 *
	 * @param color 颜色值 / Color value
	 * @return 当前实例（链式调用） / This instance (for chaining)
	 */
	public TextField setColor(int color) {
		textColor = color;
		return this;
	}

	/**
	 * 设置文本缩放比例。
	 * <p>
	 * Sets the text scale factor.
	 *
	 * @param s 缩放比例 / Scale factor
	 * @return 当前实例（链式调用） / This instance (for chaining)
	 */
	public TextField setScale(float s) {
		scale = s;
		return this;
	}

	/**
	 * 设置行间距。
	 * <p>
	 * Sets the line spacing.
	 *
	 * @param s 行间距（像素） / Line spacing in pixels
	 * @return 当前实例（链式调用） / This instance (for chaining)
	 */
	public TextField setSpacing(int s) {
		textSpacing = s;
		return this;
	}

	/**
	 * 启用文本裁剪。
	 * <p>
	 * Enables text trimming.
	 *
	 * @return 当前实例（链式调用） / This instance (for chaining)
	 */
	public TextField setTrim() {
		trim = true;
		return this;
	}

	/**
	 * 启用多行文本的完整内容提示框。
	 * <p>
	 * Enables full content tooltip for multiline text.
	 *
	 * @return 当前实例（链式调用） / This instance (for chaining)
	 */
	public TextField showTooltipForLongText() {
		tooltip = true;
		return this;
	}

	/**
	 * 判断是否水平居中。
	 * <p>
	 * Checks whether horizontally centered.
	 *
	 * @return 是否水平居中 / Whether horizontally centered
	 */
	public boolean isCentered() {
		return (textFlags&H_CENTER)!=0;
	}

	/**
	 * 判断是否垂直居中。
	 * <p>
	 * Checks whether vertically centered.
	 *
	 * @return 是否垂直居中 / Whether vertically centered
	 */
	public boolean isCenteredV() {
		return (textFlags&V_CENTER)!=0;
	}

	/**
	 * 判断是否启用文本阴影。
	 * <p>
	 * Checks whether text shadow is enabled.
	 *
	 * @return 是否启用阴影 / Whether shadow is enabled
	 */
	public boolean isShadow() {
		return (textFlags&SHADOW)!=0;
	}

	/**
	 * 设置显示的文本组件，并自动换行和调整尺寸。
	 * <p>
	 * Sets the text component to display, auto-wrapping and resizing.
	 *
	 * @param text 文本组件 / Text component
	 * @return 当前实例（链式调用） / This instance (for chaining)
	 */
	public TextField setText(Component text) {

		component = text;
		formattedText =getFont().getSplitter().splitLines(Component.literal("").append(text), maxWidth, Style.EMPTY);
			//ComponentRenderUtils.wrapComponents(Component.literal("").append(text), maxWidth, getFont());

		return resize();
	}

	/**
	 * 设置显示的文本字符串。
	 * <p>
	 * Sets the text string to display.
	 *
	 * @param txt 文本字符串 / Text string
	 * @return 当前实例（链式调用） / This instance (for chaining)
	 */
	public TextField setText(String txt) {
		return setText(Component.literal(txt));
	}

	/**
	 * 根据文本内容重新计算控件尺寸。
	 * <p>
	 * Recalculates widget size based on text content.
	 *
	 * @return 当前实例（链式调用） / This instance (for chaining)
	 */
	public TextField resize() {
		setWidth(0);

		for (FormattedText s : formattedText) {
			setWidth(Math.max(getWidth(), (int) ((float) getFont().width(s) * scale)));
		}

		setWidth(Mth.clamp(getWidth(), minWidth, maxWidth));
		setHeight((int) ((float) (Math.max(minLines, Math.min(formattedText.size(), maxLines)) * textSpacing - (textSpacing - getFont().lineHeight + 1)) * scale));
		//System.out.println("dims="+this.getX()+","+this.getY()+":"+this.getWidth()+","+this.getHeight());
		return this;
	}

	/** {@inheritDoc} */
	@Override
	public void getTooltip(TooltipBuilder list) {
		if (tooltip && formattedText.size() > 1) {
			list.accept(component);
		}
	}
	/**
	 * 绘制文本字段背景。默认为空实现。
	 * <p>
	 * Draws the text field background. No-op by default.
	 *
	 * @param graphics 图形上下文 / Graphics context
	 * @param x X坐标 / X coordinate
	 * @param y Y坐标 / Y coordinate
	 * @param w 宽度 / Width
	 * @param h 高度 / Height
	 */
	public void drawBackground(GuiGraphics graphics, int x, int y, int w, int h) {
		//graphics.fill(x, y, x+w, y+h, 0xFFFF0000);
	}

	/** {@inheritDoc} */
	@Override
	public void render(GuiGraphics graphics,  int x, int y, int w, int h, RenderingHint hint) {
		drawBackground(graphics, x, y, w, h);
		
		if (formattedText.size() != 0) {
			boolean centered = this.isCentered();
			boolean centeredV = this.isCenteredV();
			int col = textColor;

			;
			int ty = y + (centeredV ? (h - getFont().lineHeight) / 2 : 0);
			int i=-1;
			if (scale == 1.0F) {
				for (FormattedText text:formattedText) {
					int tx = x + (centered ? (w-(int) ((float) getFont().width(text) )) / 2 : 0);
					graphics.drawString(getFont(),Language.getInstance().getVisualOrder(text), tx, ty + (++i) * textSpacing, col, isShadow());
					if(i+1>=maxLines) {
						break;
					}
				}
			} else {
				graphics.pose().pushPose();
				graphics.pose().translate(0, ty, 0.0D);
				graphics.pose().scale(scale, scale, 1.0F);

				for (FormattedText text:formattedText) {
					if(centered) {
						graphics.pose().pushPose();
						int tx = x + (centered ? (w-(int) ((float) getFont().width(text) * scale)) / 2 : 0);
						graphics.pose().translate(tx, 0, 0.0D);
					}
					graphics.drawString(getFont(), Language.getInstance().getVisualOrder(text), 0, (++i) * textSpacing, col, isShadow());
					if(centered) {
						graphics.pose().popPose();
					}
					if(i+1>=maxLines) {
						break;
					}
				}

				graphics.pose().popPose();
			}
		}
	}
	
	/**
	 * 获取鼠标位置处文本的样式。用于处理可点击文本组件。
	 * <p>
	 * Gets the text style at the mouse position. Used for handling clickable text components.
	 *
	 * @param mouseX 鼠标X坐标 / Mouse X coordinate
	 * @param mouseY 鼠标Y坐标 / Mouse Y coordinate
	 * @return 该位置的文本样式（如果有） / Text style at that position, if any
	 */
	public Optional<Style> getStyle(int mouseX, int mouseY) {
		int line = (mouseY - getY()) / getFont().lineHeight;
		if (line >= 0 && line < formattedText.size() && line<maxLines) {
			boolean centered = this.isCentered();
			int textWidth = getFont().width(formattedText.get(line));
			int xStart = centered ? getX() + (getWidth() - textWidth) / 2: getX();
			if (mouseX >= xStart && mouseX <= xStart + textWidth) {
				return Optional.ofNullable(getFont().getSplitter().componentStyleAtWidth(formattedText.get(line), mouseX - xStart));
			}
		}
		return Optional.empty();
	}
}