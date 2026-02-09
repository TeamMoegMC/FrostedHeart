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

package com.teammoeg.chorda.client.cui.base;

import com.teammoeg.chorda.client.CInputHelper.Cursor;
import com.teammoeg.chorda.client.cui.screenadapter.CUIScreen;
import com.teammoeg.chorda.math.Rect;
import com.teammoeg.chorda.client.MouseHelper;
import com.teammoeg.chorda.text.Components;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Abstract ui element, for any basic ui element
 * 
 * */
public class UIElement{
	@Getter
	protected UIElement parent;
	@Getter
	@Setter
	private int x, y;
	@Getter
	protected int width;
	@Getter
	protected int height;
	protected boolean isMouseOver;
	private LayerHolder layerholderCache;
	@Getter
	private double mouseX,mouseY;
	@Getter
	private float partialTick;
	public UIElement(UIElement parent) {
		this.parent = parent;
		//CUIDebugHelper.registerUIObject(this);
	}
	protected void setParent(UIElement parent) {
		this.parent = parent;
	}
	public LayerHolder getLayerHolder() {
		if(layerholderCache==null)
			layerholderCache=parent.getLayerHolder();
		return layerholderCache;
	}

	public void setWidth(int v) {
		width = Math.max(v, 0);
	}

	public void setHeight(int v) {
		height = Math.max(v, 0);
	}


	public void setPos(int x, int y) {
		setX(x);
		setY(y);
	}

	public void setSize(int w, int h) {
		setWidth(w);
		setHeight(h);
	}

	public UIElement setPosAndSize(int x, int y, int w, int h) {
		setX(x);
		setY(y);
		setWidth(w);
		setHeight(h);
		return this;
	}

	public int getScreenX() {
		return parent.getScreenX() + x;
	}

	public int getScreenY() {
		return parent.getScreenY() + y;
	}


	public boolean isEnabled() {
		return true;
	}

	public boolean isVisible() {
		return true;
	}

	public Component getTitle() {
		return Components.immutableEmpty();
	}

	public void getTooltip(TooltipBuilder tooltip) {
		Component title = getTitle();

		if (!Components.isEmpty(title)) {
			tooltip.accept(title);
		}
	}

	public final boolean isMouseOver() {
		return isMouseOver;
	}
	public void updateMouseOver() {
		if (parent == null) {
			isMouseOver=true;
			return;
		} else if (!parent.isMouseOver()) {
			isMouseOver=false;
			return;
		}

		isMouseOver=MouseHelper.isMouseIn(this.getMouseX(), this.getMouseY(), 0,0,this.getWidth(), this.getHeight());
	}

	public Font getFont() {
		return getLayerHolder().getFont();
	}
	public boolean hasTooltip() {
		return isEnabled() && isMouseOver();
	}

	public void render(GuiGraphics graphics, int x, int y, int w, int h) {
	}

	public boolean onMousePressed(MouseButton button) {
		return false;
	}

	public boolean onMouseDoubleClicked(MouseButton button) {
		return false;
	}

	public void onMouseReleased(MouseButton button) {
	}

	public boolean onMouseScrolled(double scroll) {
		return false;
	}

	public boolean onMouseDragged(MouseButton button, double dragX, double dragY) {
		return false;
	}

	public boolean onKeyPressed(int keyCode,int scanCode,int modifier) {
		return false;
	}
	public boolean onKeyRelease(int keyCode,int scanCode,int modifier) {
		return false;
	}

	public boolean onIMEInput(char c, int modifier) {
		return false;
	}

	public void updateRenderInfo(double mx,double my,float pt) {
		this.mouseX=mx-this.getX();
		this.mouseY=my-this.getY();
		if(pt>0)
		this.partialTick=pt;
	}

	public void onClosed() {
	}


	public void tick() {
		//layerholderCache=null;
	}
	public int getContentX() {
		return getScreenX();
	}
	public int getContentY() {
		return getScreenY();
	}
	public Cursor getCursor() {
		return null;
	}
	public void refresh() {
		
	}
	public CUIScreen getManager() {
		return parent.getManager();
	}

	public Rect getBounds() {
		return new Rect(getX(), getY(), getWidth(), getHeight());
	}
}