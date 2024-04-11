package com.teammoeg.frostedheart.content.scenario.client.gui.layered.font;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.CompositeContext;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

public final class ColorComposite implements Composite {
	int color;
	Composite orig;

	public ColorComposite(int color, Composite orig) {
		super();
		this.color = color;
		this.orig = orig;
		if(orig==null)
			orig=AlphaComposite.SrcOver;
	}

	public CompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints) {
		return new Context(color, orig != null ? orig.createContext(srcColorModel, dstColorModel, hints) : null,
				srcColorModel, dstColorModel);
		
	}

	static class Context implements CompositeContext {
		/*float sa;
		int sr;
		int sg;
		int sb;*/
		ColorModel src;
		ColorModel dest;
		CompositeContext comp;
		int clr;
		public Context(int color, CompositeContext compositeContext, ColorModel srcColorModel,
				ColorModel dstColorModel) {
			super();
			
			/*sa = ((color >> 24) & 0xFF) * 1F / 0xff;
			sr = (color >> 16) & 0xFF;
			sg = (color >> 8) & 0xFF;
			sb = (color) & 0xFF;*/
			//System.out.println("ctx created "+sa+","+sr+","+sg+","+sb);
			clr=color;
			src = srcColorModel;
			dest = dstColorModel;
			this.comp = compositeContext;
		}

		@Override
		public void dispose() {
		}

		@Override
		public void compose(Raster src1, Raster src2, WritableRaster dst) {
			WritableRaster src;
			src = src1.createCompatibleWritableRaster();
			src.setDataElements(0, 0, src1);
			BufferedImage srcImg = new BufferedImage(this.src, src, this.src.isAlphaPremultiplied(), null);
			for (int x = srcImg.getMinX(); x < srcImg.getMinX() + srcImg.getWidth(); x++) {
				for (int y = srcImg.getMinY(); y < srcImg.getMinY() + srcImg.getHeight(); y++) {
					int srgb = srcImg.getRGB(x, y);
					if (srgb != 0) {
						srcImg.setRGB(x, y, clr);
					}
				}

			}
			comp.compose(srcImg.getData(),src2,dst);
		}
	}

}