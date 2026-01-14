package com.teammoeg.chorda.client.cui;

import java.util.function.Consumer;

import com.teammoeg.chorda.client.CInputHelper;
import com.teammoeg.chorda.lang.Components;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class ScrollBar extends UIElement {
	@Getter
	final boolean isVertical;
	@Getter
	private final int scrollBarSize;
	private double value = 0;
	private double step = 20;
	@Getter
	private double page=200;
	private double delta = Integer.MAX_VALUE;
	@Getter
	private double min = 0;
	@Getter
	private double max = 100;
	@Setter
	private boolean ignoreMouseOver = false;
	@Setter
	private boolean ignoreDirection = true;

	public ScrollBar(UIElement parent, boolean isVertical, int size) {
		super(parent);
		this.isVertical = isVertical;
		scrollBarSize = Math.max(size, 0);
	}

	public void setMinValue(double min) {
		this.min = min;
		setValue(getValue());
	}

	public void setMaxValue(double max) {
		this.max = max;
		setValue(getValue());
	}

	public void setScrollStep(double s) {
		step = Math.max(0D, s);
	}
	public void setPageStep(double s) {
		page = Math.max(0D, s);
	}
	@Override
	public boolean onMousePressed(MouseButton button) {
		if (isMouseOver()) {
			int scrollBarSize=getScrollBarSize();
			if(isVertical) {
				int barPos=(int) (lerpValue(height - scrollBarSize-2)+1);
				//System.out.println(getMouseY()+"/"+barPos+"("+height);
				if(getMouseY()<barPos) {
					this.setValue(this.getValue()-getPage());
				}else if(getMouseY()>barPos+scrollBarSize) {
					this.setValue(this.getValue()+getPage());
				}else
					delta =getMouseY();
			}else {
				int barPos=(int) (lerpValue(width - scrollBarSize-2)+1);
				if(getMouseX()<barPos) {
					this.setValue(this.getValue()-getPage());
				}else if(getMouseX()>barPos+scrollBarSize) {
					this.setValue(this.getValue()+getPage());
				}else
					delta=getMouseX();
			}
			return true;
		}

		return false;
	}

	@Override
	public boolean onMouseScrolled(double scroll) {
		if (scroll != 0 && isScrollDirection() && isScrollFocused()) {
			setValue(getValue() - getScrollStep() * scroll);
			return true;
		}

		return false;
	}

	@Override
	public void getTooltip(TooltipBuilder list) {
		if (showValueTooltip()) {
			Component t = getTitle();
			list.accept(Components.str(t == Component.empty() ? (Double.toString(getValue())) : (t + ": " + getValue())));
		}
	}

	public boolean showValueTooltip() {
		return false;
	}

	@Override
	public void render(GuiGraphics graphics, int x, int y, int width, int height) {
		int scrollBarSize = getScrollBarSize();

		if (scrollBarSize > 0) {
			double v = getValue();

			if (delta != Integer.MIN_VALUE) {
				if (CInputHelper.isMouseLeftDown()) {
					if (isVertical) {
						v += (getMouseY() - (delta)) * (getMax()-getMin()) / (double) (height - scrollBarSize);
						delta=getMouseY();
					} else {
						v += (getMouseX() - (delta)) * (getMax()-getMin()) / (double) (width - scrollBarSize);
						delta=getMouseX();
					}
				} else {
					delta = Integer.MIN_VALUE;
				}
			}

			setValue(v);
		}

		drawBackground(graphics, x-1, y-1, width+2, height+2);

		if (scrollBarSize > 0) {
			if (isVertical) {
				drawScrollBar(graphics, x+1, (int) (y + lerpValue(height - scrollBarSize-2)+1), width-2, scrollBarSize);
			} else {
				drawScrollBar(graphics, (int) (x + lerpValue(width - scrollBarSize-2)+1), y+1, scrollBarSize, height-2);
			}
		}
	}

	public void drawBackground(GuiGraphics graphics, int x, int y, int w, int h) {
		int i = this.isMouseOver() ? getLayerHolder().getHighlightColor() : getLayerHolder().getFrameColor();
		graphics.fill(x, y, x + w, y + h, i);
		graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, getLayerHolder().getBackgroundColor());
	}

	public void drawScrollBar(GuiGraphics graphics, int x, int y, int w, int h) {

		graphics.fill(x, y, x + w, y + h, getLayerHolder().getButtonShadowColor());
		graphics.fill(x, y, x + w - 1, y + h - 1, getLayerHolder().getButtonFaceColor());

	}

	public void onValueChanged() {
	}

	public boolean isScrollDirection() {
		return ignoreDirection || CInputHelper.isShiftKeyDown() != isVertical;
	}

	public boolean isScrollFocused() {
		return ignoreMouseOver || isMouseOver();
	}

	public void setValue(double v) {
		v = Mth.clamp(v, getMin(), getMax());

		if (value != v) {
			value = v;
			onValueChanged();
		}
	}

	public double getValue() {
		return value;
	}

	public double lerpValue(double max) {
		return Mth.clamp(getScrollRatio(), 0, 1) * max;
	}

	public float getScrollRatio() {
		return (float) (this.getValue() / (this.getMax() - this.getMin()));
	}

	public double getScrollStep() {
		return step;
	}
}