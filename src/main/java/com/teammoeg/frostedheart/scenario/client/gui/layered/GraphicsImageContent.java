package com.teammoeg.frostedheart.scenario.client.gui.layered;

import java.awt.AlphaComposite;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.scenario.client.gui.layered.text.GraphicGlyphRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.RenderComponentsUtil;
import net.minecraft.util.ResourceLocation;

public class GraphicsImageContent extends GraphicLayerContent {
	public ResourceLocation showingImage;
	public int ix=0,iy=0,iw=-1,ih=-1;
	public GraphicsImageContent() {
	}

	public GraphicsImageContent(ResourceLocation showingImage,int x, int y, int w, int h) {
		super(x, y, w, h);
		this.showingImage=showingImage;
	}

	public GraphicsImageContent(ResourceLocation showingImage,int x, int y, int width, int height, int z,float opacity) {
		super(x, y, width, height, z);
		this.showingImage=showingImage;
		this.opacity=opacity;
	}


	@Override
	public void tick() {
	}

	@Override
	public RenderableContent copy() {
		return new GraphicsImageContent(showingImage,x,y,width,height,z,opacity);
	}

	@Override
	public void prerender(PrerenderParams params) {
		BufferedImage image;
		if(Minecraft.getInstance().getResourceManager().hasResource(showingImage)) {
			try {

				image = ImageIO.read(Minecraft.getInstance().getResourceManager().getResource(showingImage).getInputStream());
				if(iw<0)
					iw=image.getWidth();
				if(ih<0)
					ih=image.getHeight();
				//params.getG2d().setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
				//params.getG2d().drawImage(image, x, y, x+width, y+height, ix, iy, iw, ih, null);
				//params.getG2d().setComposite(AlphaComposite.SrcOver);
				RenderComponentsUtil.func_238505_a_(GuiUtils.str("test测试1234"), 1000, Minecraft.getInstance().fontRenderer)
				.get(0).accept(new GraphicGlyphRenderer(10, params.getG2d(), 10));
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println(showingImage+" load error");
			}
		}else {
			System.out.println(showingImage+" not found");
		}

	}

}
