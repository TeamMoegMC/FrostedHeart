package com.teammoeg.chorda.client.cui;


public class PanelScrollBar extends ScrollBar {
	private final Layer panel;

	public PanelScrollBar(Layer parent, boolean isVertical, Layer affected) {
		super(parent, isVertical, 0);
		panel = affected;
	}

	public PanelScrollBar(Layer parent, Layer affected) {
		this(parent, true, affected);
	}

	public Layer getAffectedLayer() {
		return panel;
	}

	@Override
	public double getMin() {
		return 0;
	}

	@Override
	public void setMaxValue(double max) {
		throw new UnsupportedOperationException();
	}
	@Override
	public void setMinValue(double min) {
		if(min!=0)
		throw new UnsupportedOperationException();
	}
	@Override
	public double getMax() {
		return isVertical ? panel.getContentHeight() - panel.getHeight() : panel.getContentWidth() - panel.getWidth();
	}

	@Override
	public void setScrollStep(double s) {
		panel.setScrollStep(s);
	}

	@Override
	public double getScrollStep() {
		return panel.getScrollStep();
	}

	@Override
	public int getScrollBarSize() {
		var max = getMax();
		if (max <= 0) {
			return 0;
		}

		int size = isVertical ?
				(int) (panel.getHeight() / (max + panel.getHeight()) * getHeight()) :
				(int) (panel.getWidth() / (max + panel.getWidth()) * getWidth());

        return Math.max(size, 10);
	}

	@Override
	public void onValueChanged() {
		var value = getMax() <= 0 ? 0 : getValue();

		if (isVertical) {
			getAffectedLayer().setOffsetY((int) value);
		} else {
			getAffectedLayer().setOffsetX((int) value);
		}
	}

	@Override
	public boolean isScrollFocused() {
		return super.isScrollFocused() || panel.isMouseOver();
	}

	@Override
	public boolean isVisible() {
		return getScrollBarSize() > 0;
	}

	@Override
	public boolean isEnabled() {
		return getScrollBarSize() > 0;
	}
}