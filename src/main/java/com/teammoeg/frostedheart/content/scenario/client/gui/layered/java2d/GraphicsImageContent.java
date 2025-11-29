/*
 * Copyright (c) 2024 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.content.scenario.client.gui.layered.java2d;

import java.awt.AlphaComposite;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import javax.imageio.ImageIO;

import com.teammoeg.chorda.client.ui.Rect;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.scenario.client.gui.layered.PrerenderParams;
import com.teammoeg.frostedheart.content.scenario.client.gui.layered.RenderableContent;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.Unit;

public class GraphicsImageContent extends GraphicLayerContent {
	public ResourceLocation showingImage;
	public int ix = 0, iy = 0, iw = -1, ih = -1;
	private BufferedImage cachedImage;

	public GraphicsImageContent() {
	}

	public GraphicsImageContent(ResourceLocation showingImage, int x, int y, int w, int h) {
		super(x, y, w, h);
		this.showingImage = showingImage;
	}

	public GraphicsImageContent(ResourceLocation showingImage, Rect r1, Rect r2) {
		this(showingImage, r1.getX(), r1.getY(), r1.getW(), r1.getH());
		ix = r2.getX();
		iy = r2.getY();
		iw = r2.getW();
		ih = r2.getH();
	}

	public GraphicsImageContent(ResourceLocation showingImage, int x, int y, int width, int height, int z, float opacity) {
		super(x, y, width, height, z);
		this.showingImage = showingImage;
		this.opacity = opacity;
	}

	@Override
	public void tick() {
	}

	@Override
	public CompletableFuture<Void> prepare(ExecutorService threadPool) {
		return CompletableFuture.runAsync(() -> {
			try {
				
				Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(showingImage);
				if (resource.isPresent()) {
					try (
						InputStream stream = resource.get().open()) {
						BufferedImage image = ImageIO.read(stream);
						if (iw < 0)
							iw = image.getWidth();
						if (ih < 0)
							ih = image.getHeight();
						this.cachedImage = image;
					} catch (IOException e) {
						this.cachedImage=null;
						FHMain.LOGGER.fatal(showingImage + " load error",e);
					}
				} else {
					this.cachedImage=null;
					FHMain.LOGGER.error(showingImage + " not found");
				}
			}catch(Throwable t) {
				this.cachedImage=null;
				FHMain.LOGGER.error("Exception when loading scenario image "+showingImage,t);
			}
		}, threadPool);
	}

	@Override
	public RenderableContent copy() {
		return new GraphicsImageContent(showingImage, x, y, width, height, z, opacity);
	}

	@Override
	public void prerender(PrerenderParams params) {
		if(cachedImage!=null) {
			Rect r = params.calculateRect(x, y, width, height);
			params.getG2d().setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
			// System.out.println(r);
			params.getG2d().drawImage(cachedImage, r.getX(), r.getY(), r.getW() + r.getX(), r.getH() + r.getY(), ix, iy, iw + ix, ih + iy, null);
			params.getG2d().setComposite(AlphaComposite.SrcOver);
		}

	}

}
