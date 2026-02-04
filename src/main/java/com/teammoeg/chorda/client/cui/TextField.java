/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.chorda.client.cui;
import java.util.List;
import java.util.Optional;

import com.teammoeg.chorda.lang.Components;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;



public class TextField extends UIElement {
	public static final int H_CENTER = 4;
	public static final int V_CENTER = 32;
	public static final int SHADOW = 2;
	private List<FormattedText> formattedText = List.of();
	private Component component = Components.immutableEmpty();
	public int textFlags = 0;
	public int minWidth = 0;
	public int maxWidth = 5000;
	public int maxLines=Integer.MAX_VALUE;
	public int minLines=1;
	public int textSpacing = 10;
	public float scale = 1.0F;
	public int textColor = 0xFFFFFFFF;
	public boolean trim = false;
	private boolean tooltip = false;

	public TextField(UIElement parent) {
		super(parent);
	}

	public TextField addFlags(int flags) {
		textFlags |= flags;
		return this;
	}
	//Horizontal centered(center in width)
	public TextField centerH() {
		return addFlags(H_CENTER);
	}
	//Vertically centered(center in height)
	public TextField centerV() {
		return addFlags(V_CENTER);
	}
	public TextField shadow() {
		return addFlags(SHADOW);
	}
	public TextField setMinWidth(int width) {
		minWidth = width;
		return this;
	}

	public TextField setMaxWidth(int width) {
		maxWidth = width;
		return this;
	}
	public TextField setMinLines(int lines) {
		minLines = lines;
		return this;
	}

	public TextField setMaxLines(int lines) {
		maxLines = lines;
		return this;
	}
	public TextField setColor(int color) {
		textColor = color;
		return this;
	}

	public TextField setScale(float s) {
		scale = s;
		return this;
	}

	public TextField setSpacing(int s) {
		textSpacing = s;
		return this;
	}

	public TextField setTrim() {
		trim = true;
		return this;
	}

	public TextField showTooltipForLongText() {
		tooltip = true;
		return this;
	}
	public boolean isCentered() {
		return (textFlags&H_CENTER)!=0;
	}
	public boolean isCenteredV() {
		return (textFlags&V_CENTER)!=0;
	}
	public boolean isShadow() {
		return (textFlags&SHADOW)!=0;
	}

	public TextField setText(Component text) {
		
		component = text;
		formattedText =getFont().getSplitter().splitLines(Component.literal("").append(text), maxWidth, Style.EMPTY);
			//ComponentRenderUtils.wrapComponents(Component.literal("").append(text), maxWidth, getFont());

		return resize();
	}

	public TextField setText(String txt) {
		return setText(Component.literal(txt));
	}

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

	@Override
	public void getTooltip(TooltipBuilder list) {
		if (tooltip && formattedText.size() > 1) {
			list.accept(component);
		}
	}
	public void drawBackground(GuiGraphics graphics, int x, int y, int w, int h) {
		//graphics.fill(x, y, x+w, y+h, 0xFFFF0000);
	}

	@Override
	public void render(GuiGraphics graphics,  int x, int y, int w, int h) {
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