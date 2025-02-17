package com.teammoeg.chorda.client.cui;

import java.util.function.Consumer;

import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.client.icon.CIcons.CIcon;

import net.minecraft.network.chat.Component;

public abstract class SimpleTextButton extends Button {
	public SimpleTextButton(UIElementBase panel, Component txt, CIcon icon) {
		super(panel, txt, icon);
		setWidth(panel.getFont().width(txt) + (hasIcon() ? 28 : 8));
		setHeight(20);
	}

	@Override
	public SimpleTextButton setTitle(Component txt) {
		super.setTitle(txt);
		setWidth(getFont().width(getTitle()) + (hasIcon() ? 28 : 8));
		return this;
	}

	public boolean renderTitleInCenter() {
		return false;
	}


	public boolean hasIcon() {
		return icon!=CIcons.nop();
	}

	@Override
	public void addMouseOverText(Consumer<Component> list) {
		if (getFont().width(getTitle()) + (hasIcon() ? 28 : 8) > super.getWidth()) {
			list.add(getTitle());
		}
	}

	@Override
	public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
		drawBackground(graphics, theme, x, y, w, h);
		var s = h >= 16 ? 16 : 8;
		var off = (h - s) / 2;
		FormattedText title = getTitle();
		var textX = x;
		var textY = y + (h - theme.getFontHeight() + 1) / 2;

		var sw = theme.getStringWidth(title);
		var mw = w - (hasIcon() ? off + s : 0) - 6;

		if (sw > mw) {
			sw = mw;
			title = theme.trimStringToWidth(title, mw);
		}

		if (renderTitleInCenter()) {
			textX += (mw - sw + 6) / 2;
		} else {
			textX += 4;
		}

		if (hasIcon()) {
			drawIcon(graphics, theme, x + off, y + off, s, s);
			textX += off + s;
		}

		theme.drawString(graphics, title, textX, textY, theme.getContentColor(getWidgetType()), Theme.SHADOW);
	}

	public static SimpleTextButton create(Panel panel, Component txt, Icon icon, Consumer<MouseButton> callback, Component... tooltip) {
		return new SimpleTextButton(panel, txt, icon) {
			@Override
			public void onClicked(MouseButton button) {
				callback.accept(button);
			}

			@Override
			public void addMouseOverText(TooltipList list) {
				for (Component c : tooltip) {
					list.add(c);
				}
			}
		};
	}

	public static SimpleTextButton accept(Panel panel, Consumer<MouseButton> callback, Component... tooltip) {
		return create(panel, Component.translatable("gui.accept"), Icons.ACCEPT, callback, tooltip);
	}

	public static SimpleTextButton cancel(Panel panel, Consumer<MouseButton> callback, Component... tooltip) {
		return create(panel, Component.translatable("gui.cancel"), Icons.CANCEL, callback, tooltip);
	}
}