package com.teammoeg.frostedheart.scenario.client.gui.layered;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.PriorityQueue;

import com.mojang.blaze3d.matrix.MatrixStack;

public class LayerManager extends LayerContent {
	Map<String,RenderableContent> names=new LinkedHashMap<>();
	PriorityQueue<RenderableContent> pq;
	public LayerManager() {
	}
	public LayerManager(Map<String, RenderableContent> names) {
		super();
		this.names = names;
	}
	public LayerManager copy() {
		return new LayerManager(names);
	}
	public LayerManager(int x, int y, int w, int h) {
		super(x, y, w, h);
	}

	@Override
	public void tick() {
		for(RenderableContent i:names.values()) {
			i.tick();
		}
	}
	public void addLayer(String name,RenderableContent layer) {
		names.put(name, layer);
	}
	public void freeLayer(String name) {
		names.remove(name);
	}
	public LayerManager getLayer(String name) {
		RenderableContent v= names.get(name);
		if(v instanceof LayerManager)
			return ((LayerManager) v).copy();
		return new LayerManager();
	}
	public void commitChanges() {
		pq=new PriorityQueue<>(Comparator.comparingInt(RenderableContent::getZ).thenComparing(RenderableContent::getOrder));
		int i=0;
		for(RenderableContent r:names.values()) {
			r.setOrder(i);
			pq.add(r);
			i++;
		}
	}
	@Override
	public void renderContents(ImageScreenDialog screen, MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks,float opacity) {
		pq.forEach(t->t.render(screen, matrixStack, mouseX, mouseY, partialTicks, opacity));
	}

}
