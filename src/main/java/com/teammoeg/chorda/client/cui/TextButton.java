package com.teammoeg.chorda.client.cui;

import java.util.List;
import java.util.function.Consumer;

import com.teammoeg.chorda.client.icon.CIcons.CIcon;

import com.teammoeg.chorda.client.icon.FlatIcon;
import com.teammoeg.chorda.lang.Components;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;

public abstract class TextButton extends Button {
	public TextButton(UIElement panel, Component txt, CIcon icon) {
		super(panel, txt, icon);
		fitSize();
	}

	public boolean renderTitleInCenter() {
		return false;
	}



	@Override
	public void getTooltip(TooltipBuilder list) {
		if ((!Components.isEmpty(getTitle()))&&getFont().width(getTitle()) + (hasIcon() ? 28 : 8) > super.getWidth()) {
			list.accept(getTitle());
		}
	}

	@Override
	public void render(GuiGraphics graphics, int x, int y, int w, int h) {
		drawBackground(graphics, x, y, w, h);
		var s = h >= 16 ? 16 : 8;
		var off = (h - s) / 2;
		FormattedText title;
		title = getTitle();
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
		for(FormattedCharSequence fcs:list) {
			graphics.drawString(getFont(), fcs, textX, textY, 0xFFFFFFFF,true);
			textY+=7;
		}
	}

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

	public static TextButton accept(UIElement panel, Consumer<MouseButton> callback, Component... tooltip) {
		return create(panel, Component.translatable("gui.accept"), FlatIcon.CHECK.toCIcon(), callback, tooltip);
	}

	public static TextButton cancel(UIElement panel, Consumer<MouseButton> callback, Component... tooltip) {
		return create(panel, Component.translatable("gui.cancel"), FlatIcon.CROSS.toCIcon(), callback, tooltip);
	}
}