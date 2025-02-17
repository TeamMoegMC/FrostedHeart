package com.teammoeg.chorda.client.cui;

import com.teammoeg.chorda.client.CInputHelper.Cursor;
import com.teammoeg.chorda.client.MouseHelper;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.function.Consumer;

public class UIElement implements UIElementBase{
	@Getter
	
	private UIElementBase parent;
	@Getter
	@Setter
	private int x, y;
	@Getter
	private int width, height;
	protected boolean isMouseOver;
	private LayerHolder layerholderCache;
	public UIElement(UIElementBase parent) {
		
		if(parent instanceof Layer layer) {
			layer.add(this);
		}else {
			this.parent = parent;
		}
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

	public int getActualX() {
		return parent.getX() + x;
	}

	public int getActualY() {
		return parent.getY() + y;
	}

	public boolean collidesWith(int x, int y, int w, int h) {
		var ay = getY();
		if (ay >= y + h || ay + height <= y) {
			return false;
		}

		var ax = getX();
		return ax < x + w && ax + width > x;
	}

	public boolean isEnabled() {
		return true;
	}

	public boolean isVisible() {
		return true;
	}

	public Component getTitle() {
		return Component.empty();
	}

	public void getTooltip(Consumer<Component> tooltip) {
		Component title = getTitle();

		if (title == Component.empty()) {
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

	public int getMouseX() {
		return parent.getMouseX()-x;
	}

	public int getMouseY() {
		return parent.getMouseY()-y;
	}
	public void onClosed() {
	}


	public void tick() {
		//layerholderCache=null;
	}
	public int getContentX() {
		return getActualX();
	}
	public int getContentY() {
		return getActualY();
	}
	public Cursor getCursor() {
		return null;
	}


}