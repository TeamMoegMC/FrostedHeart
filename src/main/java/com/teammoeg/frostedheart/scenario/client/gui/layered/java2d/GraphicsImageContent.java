package com.teammoeg.frostedheart.scenario.client.gui.layered.java2d;

import java.awt.AlphaComposite;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.teammoeg.frostedheart.scenario.client.gui.layered.PrerenderParams;
import com.teammoeg.frostedheart.scenario.client.gui.layered.RenderableContent;
import com.teammoeg.frostedheart.util.client.Rect;

import net.minecraft.client.Minecraft;
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
	public GraphicsImageContent(ResourceLocation showingImage,Rect r1,Rect r2) {
		this(showingImage,r1.getX(), r1.getY(), r1.getW(), r1.getH());
		ix=r2.getX();
		iy=r2.getY();
		iw=r2.getW();
		ih=r2.getH();
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
				Rect r=params.calculateRect(x, y, width, height);
				params.getG2d().setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
				//System.out.println(r);
				params.getG2d().drawImage(image,r.getX(),r.getY(),r.getW()+r.getX(),r.getH()+r.getY(), ix, iy, iw+ix, ih+iy, null);
				params.getG2d().setComposite(AlphaComposite.SrcOver);
				
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println(showingImage+" load error");
			}
		}else {
			System.out.println(showingImage+" not found");
		}

	}

}
