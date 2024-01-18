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
	PriorityQueue<RenderableContent> opq;
	int transTicks;
	int maxTransTicks;
	TransitionFunction trans;
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
	public void setTrans(TransitionFunction t,int ticks) {
		this.trans=t;
		this.transTicks=0;
		this.maxTransTicks=ticks;
	}
	@Override
	public void tick() {
		transTicks++;
		System.out.println(transTicks+"/"+maxTransTicks);
		if(transTicks>=maxTransTicks) {
			maxTransTicks=transTicks=0;
			trans=null;
			opq=null;
		}
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
			return ((LayerManager) v);
		return new LayerManager();
	}
	public void commitChanges() {
		if(trans!=null)
			opq=pq;
		if(!names.isEmpty()) {
			pq=new PriorityQueue<>(Comparator.comparingInt(RenderableContent::getZ).thenComparing(RenderableContent::getOrder));
			int i=0;
			for(RenderableContent r:names.values()) {
				r.setOrder(i);
				pq.add(r);
				i++;
			}
		}
	}
	@Override
	public void renderContents(RenderParams params) {
		if(trans!=null) {
			RenderParams prev=params.copyWithCurrent(this);
			RenderParams next=params.copyWithCurrent(this);
			float val=(transTicks+params.partialTicks)/maxTransTicks;
			System.out.println(val);
			trans.compute(prev, next, val);
			if(prev.forceFirst) {
				if(pq!=null)
					pq.forEach(t->t.render(next));
				if(opq!=null)
					opq.forEach(t->t.render(prev));
			}else {
				if(opq!=null)
					opq.forEach(t->t.render(prev));
				if(pq!=null)
					pq.forEach(t->t.render(next));
			}
		}else {
			if(pq!=null)
				pq.forEach(t->t.render(params));
		}
	}

}
