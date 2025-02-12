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

package com.teammoeg.frostedheart.content.scenario.client.gui.layered;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import com.teammoeg.frostedheart.content.scenario.client.ClientScene;
import com.teammoeg.frostedheart.content.scenario.client.gui.layered.gl.GLImageContent;
import com.teammoeg.frostedheart.content.scenario.client.gui.layered.gl.GLLayerContent;
import com.teammoeg.frostedheart.content.scenario.client.gui.layered.gl.TypedDynamicTexture;

public class LayerManager extends GLLayerContent {
	private static record RerenderRequest(TransitionFunction trans,int ticks){}
	private static record LayerContext(PriorityQueue<OrderedRenderableContent> pq,GLImageContent glc) {
		public void render(RenderParams params) {
			glc.render(params);
			if (pq != null)
				for(OrderedRenderableContent i:pq) {
					i.render(params);
				}
		}

		public void close() {
			glc.texture.release();
		}
		
	}
	private static class TransitionInfo{
		int transTicks=0;
		int maxTransTicks;
		TransitionFunction trans;
		public TransitionInfo(int maxTransTicks, TransitionFunction trans) {
			super();
			this.maxTransTicks = maxTransTicks;
			this.trans = trans;
		}
	}
	Map<String, OrderedRenderableContent> names = new LinkedHashMap<>();
	volatile LayerContext nextlayer;
	volatile LayerContext current;
	volatile TransitionInfo trans;
	private static final AtomicInteger THREAD_NUM=new AtomicInteger(0);
	static ExecutorService renderThread=Executors.newFixedThreadPool(FHConfig.CLIENT.scenarioRenderThread.get(),r->{
		Thread th=new Thread(r);
		th.setDaemon(true);//if the game exits, we have no need to render anymore
		th.setName("scenario-render-pool-"+THREAD_NUM.incrementAndGet());
		return th;
	});
	RerenderRequest rrq;
	
	public LayerManager() {
	}
	public LayerContext createContext() {
		return new LayerContext(new PriorityQueue<>(Comparator.comparingInt(OrderedRenderableContent::getZ).thenComparing(OrderedRenderableContent::getOrder)),new GLImageContent(null,0,0, 1f, 1f, 0, 0, 1024*FHConfig.CLIENT.getScenarioScale(), 576*FHConfig.CLIENT.getScenarioScale(), 1024*FHConfig.CLIENT.getScenarioScale(), 576*FHConfig.CLIENT.getScenarioScale()));
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
		if(trans!=null&&trans.maxTransTicks>0) {
			trans.transTicks++;

			if (trans.transTicks >= trans.maxTransTicks) {
				LayerContext ol=current;
				current=nextlayer;
				trans = null;
				nextlayer=null;
				if(ol!=null)
				ol.close();
				ClientScene.INSTANCE.onTransitionComplete.setFinished();
				
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
		if(nextlayer!=null){
			nextlayer.close();
			nextlayer=null;
		}
		if(current!=null) {
			current.close();
			current=null;
		}
		for(OrderedRenderableContent ctx:names.values())
			if(ctx instanceof LayerManager) {
				((LayerManager) ctx).close();
			}
	}
	boolean prerenderRequested;
	public void commitChanges(TransitionFunction t, int ticks) {
		//rrq=new RerenderRequest(t,ticks);
		ClientScene.INSTANCE.onRenderComplete.resetFinished();
		ClientScene.INSTANCE.onTransitionComplete.resetFinished();
		renderThread.submit(()->renderPrerendered(t,ticks));
		
	}
	public void renderPrerendered(TransitionFunction t, int ticks) {
		LayerContext inext=this.createContext();
		if (!names.isEmpty()) {
			int i = 0;
			for (OrderedRenderableContent r : names.values()) {
				r.setOrder(i);
				inext.pq.add(r);
				i++;
			}
			final PrerenderParams prerender=new PrerenderParams();
			inext.pq.forEach(s->s.prerender(prerender));				
			inext.glc.texture=prerender.loadTexture();

		}
		if (t != null) {
			this.nextlayer=inext;
			this.trans = new TransitionInfo(ticks,t);
		}else {
			LayerContext otex=current;
			current=inext;
			if(otex!=null)
				otex.close();
			ClientScene.INSTANCE.onTransitionComplete.setFinished();
		}
		ClientScene.INSTANCE.onRenderComplete.setFinished();
	}

	@Override
	public void renderContents(RenderParams params) {
		/*if(rrq!=null)
			synchronized(this) {
				if(rrq!=null) {
					RerenderRequest trrq=rrq;
					rrq=null;
					renderPrerendered(trrq.trans(),trrq.ticks());
				}
			}
		*/
		if (trans != null) {
			RenderParams prev = params.copyWithCurrent(this);
			RenderParams next = params.copyWithCurrent(this);
			float val = (trans.transTicks)*1f / trans.maxTransTicks;
			// System.out.println(val);
			trans.trans.compute(prev, next, val);

			if (prev.forceFirst) {
				if(nextlayer!=null)
					nextlayer.render(next);
				if(current!=null)
					current.render(prev);
			} else {
				if(current!=null)
					current.render(prev);
				if(nextlayer!=null)
					nextlayer.render(next);
			}
		} else {
			if(current!=null)
				current.render(params);
		}
	}
}
