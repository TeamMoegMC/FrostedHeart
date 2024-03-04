package com.teammoeg.frostedheart.content.scenario.client.gui.layered;

public interface RenderableContent{
	void tick();
	RenderableContent copy();
	void render(RenderParams params);
	void prerender(PrerenderParams params);
}