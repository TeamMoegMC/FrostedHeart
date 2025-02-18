package com.teammoeg.chorda.client.cui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.teammoeg.chorda.client.CInputHelper;
import com.teammoeg.chorda.client.CInputHelper.Cursor;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public abstract class Layer extends UIWidget {
	@Getter
	protected final List<UIWidget> elements;
	@Getter
	@Setter
	private int offsetX = 0, offsetY = 0;
	private double scrollStep = 20;
	private int contentWidth = -1, contentHeight = -1;
	private RatedScrollbar attachedScrollbar = null;

	public Layer(UIElement panel) {
		super(panel);
		elements = new ArrayList<>();
	}

	public abstract void addUIElements();

	public abstract void alignWidgets();

	public void clearElement() {
		elements.clear();
	}

	public void refresh() {
		contentWidth = contentHeight = -1;

		clearElement();
		try {
			addUIElements();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		elements.sort(null);

		for (UIWidget element : elements) {
			if (element instanceof Layer p) {
				p.refresh();
			}
		}

		alignWidgets();
	}

	public void add(UIWidget element) {
		if (element.getParent() != this) {
			return;
		}
		if (element instanceof RatedScrollbar psb) {
			attachedScrollbar = psb;
		}

		elements.add(element);
		contentWidth = contentHeight = -1;
	}


	public final int align(boolean isHorizontal) {
		contentWidth = contentHeight = 0;
		if(isHorizontal) {
			for(UIWidget elm:elements) {
				elm.setPos(contentWidth,0);
				contentWidth+=elm.getWidth();
				contentHeight=Math.max(elm.getHeight(), contentHeight);
			}
			return contentWidth;
		}
		for(UIWidget elm:elements) {
			elm.setPos(0,contentHeight);
			contentHeight+=elm.getHeight();
			contentWidth=Math.max(elm.getWidth(), contentWidth);
		}
		return contentHeight;
	}
	@Override
	public int getContentX() {
		return super.getContentX() + offsetX;
	}
	@Override
	public int getContentY() {
		return super.getContentY() + offsetY;
	}

	public int getContentWidth() {
		if (contentWidth == -1) {
			if(elements.size()==0) {
				contentWidth=0;
			}else {
				int x1 = Integer.MAX_VALUE;
				int x2 = Integer.MIN_VALUE;
	
				for (UIWidget element : elements) {
					if (element.getX() < x1) {
						x1 = element.getX();
					}
	
					if (element.getX() + element.getWidth() > x2) {
						x2 = element.getX() + element.getWidth();
					}
				}
	
				contentWidth = x2 - x1;
			}
		}

		return contentWidth;
	}

	public int getContentHeight() {
		if (contentHeight == -1) {
			if(elements.size()==0) {
				contentHeight=0;
			}else {
				int y1 = Integer.MAX_VALUE;
				int y2 = Integer.MIN_VALUE;
	
				for (UIWidget element : elements) {
					if (element.getY() < y1) {
						y1 = element.getY();
					}
	
					if (element.getY() + element.getHeight() > y2) {
						y2 = element.getY() + element.getHeight();
					}
				}
	
				contentHeight = y2 - y1 ;
			}
		}

		return contentHeight;
	}

	public void resetOffset(boolean flag) {
		offsetX = offsetY = 0;
	}

	@Override
	public void render(GuiGraphics graphics,  int x, int y, int w, int h) {

		drawBackground(graphics, x, y, w, h);
		graphics.enableScissor(x, y, w, h);

		for(UIWidget elm:elements) {
			drawElement(graphics, elm, x + offsetX, y + offsetY, w, h);
		}


		graphics.disableScissor();
	}
	@Override
	public void updateRenderInfo(double mx, double my, float pt) {
		super.updateRenderInfo(mx, my, pt);
		for(UIWidget elm:elements) {
			elm.updateRenderInfo(getMouseX(), getMouseY(), pt);
		}
	}
	public void drawBackground(GuiGraphics graphics, int x, int y, int w, int h) {
	}

	public void drawElement(GuiGraphics graphics, UIWidget element, int x, int y, int w, int h) {

		element.render(graphics, element.getX()+x, element.getY()+y, element.getWidth(), element.getHeight());
	}

	@Override
	public void getTooltip(Consumer<Component> list) {
		if (!hasTooltip() || !isMouseOver()) {
			return;
		}

		for (int i = elements.size() - 1; i >= 0; i--) {
			UIWidget element = elements.get(i);

			if (element.hasTooltip()) {
				element.getTooltip(list);
			}
		}
	}

	@Override
	public void updateMouseOver() {
		super.updateMouseOver();
		for (UIWidget element : elements) {
			element.updateMouseOver();
		}
	}

	@Override
	public boolean onMousePressed(MouseButton button) {
		if ( !isMouseOver()) {
			return false;
		}

		for (int i = elements.size() - 1; i >= 0; i--) {
			UIWidget element = elements.get(i);

            if (element.isEnabled() && element.isVisible() && element.onMousePressed(button)) {
                return true;
            }
		}
		return false;
	}

	@Override
	public boolean onMouseDoubleClicked(MouseButton button) {
		if (!isMouseOver()) {
			return false;
		}

		for (int i = elements.size() - 1; i >= 0; i--) {
			UIWidget element = elements.get(i);

			if (element.isEnabled() && element.onMouseDoubleClicked(button)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public void onMouseReleased(MouseButton button) {
		for (int i = elements.size() - 1; i >= 0; i--) {
			UIWidget element = elements.get(i);

			if (element.isEnabled()) {
				element.onMouseReleased(button);
			}
		}

	}

	@Override
	public boolean onMouseScrolled(double scroll) {
		for (int i = elements.size() - 1; i >= 0; i--) {
			UIWidget element = elements.get(i);

			if (element.isEnabled() && element.onMouseScrolled(scroll)) {
				return true;
			}
		}
		return false;
		//return onScrollWithoutBar(scroll);
	}

	@Override
	public boolean onMouseDragged(MouseButton button, double dragX, double dragY) {
		for (int i = elements.size() - 1; i >= 0; i--) {
			UIWidget element = elements.get(i);

			if (element.isEnabled() && element.onMouseDragged(button, dragX, dragY)) {
				return true;
			}
		}

		return false;
	}

	public boolean onScrollWithoutBar(double scroll) {
		if (attachedScrollbar != null || !isMouseOver()) {
			return false;
		}

		if (isDefaultScrollVertical() != CInputHelper.isShiftKeyDown()) {
			return applyOffsetDelta(0, -getScrollStep() * scroll);
		} else {
			return applyOffsetDelta(-getScrollStep() * scroll, 0);
		}
	}

	public boolean applyOffsetDelta(double dx, double dy) {
		if (dx == 0 && dy == 0) {
			return false;
		}

		int sx=offsetX;
		int sy=offsetY;
		if (dx != 0) {
			int w = getContentWidth();

			if (w > super.getWidth()) {
				offsetX=(int) Mth.clamp(sx + dx, 0, w - super.getWidth());
			}
		}

		if (dy != 0) {
			int h = getContentHeight();

			if (h > super.getHeight()) {
				offsetY=(int) Mth.clamp(sy + dy, 0, h - super.getHeight());
			}
		}

		return offsetX != sx || offsetY != sy;
	}

	public boolean isDefaultScrollVertical() {
		return true;
	}

	public void setScrollStep(double s) {
		scrollStep = s;
	}

	public double getScrollStep() {
		return scrollStep;
	}

	@Override
	public boolean onKeyPressed(int keyCode,int scanCode,int modifier) {
		if (super.onKeyPressed(keyCode,scanCode,modifier)) {
			return true;
		}


		for (int i = elements.size() - 1; i >= 0; i--) {
			UIWidget element = elements.get(i);

			if (element.isEnabled() && element.onKeyPressed(keyCode,scanCode,modifier)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean onKeyRelease(int keyCode,int scanCode,int modifier) {

		for (int i = elements.size() - 1; i >= 0; i--) {
			UIWidget element = elements.get(i);

			if (element.isEnabled()&&element.onKeyRelease(keyCode,scanCode,modifier)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean onIMEInput(char c, int modifiers) {
		if (super.onIMEInput(c, modifiers)) {
			return true;
		}


		for (int i = elements.size() - 1; i >= 0; i--) {
			UIWidget element = elements.get(i);

			if (element.isEnabled() && element.onIMEInput(c, modifiers)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public void onClosed() {
		for (UIWidget element : elements) {
			element.onClosed();
		}
	}

	@Override
	public void tick() {

		for (UIWidget element : elements) {
			if (element.isEnabled()) {
				element.tick();
			}
		}
	}

	public boolean isMouseOverAnyWidget() {
		for (UIWidget element : elements) {
			if (element.isMouseOver()) {
				return true;
			}
		}

		return false;
	}
	@Override
	public Cursor getCursor() {

		for (var i = elements.size() - 1; i >= 0; i--) {
			UIWidget widget = elements.get(i);
			if (widget.isEnabled() && widget.isMouseOver()) {
				var cursor = widget.getCursor();
				if (cursor != null) {
					return cursor;
				}
			}
		}
		return null;
	}


}