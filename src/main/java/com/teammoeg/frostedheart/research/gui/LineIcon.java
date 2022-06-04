package com.teammoeg.frostedheart.research.gui;

import com.mojang.blaze3d.matrix.MatrixStack;

import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.IconProperties;
import dev.ftb.mods.ftblibrary.icon.IconWithParent;

public class LineIcon extends IconWithParent {
	int x;
	int y;
	int h;
	int w;
	int side1;
	int side2;
	int tw = 256;
	int th = 256;
	Icon s0;
	Icon m;
	Icon s1;

	public LineIcon(Icon i, int x, int y, int w, int h, int side1, int side2, int tw, int th) {
		super(i);
		this.x = x;
		this.y = y;
		this.h = h;
		this.w = w;
		this.side1 = side1;
		this.side2 = side2;
		this.tw = tw;
		this.th = th;
		updateParts();
	}

	private Icon get(int x, int y, int w, int h) {
		return parent.withUV(this.x + x, this.y + y, w, h, tw, th);
	}

	public void updateParts() {
		s0 = get(0, 0, side1, h);
		m = get(side1, 0, w - side2 - side1, h);
		s1 = get(this.w - side2, 0, side2, h);
	}

	@Override
	public LineIcon copy() {
		LineIcon icon = new LineIcon(parent.copy(), x, y, w, h, side1, side2, tw, th);
		return icon;
	}

	@Override
	protected void setProperties(IconProperties properties) {
		super.setProperties(properties);
		x = properties.getInt("x", x);
		y = properties.getInt("y", y);
		w = properties.getInt("width", w);
		h = properties.getInt("height", h);
		side1 = properties.getInt("side1", side1);
		side2 = properties.getInt("side2", side2);
		tw = properties.getInt("texture_w", tw);
		th = properties.getInt("texture_h", th);

		String s = properties.getString("pos", "");

		if (!s.isEmpty()) {
			String[] s1 = s.split(",", 4);

			if (s1.length == 4) {
				x = Integer.parseInt(s1[0]);
				y = Integer.parseInt(s1[1]);
				w = Integer.parseInt(s1[2]);
				h = Integer.parseInt(s1[3]);
			}
		}

		updateParts();
	}

	@Override
	public void draw(MatrixStack matrixStack, int x, int y, int w, int h) {
		int msize = w - side2 - side1;
		if (msize <= 0) {
			s0.draw(matrixStack, x, y, side1, h);
			s1.draw(matrixStack, x + side1, y, side2, h);
		} else {
			m.draw(matrixStack, x + side1, y, msize, h);
			s0.draw(matrixStack, x, y, side1, h);
			s1.draw(matrixStack, x + w - side2, y, side2, h);
		}
	}

}
