package com.teammoeg.frostedheart.scenario.client.gui.layered;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.PriorityQueue;

import com.teammoeg.frostedheart.scenario.client.gui.layered.gl.GLImageContent;
import com.teammoeg.frostedheart.scenario.client.gui.layered.gl.GLLayerContent;

public class LayerManager extends GLLayerContent {
	Map<String, OrderedRenderableContent> names = new LinkedHashMap<>();
	PriorityQueue<OrderedRenderableContent> pq;
	PriorityQueue<OrderedRenderableContent> opq;
	GLImageContent oglc=new GLImageContent(null,0,0, 1, 1, 0, 0, 2048, 1152, 2048, 2048);
	GLImageContent nglc=new GLImageContent(null,0,0, 1, 1, 0, 0, 2048, 1152, 2048, 2048);
	int transTicks;
	int maxTransTicks;
	TransitionFunction trans;

	public LayerManager() {
	}

	public LayerManager(Map<String, OrderedRenderableContent> names) {
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
		if(maxTransTicks>0) {
			transTicks++;

			if (transTicks >= maxTransTicks) {
				maxTransTicks = transTicks = 0;
				trans = null;
				opq = null;
			}
		}
		for (RenderableContent i : names.values()) {
			i.tick();
		}
	}

	public void addLayer(String name, OrderedRenderableContent layer) {
		names.put(name, layer);
	}

	public void freeLayer(String name) {
		OrderedRenderableContent ctx=names.remove(name);
		if(ctx instanceof LayerManager) {
			((LayerManager) ctx).close();
		}
	}

	public LayerManager getLayer(String name) {
		RenderableContent v = names.get(name);
		if (v instanceof LayerManager)
			return ((LayerManager) v);
		return new LayerManager();
	}
	public void close() {
		if(oglc.texture!=0)
			PrerenderParams.freeTexture(oglc.texture);
		if(nglc.texture!=0)
			PrerenderParams.freeTexture(nglc.texture);
		for(OrderedRenderableContent ctx:names.values())
			if(ctx instanceof LayerManager) {
				((LayerManager) ctx).close();
			}
	}
	public synchronized void commitChanges(TransitionFunction t, int ticks) {

		if (t != null) {
			this.trans = t;
			this.transTicks = 0;
			this.maxTransTicks = ticks;
			opq = pq;
			
			if(oglc.texture!=0)
				PrerenderParams.freeTexture(oglc.texture);
			oglc.texture=nglc.texture;
			nglc.texture=0;
		}else close();
		pq=null;
		if (!names.isEmpty()) {
			pq = new PriorityQueue<>(Comparator.comparingInt(OrderedRenderableContent::getZ).thenComparing(OrderedRenderableContent::getOrder));
			int i = 0;
			for (OrderedRenderableContent r : names.values()) {
				r.setOrder(i);
				pq.add(r);
				i++;
			}
			PrerenderParams prerender=new PrerenderParams();
			pq.forEach(s->s.prerender(prerender));
			nglc.texture=prerender.loadTexture();
		}
	}

	@Override
	public synchronized void renderContents(RenderParams params) {
		if (trans != null) {
			RenderParams prev = params.copyWithCurrent(this);
			RenderParams next = params.copyWithCurrent(this);
			float val = (transTicks + params.partialTicks) / maxTransTicks;
			// System.out.println(val);
			trans.compute(prev, next, val);

			if (prev.forceFirst) {
				nglc.render(next);
				if (pq != null)
					pq.forEach(t -> t.render(next));
				oglc.render(prev);
				if (opq != null)
					opq.forEach(t -> t.render(prev));
			} else {
				oglc.render(prev);
				if (opq != null)
					opq.forEach(t -> t.render(prev));
				nglc.render(next);
				if (pq != null)
					pq.forEach(t -> t.render(next));
			}
		} else {
			nglc.render(params);
			if (pq != null)
				pq.forEach(t -> t.render(params));
		}
	}
}
