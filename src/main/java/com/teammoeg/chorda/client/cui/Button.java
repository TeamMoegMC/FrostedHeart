package com.teammoeg.chorda.client.cui;

import com.teammoeg.chorda.client.CInputHelper.Cursor;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.client.icon.CIcons.CIcon;
import com.teammoeg.chorda.client.ui.CGuiHelper;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

public abstract class Button extends UIElement {
	protected Component title;
	protected CIcon icon;

	public Button(UIElementBase panel, Component t, CIcon i) {
		super(panel);
		setSize(16, 16);
		icon = i;
		title = t;
	}

	public Button(UIElementBase panel) {
		this(panel, Component.empty(), CIcons.nop());
	}

	@Override
	public Component getTitle() {
		return title;
	}

	public Button setTitle(Component s) {
		title = s;
		return this;
	}

	public Button setIcon(CIcon i) {
		icon = i;
		return this;
	}

	private int getTextureY() {
		int i = 1;
		if (!this.isEnabled()) {
			i = 0;
		} else if (this.isMouseOver()) {
			i = 2;
		}

		return 46 + i * 20;
	}

	public void drawBackground(GuiGraphics graphics, int x, int y, int w, int h) {
		graphics.blitNineSliced(AbstractWidget.WIDGETS_LOCATION, this.getX(), this.getY(), this.getWidth(), this.getHeight(), 20, 4, 200, 20, 0, this.getTextureY());

	}

	public void drawIcon(GuiGraphics graphics, int x, int y, int w, int h) {
		icon.draw(graphics, x, y, w, h);
	}

	@Override
	public void render(GuiGraphics graphics, int x, int y, int w, int h) {
		CGuiHelper.resetGuiDrawing();
		var s = h >= 16 ? 16 : 8;
		drawBackground(graphics, x, y, w, h);
		drawIcon(graphics, x + (w - s) / 2, y + (h - s) / 2, s, s);
	}

	@Override
	public boolean onMousePressed(MouseButton button) {
		if (isMouseOver()) {
			if (isEnabled()) {
				onClicked(button);
			}

			return true;
		}

		return false;
	}

	public abstract void onClicked(MouseButton button);

	@Override
	public Cursor getCursor() {
		return Cursor.HAND;
	}
}