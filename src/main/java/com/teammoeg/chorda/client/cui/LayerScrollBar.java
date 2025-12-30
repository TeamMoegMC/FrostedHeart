package com.teammoeg.chorda.client.cui;


public class LayerScrollBar extends ScrollBar {
	private final Layer layer;

	public LayerScrollBar(UIWidget parent, boolean isVertical, Layer affected) {
		super(parent, isVertical, 0);
		layer = affected;
		affected.setSmoothScrollEnabled(true);
	}

	public LayerScrollBar(Layer parent, Layer affected) {
		this(parent, true, affected);
	}

	public Layer getAffectedLayer() {
		return layer;
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
	public double getPage() {
		return isVertical ? layer.getHeight() :  layer.getWidth();
	}

	@Override
	public void setPageStep(double s) {
		throw new UnsupportedOperationException();
	}

	@Override
	public double getMax() {
		return isVertical ? layer.getContentHeight() - layer.getHeight() : layer.getContentWidth() - layer.getWidth();
	}

	@Override
	public void setScrollStep(double s) {
		layer.setScrollStep(s);
	}

	@Override
	public double getScrollStep() {
		return layer.getScrollStep();
	}

	@Override
	public int getScrollBarSize() {
		var max = getMax();
		if (max <= 0) {
			return 0;
		}

		int size = isVertical ?
				(int) (layer.getHeight() / (max + layer.getHeight()) * getHeight()) :
				(int) (layer.getWidth() / (max + layer.getWidth()) * getWidth());

        return Math.max(size, 10);
	}

	@Override
	public void onValueChanged() {
		var value = getMax() <= 0 ? 0 : getValue();

		if (isVertical) {
			getAffectedLayer().setOffsetY(-(int) value);
		} else {
			getAffectedLayer().setOffsetX(-(int) value);
		}
	}

	@Override
	public boolean isScrollFocused() {
		return super.isScrollFocused() || layer.isMouseOver();
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