package com.teammoeg.frostedheart.scenario.client.gui.layered.java2d;

import java.awt.AlphaComposite;
import com.teammoeg.frostedheart.scenario.client.gui.layered.PrerenderParams;
import com.teammoeg.frostedheart.scenario.client.gui.layered.RenderableContent;
import com.teammoeg.frostedheart.scenario.client.gui.layered.font.GraphicGlyphRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.RenderComponentsUtil;
import net.minecraft.util.text.ITextComponent;

public class GraphicsTextContent extends GraphicLayerContent {
	public int size;
	public boolean shadow;
	ITextComponent text;
	public GraphicsTextContent() {
	}

	public GraphicsTextContent(ITextComponent text,int x, int y, int w, int h,boolean shadow) {
		super(x, y, w, h);
		this.text=text;
		this.shadow=shadow;
	}

	public GraphicsTextContent(int x, int y, int width, int height, int z, float opacity, int size, boolean shadow,
			ITextComponent text) {
		super(x, y, width, height, z, opacity);
		this.size = size;
		this.shadow = shadow;
		this.text = text;
	}

	@Override
	public void tick() {
	}

	@Override
	public RenderableContent copy() {
		return new GraphicsTextContent(x,y,width,height,z,opacity,size,shadow,text);
	}

	@Override
	public void prerender(PrerenderParams params) {
		//params.getG2d().setClip(x, y, width, height);
		params.getG2d().setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
		RenderComponentsUtil.func_238505_a_(text,(int) (width/(size/7f)), Minecraft.getInstance().fontRenderer).get(0).accept(new GraphicGlyphRenderer(params.getG2d(),x,y,size,shadow));
		params.getG2d().setComposite(AlphaComposite.SrcOver);
		//params.getG2d().setClip(0, 0, params.getWidth(),params.getHeight());
	}

}
