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

package com.teammoeg.chorda.client.cui.editor;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.TooltipBuilder;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.chorda.client.cui.widgets.Button;
import com.teammoeg.chorda.client.cui.widgets.LayerScrollBar;
import com.teammoeg.chorda.client.cui.widgets.TextBox;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.client.icon.CIcons.CIcon;
import com.teammoeg.chorda.client.ui.CGuiHelper;
import com.teammoeg.chorda.text.Components;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

public class SelectDialog<T> extends EditDialog {
	public LayerScrollBar scroll;
	public SelectorList rl;
	public TextBox searchBox;
	Component lbl;
	T val;
	Consumer<T> cb;
	Supplier<Collection<T>> fetcher;
	Function<T, Component> tostr;
	Function<T, String[]> tosearch;
	Function<T, CIcon> toicon;

	public SelectDialog(UIElement panel, Component lbl, T val, Consumer<T> cb, Supplier<Collection<T>> fetcher) {
		this(panel, lbl, val, cb, fetcher, e -> Components.str(e.toString()), null, e -> CIcons.nop());
	}

	public SelectDialog(UIElement panel, Component lbl, T val, Consumer<T> cb, Supplier<Collection<T>> fetcher,
		Function<T, Component> tostr) {
		this(panel, lbl, val, cb, fetcher, tostr, null, e -> CIcons.nop());
	}

	public SelectDialog(UIElement panel, Component lbl, T val, Consumer<T> cb, Supplier<Collection<T>> fetcher,
		Function<T, Component> tostr, Function<T, String[]> tosearch) {
		this(panel, lbl, val, cb, fetcher, tostr, tosearch, e -> CIcons.nop());
	}

	public SelectDialog(UIElement panel, Component lbl, T val, Consumer<T> cb, Supplier<Collection<T>> fetcher,
		Function<T, Component> tostr, Function<T, String[]> tosearch, Function<T, CIcon> toicon) {
		super(panel);
		this.lbl = lbl;
		this.val = val;
		this.cb = cb;
		this.fetcher = fetcher;
		this.tostr = tostr;
		this.tosearch = tosearch;
		this.toicon = toicon;
		setSize(300, 200);
	}

	public static <R> Function<R, Component> wrap(Function<R, Object> str) {
		return e -> Components.str(String.valueOf(str.apply(e)));
	}

	@Override
	public void addUIElements() {

		rl = new SelectorList(this);
		searchBox = new TextBox(this) {
			@Override
			public void onTextChanged() {
				rl.refresh();
			}
		};
		searchBox.ghostText = "Search...";
		searchBox.setFocused(true);
		rl.setPosAndSize(5, 25, width - 21, height - 30);
		scroll = new LayerScrollBar(this, rl);
		add(rl);
		add(scroll);
		add(searchBox);
		searchBox.setPosAndSize(5, 5, width - 12, 18);
		scroll.setPosAndSize(width - 16, 25, 8, height - 30);

	}

	@Override
	public void alignWidgets() {

	}

	@Override
	public void render(GuiGraphics matrixStack, int x, int y, int w, int h) {
		super.render(matrixStack, x, y, w, h);
		matrixStack.drawString(getFont(), lbl, x, y - 10, getTheme().getButtonTextColor(),getTheme().isButtonTextShadow());
	}

	@Override
	public void drawBackground(GuiGraphics matrixStack, int x, int y, int w, int h) {
		getTheme().drawUIBackground(matrixStack, x, y, w, h);
	}

	@Override
	public void onClose() {
	}

	public class SelectorButton extends Button {
		T obj;
		SelectorList listPanel;
		Component t;

		public SelectorButton(SelectorList panel, T obj, Component title) {
			super(panel, Components.immutableEmpty(), toicon.apply(obj));
			this.obj = obj;
			this.listPanel = panel;
			t = title;

		}

		@Override
		public void getTooltip(TooltipBuilder list) {
			list.accept(t);
		}

		@Override
		public void render(GuiGraphics matrixStack, int x, int y, int w, int h) {
			// CGuis.setupDrawing();

			matrixStack.blitNineSliced(AbstractWidget.WIDGETS_LOCATION, x, y, w, h, 20, 4, 200, 20, 0, this.getTextureY());
			this.drawIcon(matrixStack, x + 1, y + 1, 16, 16);
			matrixStack.drawString(getFont(), t, x + 18, y + 6, getTheme().getButtonTextColor(),getTheme().isButtonTextShadow());

		}

		private int getTextureY() {
			int i = 1;
			if (val == this.obj) {
				i = 0;
			} else if (this.isMouseOver()) {
				i = 2;
			}

			return 46 + i * 20;
		}

		@Override
		public void onClicked(MouseButton mouseButton) {
			cb.accept(obj);
			close();
		}
	}

	public class SelectorList extends UILayer {
		public SelectorList(UIElement panel) {
			super(panel);
			this.setWidth(200);

		}

		@Override
		public void addUIElements() {
			int offset = 0;
			String stext = searchBox.getText();
			for (T r : fetcher.get()) {
				Component text = tostr.apply(r);
				if (!stext.isEmpty()) {
					boolean flag = false;
					if (tosearch != null)
						for (String s : tosearch.apply(r)) {
							if (s.contains(stext)) {
								flag = true;
								break;
							}
						}
					else if (text.getString().contains(stext)) 
						flag = true;
					if (!flag) continue;
				}
				SelectorButton button = new SelectorButton(this, r, text);
				add(button);
				button.setPosAndSize(2, offset, width - 4, 18);
				offset += 20;
			}
			// scroll.setMaxValue(offset);
		}

		@Override
		public void alignWidgets() {

		}

	}
}
