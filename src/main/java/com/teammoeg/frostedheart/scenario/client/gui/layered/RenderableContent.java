package com.teammoeg.frostedheart.scenario.client.gui.layered;

public interface RenderableContent{
	void tick();
	RenderableContent copy();
	void render(RenderParams params);
	void prerender(PrerenderParams params);
}