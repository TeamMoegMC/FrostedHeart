package com.teammoeg.frostedheart.scenario.client.gui.layered;

import com.mojang.blaze3d.matrix.MatrixStack;

interface RenderableContent{
	void tick();
	RenderableContent copy();
	int getZ();
	int getOrder();
	void setOrder(int value);
	void render(RenderParams params);
}