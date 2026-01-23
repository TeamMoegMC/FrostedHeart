package com.teammoeg.chorda.client.cui;

import com.teammoeg.chorda.Chorda;
import com.teammoeg.chorda.client.CInputHelper;
import com.teammoeg.chorda.client.CInputHelper.Cursor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * UI Layer, contains elements as well as manage them
 * 
 * */
public abstract class UILayer extends UIElement {
	
	@Getter
	protected final List<UIElement> elements;
	@Getter
	@Setter
	private int offsetX = 0, offsetY = 0, zIndex=0;
	@Getter
	private float displayOffsetX = 0, displayOffsetY = 0;
	@Getter
	private long lastFrameTime = 0;
	@Getter
	@Setter
	private boolean smoothScrollEnabled = false;
	private double scrollStep = 20;
	private int contentWidth = -1, contentHeight = -1;
	private RatedScrollbar attachedScrollbar = null;
	@Getter
	@Setter
	private boolean scissorEnabled=true;
	public UILayer(UIElement panel) {
		super(panel);
		elements = new ArrayList<>();
	}

	public abstract void addUIElements();

	public abstract void alignWidgets();

	public void clearElement() {
		elements.clear();
	}
	public final void recalcContentSize() {
		contentWidth = contentHeight = -1;
	}
	public void refresh() {
		recalcContentSize();

		clearElement();
		try {
			addUIElements();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		//elements.sort(null);

		for (UIElement element : elements) {
			element.refresh();
		}

		alignWidgets();
	}


	public void add(UIElement element) {
		if (element.getParent() != this) {
			Chorda.LOGGER.warn(Chorda.UI, element+" Could not be added because parent mismatch");
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
			for(UIElement elm:elements) {
				elm.setX(contentWidth);
				contentWidth+=elm.getWidth()+1;
				contentHeight=Math.max(elm.getHeight(), contentHeight);
			}
			return contentWidth;
		}
		for(UIElement elm:elements) {
			elm.setY(contentHeight);
			contentHeight+=elm.getHeight()+1;
			contentWidth=Math.max(elm.getWidth(), contentWidth);
		}
		return contentHeight;
	}

	public final int align(int lineSpace, boolean isHorizontal) {
		contentWidth = contentHeight = 0;
		if(isHorizontal) {
			for(UIElement elm:elements) {
				elm.setX(contentWidth);
				contentWidth+=elm.getWidth()+lineSpace;
				contentHeight=Math.max(elm.getHeight(), contentHeight);
			}
			contentWidth -= lineSpace;
			return contentWidth;
		}
		for(UIElement elm:elements) {
			elm.setY(contentHeight);
			contentHeight+=elm.getHeight()+lineSpace;
			contentWidth=Math.max(elm.getWidth(), contentWidth);
		}
		contentHeight -= lineSpace;
		return contentHeight;
	}

	public final int align(int start, int lineSpace, boolean isHorizontal) {
		contentWidth = contentHeight = 0;
		if(isHorizontal) {
			contentWidth += start;
			for(UIElement elm:elements) {
				elm.setX(contentWidth);
				contentWidth+=elm.getWidth()+lineSpace;
				contentHeight=Math.max(elm.getHeight(), contentHeight);
			}
			contentWidth -= lineSpace;
			return contentWidth;
		}
		contentHeight += start;
		for(UIElement elm:elements) {
			elm.setY(contentHeight);
			contentHeight+=elm.getHeight()+lineSpace;
			contentWidth=Math.max(elm.getWidth(), contentWidth);
		}
		contentHeight -= lineSpace;
		return contentHeight;
	}

	public final void flow(boolean isHorizontalFirst) {
		contentWidth = contentHeight = 0;
		int currentX=0,currentY=0;
		int currentH=0,currentW=0;;
		if(isHorizontalFirst) {
			for(UIElement elm:elements) {
				if(currentX+elm.getWidth()>width) {
					contentWidth=Math.max(contentWidth, currentX);
					currentY+=currentH+1;
					currentX=0;
					currentH=0;
				}
				elm.setPos(currentX,currentY);
				currentX+=elm.getWidth()+1;
				currentH=Math.max(elm.getHeight(), currentH);
			}
			contentHeight=currentY+currentH;
			return;
		}
		for(UIElement elm:elements) {
			if(currentY+elm.getHeight()>height) {
				contentHeight=Math.max(contentHeight, currentY);
				currentX+=currentW+1;
				currentY=0;
				currentW=0;
			}
			elm.setPos(currentX,currentY);
			currentY+=elm.getHeight()+1;
			currentW=Math.max(elm.getWidth(), currentW);
		}
		contentWidth=currentX+currentW;
		
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
	
				for (UIElement element : elements) {
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
	
				for (UIElement element : elements) {
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

		


		int contentX = x;
		int contentY = y;
		if (isSmoothScrollEnabled()) {
			long now = Util.getNanos();
			float delta = (now - lastFrameTime) / 1_000_000_000.0f;
			delta = Math.min(delta, 0.1F);

			float f = 1.0f - (float)Math.exp(-delta / 0.05F);
			displayOffsetX += (offsetX - displayOffsetX) * f;
			displayOffsetY += (offsetY - displayOffsetY) * f;
			contentX += Math.round(displayOffsetX);
			contentY += Math.round(displayOffsetY);
		} else {
			contentX += offsetX;
			contentY += offsetY;
		}

		graphics.pose().pushPose();
		graphics.pose().translate(0, 0, zIndex);
		drawBackground(graphics, x, y, w, h);
		if(scissorEnabled)
			graphics.enableScissor(x, y, x+w, y+h);
		graphics.pose().translate(displayOffsetX-(int)displayOffsetX, displayOffsetX-(int)displayOffsetX, 0);
		for(UIElement elm:elements) {
			if(elm.isVisible()) {
				drawElement(graphics, elm,x,y, contentX, contentY, w, h);
			}
		}
		graphics.pose().popPose();

		if(scissorEnabled)
			graphics.disableScissor();

		lastFrameTime = Util.getNanos();
		//if(isMouseOver)
		//	TechIcons.ADD.draw(graphics, (int)getMouseX()+x-4, (int)getMouseY()+y-4, 8, 8);
		//graphics.pose().popPose();
	}
	@Override
	public void updateRenderInfo(double mx, double my, float pt) {
		super.updateRenderInfo(mx, my, pt);
		for(UIElement elm:elements) {
			elm.updateRenderInfo(getMouseX()-offsetX, getMouseY()-offsetY, pt);
		}
	}
	public void drawBackground(GuiGraphics graphics, int x, int y, int w, int h) {
	}

	public void drawElement(GuiGraphics graphics, UIElement element,int parX,int parY, int x, int y, int w, int h) {
		int childX=element.getX()+x;
		int childY=element.getY()+y;
		int childW=element.getWidth();
		int childH=element.getHeight();
		//Skip rendering out-of-bounds content if scissor is enabled, improve performance
		if(scissorEnabled) {
			if(childX>w+parX||childY>h+parY||childX+childW<parX||childY+childH<parY) {
				return;
			}
		}

		element.render(graphics, childX, childY, childW, childH);
		if(CUIDebugHelper.isDebugEnabled()) {
			graphics.hLine(childX, childX+childW, childY, 0xFF00FF00);
			graphics.vLine(childX, childY, childY+childH, 0xFF00FF00);
			graphics.hLine(childX, childX+childW, childY+childH, 0xFF00FF00);
			graphics.vLine(childX+childW, childY, childY+childH, 0xFF00FF00);
		}
	}

	@Override
	public void getTooltip(TooltipBuilder list) {
		if (!hasTooltip() || !isMouseOver()) {
			return;
		}

		for (int i = elements.size() - 1; i >= 0; i--) {
			UIElement element = elements.get(i);

			if (element.hasTooltip()) {
				element.getTooltip(list);
			}
		}
	}

	@Override
	public void updateMouseOver() {
		super.updateMouseOver();
		for (UIElement element : elements) {
			element.updateMouseOver();
		}
	}

	@Override
	public boolean onMousePressed(MouseButton button) {
		if ( !isMouseOver()) {
			return false;
		}

		for (int i = elements.size() - 1; i >= 0; i--) {
			UIElement element = elements.get(i);

            if (element.isEnabled() && element.isVisible() && element.onMousePressed(button)) {
            	if(CUIDebugHelper.isDebugEnabled())
            		Chorda.LOGGER.debug(Chorda.UI, "consumed mousePressed: "+button+" "+element);
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
			UIElement element = elements.get(i);

			if (element.isEnabled() && element.isVisible() && element.onMouseDoubleClicked(button)) {
				if(CUIDebugHelper.isDebugEnabled())
					Chorda.LOGGER.debug(Chorda.UI, "consumed mouseDoubleClicked: "+button+" "+element);
				return true;
			}
		}

		return false;
	}

	@Override
	public void onMouseReleased(MouseButton button) {
		for (int i = elements.size() - 1; i >= 0; i--) {
			UIElement element = elements.get(i);

			if (element.isEnabled() && element.isVisible()) {
				element.onMouseReleased(button);
			}
		}

	}

	@Override
	public boolean onMouseScrolled(double scroll) {
		for (int i = elements.size() - 1; i >= 0; i--) {
			UIElement element = elements.get(i);

			if (element.isEnabled() && element.isVisible() && element.onMouseScrolled(scroll)) {
				if(CUIDebugHelper.isDebugEnabled())
					Chorda.LOGGER.debug(Chorda.UI, "consumed mouseScrolled: "+scroll+" "+element);
				return true;
			}
		}
		return false;
		//return onScrollWithoutBar(scroll);
	}

	@Override
	public boolean onMouseDragged(MouseButton button, double dragX, double dragY) {
		for (int i = elements.size() - 1; i >= 0; i--) {
			UIElement element = elements.get(i);

			if (element.isEnabled() && element.isVisible() && element.onMouseDragged(button, dragX, dragY)) {
				if(CUIDebugHelper.isDebugEnabled())
					Chorda.LOGGER.debug(Chorda.UI, "consumed mouseDragged: "+button+","+dragX+","+dragY+" "+element);
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
			UIElement element = elements.get(i);

			if (element.isEnabled() && element.isVisible() && element.onKeyPressed(keyCode,scanCode,modifier)) {
				if(CUIDebugHelper.isDebugEnabled())
					Chorda.LOGGER.debug(Chorda.UI, "consumed keyPressed: "+keyCode+","+scanCode+","+modifier+" "+element);
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean onKeyRelease(int keyCode,int scanCode,int modifier) {

		for (int i = elements.size() - 1; i >= 0; i--) {
			UIElement element = elements.get(i);

			if (element.isEnabled() && element.isVisible() && element.onKeyRelease(keyCode,scanCode,modifier)) {
				if(CUIDebugHelper.isDebugEnabled())
					Chorda.LOGGER.debug(Chorda.UI, "consumed keyReleased: "+keyCode+","+scanCode+","+modifier+" "+element);
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
			UIElement element = elements.get(i);

			if (element.isEnabled() && element.isVisible() && element.onIMEInput(c, modifiers)) {
				if(CUIDebugHelper.isDebugEnabled())
					Chorda.LOGGER.debug(Chorda.UI, "consumed IMEInput: "+c+"(0x"+Integer.toHexString(c)+")"+","+modifiers+" "+element);
				return true;
			}
		}

		return false;
	}

	@Override
	public void onClosed() {
		for (UIElement element : elements) {
			element.onClosed();
		}
	}

	@Override
	public void tick() {

		for (UIElement element : elements) {
			if (element.isEnabled()) {
				element.tick();
			}
		}
	}

	public boolean isMouseOverAnyWidget() {
		for (UIElement element : elements) {
			if (element.isMouseOver()) {
				return true;
			}
		}

		return false;
	}
	@Override
	public Cursor getCursor() {

		for (var i = elements.size() - 1; i >= 0; i--) {
			UIElement widget = elements.get(i);
			if (widget.isEnabled() && widget.isVisible() && widget.isMouseOver()) {
				var cursor = widget.getCursor();
				if (cursor != null) {
					return cursor;
				}
			}
		}
		return null;
	}


}