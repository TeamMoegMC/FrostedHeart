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
import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.TooltipBuilder;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.cui.theme.Coloring;
import com.teammoeg.chorda.client.cui.theme.UIColors;
import com.teammoeg.chorda.client.icon.CIcons.CIcon;
import com.teammoeg.chorda.client.icon.FlatIcon;
import com.teammoeg.chorda.text.Components;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;
import java.util.function.Consumer;

/**
 * 文本按钮控件。显示文本标题的按钮，支持图标和文本组合显示。
 * 当文本超出按钮宽度时，自动截断并在悬停时通过提示框显示完整文本。
 * <p>
 * Text button widget. A button that displays a text title, supporting combined
 * icon and text display. When text exceeds the button width, it is automatically
 * truncated and the full text is shown via tooltip on hover.
 */
public abstract class TextButton extends Button {
	@Setter
	@Getter
	protected Coloring textColor=UIColors.BUTTON_TEXT,textOverColor=UIColors.BUTTON_TEXT_OVER,textDisabledColor=UIColors.BUTTON_TEXT_DISABLED;
	/**
	 * 创建文本按钮。
	 * <p>
	 * Creates a text button.
	 *
	 * @param panel 父级UI元素 / Parent UI element
	 * @param txt 按钮文本 / Button text
	 * @param icon 按钮图标 / Button icon
	 */
	public TextButton(UIElement panel, Component txt, CIcon icon) {
		super(panel, txt, icon);
		fitSize();
	}
	protected void fitSize() {
		setWidth(parent.getFont().width(title)+((Components.isEmpty(title)&&hasIcon())?0:8) + (hasIcon() ? 20 : 0));
		setHeight(hasIcon() ?20:16);
	}
	/**
	 * 是否居中渲染标题文本。默认返回false。
	 * <p>
	 * Whether to render the title text centered. Returns false by default.
	 *
	 * @return 是否居中渲染 / Whether to render centered
	 */
	public boolean renderTitleInCenter() {
		return false;
	}

	/** {@inheritDoc} */
	@Override
	public void getTooltip(TooltipBuilder list) {
		Component title=getTitle();
		if ((!Components.isEmpty(title))&&getFont().width(title) + (hasIcon() ? 28 : 8) > super.getWidth()) {
			list.accept(title);
		}
	}

    /**
     * 设置标题并自动调整按钮宽度以适应文本。
     * <p>
     * Sets the title and auto-adjusts button width to fit the text.
     *
     * @param txt 新标题 / New title
     * @return 当前实例（链式调用） / This instance (for chaining)
     */
    public TextButton setTitleAndSize(Component txt) {
        super.setTitle(txt);
        setWidth(getFont().width(getTitle()) + (hasIcon() ? 28 : 8));
        return this;
    }

	/** {@inheritDoc} */
	@Override
	public void render(GuiGraphics graphics, int x, int y, int w, int h, RenderingHint hint) {
		drawBackground(graphics, x, y, w, h, hint);
		var s = h >= 16 ? 16 : 8;
		var off = (h - s) / 2;
		FormattedText title = getTitle();
		var textX = x;
		var textY = y + (h - getFont().lineHeight + 1) / 2;

		var sw = getFont().width(title);
		var mw = w - (hasIcon() ? off + s : 0) - 6;

		if (sw > mw) {
			sw = mw;
			title = getFont().substrByWidth(title, mw);
		}

		if (renderTitleInCenter()) {
			textX += (mw - sw + 6) / 2;
		} else {
			textX += 4;
		}

		if (hasIcon()) {
			drawIcon(graphics, x + off, y + off, s, s);
			textX += off + s;
		}
		List<FormattedCharSequence> list=getFont().split(title, mw);
		int actColor=(isEnabled()?(isMouseOver()? textOverColor: textColor): textDisabledColor).getColorARGB(this, x, y, hint);
		for(FormattedCharSequence fcs:list) {
			graphics.drawString(getFont(), fcs, textX, textY, actColor, hint.theme(this).isButtonTextShadow());
			textY+=7;
		}
	}

	/**
	 * 创建带回调和提示信息的文本按钮。
	 * <p>
	 * Creates a text button with a callback and tooltip.
	 *
	 * @param panel 父级UI元素 / Parent UI element
	 * @param txt 按钮文本 / Button text
	 * @param icon 按钮图标 / Button icon
	 * @param callback 点击回调 / Click callback
	 * @param tooltip 提示信息 / Tooltip components
	 * @return 新创建的文本按钮 / Newly created text button
	 */
	public static TextButton create(UIElement panel, Component txt, CIcon icon, Consumer<MouseButton> callback, Component... tooltip) {
		return new TextButton(panel, txt, icon) {
			@Override
			public void onClicked(MouseButton button) {
				callback.accept(button);
			}

			@Override
			public void getTooltip(TooltipBuilder list) {
				for (Component c : tooltip) {
					list.accept(c);
				}
			}
		};
	}

	/**
	 * 创建"接受"按钮（带勾选图标）。
	 * <p>
	 * Creates an "Accept" button with a checkmark icon.
	 *
	 * @param panel 父级UI元素 / Parent UI element
	 * @param callback 点击回调 / Click callback
	 * @param tooltip 提示信息 / Tooltip components
	 * @return 接受按钮 / Accept button
	 */
	public static TextButton accept(UIElement panel, Consumer<MouseButton> callback, Component... tooltip) {
		return create(panel, Component.translatable("gui.accept"), FlatIcon.CHECK.toCIcon(), callback, tooltip);
	}

	/**
	 * 创建"取消"按钮（带叉号图标）。
	 * <p>
	 * Creates a "Cancel" button with a cross icon.
	 *
	 * @param panel 父级UI元素 / Parent UI element
	 * @param callback 点击回调 / Click callback
	 * @param tooltip 提示信息 / Tooltip components
	 * @return 取消按钮 / Cancel button
	 */
	public static TextButton cancel(UIElement panel, Consumer<MouseButton> callback, Component... tooltip) {
		return create(panel, Component.translatable("gui.cancel"), FlatIcon.CROSS.toCIcon(), callback, tooltip);
	}
}