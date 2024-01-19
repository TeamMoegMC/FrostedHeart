package com.teammoeg.frostedheart.scenario.client.gui.layered;

import com.mojang.blaze3d.matrix.MatrixStack;

interface RenderableContent{
	void tick();
	RenderableContent copy();
	void render(RenderParams params);
	void prerender(PrerenderParams params);
}