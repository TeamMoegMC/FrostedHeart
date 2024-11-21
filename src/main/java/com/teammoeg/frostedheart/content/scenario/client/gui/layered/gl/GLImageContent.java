package com.teammoeg.frostedheart.content.scenario.client.gui.layered.gl;

import com.teammoeg.frostedheart.content.scenario.client.gui.layered.PrerenderParams;
import com.teammoeg.frostedheart.content.scenario.client.gui.layered.RenderParams;
import com.teammoeg.frostedheart.content.scenario.client.gui.layered.RenderableContent;
import com.teammoeg.frostedheart.util.client.FHGuiHelper;
import net.minecraft.resources.ResourceLocation;

public class GLImageContent extends GLLayerContent {
	public ResourceLocation showingImage;
	public TypedDynamicTexture texture;
	int u, v, uw, uh, tw, th;

	public GLImageContent(float x, float y, float width, float height, int z, ResourceLocation showingImage, int u, int v, int uw, int uh, int tw, int th) {
		super(x, y, width, height, z);
		this.showingImage = showingImage;
		this.u = u;
		this.v = v;
		this.uw = uw;
		this.uh = uh;
		this.tw = tw;
		this.th = th;
	}

	@Override
	public RenderableContent copy() {
		return new GLImageContent(x, y, width, height, z, showingImage, u, v, uw, uh, tw, th);
	}

	@Override
	public void renderContents(RenderParams params) {
		//RenderSystem.colorMask(false, false, false, false);
		System.out.println("computed width:"+params.getContentWidth());
		System.out.println("total width:"+params.getScreenWidth());
		if(texture==null) {
			if(showingImage!=null) {
				
				FHGuiHelper.bindTexture(showingImage);
				FHGuiHelper.blit(params.getMatrixStack(), params.getContentX(), params.getContentY(), params.getContentWidth(), params.getContentHeight(), u, v, uw, uh, tw, th, params.getOpacity());
			}
		}else {
			texture.draw(params.getGuiGraphics(), params.getContentX(), params.getContentY(), params.getContentWidth(), params.getContentHeight(), u, v, uw, uh, params.getOpacity());

			//texture.bind();
			//FHGuiHelper.blit(params.getMatrixStack(), params.getContentX(), params.getContentY(), params.getContentWidth(), params.getContentHeight(), u, v, uw, uh, tw, th, params.getOpacity());
		}
	}


	public GLImageContent(ResourceLocation showingImage, float x, float y, float w, float h, int u, int v, int uw, int uh, int tw, int th) {
		super(x, y, w, h);
		this.showingImage = showingImage;
		this.u = u;
		this.v = v;
		this.uw = uw;
		this.uh = uh;
		this.tw = tw;
		this.th = th;
	}

	public GLImageContent(ResourceLocation showingImage, int uw, int uh, int tw, int th) {
		super(0, 0, -1, -1);
		this.showingImage = showingImage;
		this.u = 0;
		this.v = 0;
		this.uw = uw;
		this.uh = uh;
		this.tw = tw;
		this.th = th;
	}

	@Override
	public void tick() {
	}

	@Override
	public void prerender(PrerenderParams params) {

	}

}