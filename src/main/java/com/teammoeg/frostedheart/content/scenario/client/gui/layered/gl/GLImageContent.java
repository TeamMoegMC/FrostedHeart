package com.teammoeg.frostedheart.content.scenario.client.gui.layered.gl;

import org.lwjgl.opengl.GL11C;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.teammoeg.frostedheart.content.scenario.client.gui.layered.PrerenderParams;
import com.teammoeg.frostedheart.content.scenario.client.gui.layered.RenderParams;
import com.teammoeg.frostedheart.content.scenario.client.gui.layered.RenderableContent;
import com.teammoeg.frostedheart.util.client.ClientUtils;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.BufferUploader;
import net.minecraft.client.renderer.texture.DynamicTexture;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.resources.ResourceLocation;
import com.mojang.math.Matrix4f;

public class GLImageContent extends GLLayerContent {
	public ResourceLocation showingImage;
	public DynamicTexture texture;
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
		if(texture==null) {
			if(showingImage!=null) {
				
				ClientUtils.bindTexture(showingImage);
				blit(params.getMatrixStack(), params.getContentX(), params.getContentY(), params.getContentWidth(), params.getContentHeight(), u, v, uw, uh, tw, th, params.getOpacity());
			}
		}else {
			texture.bind();
			blit(params.getMatrixStack(), params.getContentX(), params.getContentY(), params.getContentWidth(), params.getContentHeight(), u, v, uw, uh, tw, th, params.getOpacity());
		}
	}

	public static void blit(PoseStack matrixStack, int x, int y, int width, int height, float uOffset, float vOffset, int uWidth, int vHeight, int textureWidth, int textureHeight,float opacity) {
		innerBlit(matrixStack, x, x + width, y, y + height, 0, uWidth, vHeight, uOffset, vOffset, textureWidth, textureHeight,opacity);
	}

	public static void innerBlit(PoseStack matrixStack, int x1, int x2, int y1, int y2, int blitOffset, int uWidth, int vHeight, float uOffset, float vOffset, int textureWidth, int textureHeight,
		float opacity) {
		innerBlit(matrixStack.last().pose(), x1, x2, y1, y2, blitOffset, (uOffset + 0.0F) / textureWidth, (uOffset + uWidth) / textureWidth,
			(vOffset + 0.0F) / textureHeight, (vOffset + vHeight) / textureHeight, opacity);
	}

	public static void innerBlit(Matrix4f matrix, int x1, int x2, int y1, int y2, int blitOffset, float minU, float maxU, float minV, float maxV, float opacity) {
		//RenderSystem.enableAlphaTest();
		BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
		bufferbuilder.begin(GL11C.GL_QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
		bufferbuilder.vertex(matrix, x1, y2, blitOffset).color(1, 1, 1, opacity).uv(minU, maxV).endVertex();
		bufferbuilder.vertex(matrix, x2, y2, blitOffset).color(1, 1, 1, opacity).uv(maxU, maxV).endVertex();
		bufferbuilder.vertex(matrix, x2, y1, blitOffset).color(1, 1, 1, opacity).uv(maxU, minV).endVertex();
		bufferbuilder.vertex(matrix, x1, y1, blitOffset).color(1, 1, 1, opacity).uv(minU, minV).endVertex();
		bufferbuilder.end();
		RenderSystem.enableBlend();
		RenderSystem.enableAlphaTest();
		BufferUploader.end(bufferbuilder);
		RenderSystem.disableBlend();
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
		// TODO Auto-generated method stub
		
	}

}