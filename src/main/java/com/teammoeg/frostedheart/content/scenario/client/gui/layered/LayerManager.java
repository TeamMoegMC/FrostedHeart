package com.teammoeg.frostedheart.content.scenario.client.gui.layered;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import com.teammoeg.frostedheart.FHConfig;
import com.teammoeg.frostedheart.content.scenario.client.gui.layered.gl.GLImageContent;
import com.teammoeg.frostedheart.content.scenario.client.gui.layered.gl.GLLayerContent;
import com.teammoeg.frostedheart.content.scenario.client.gui.layered.gl.TypedDynamicTexture;

public class LayerManager extends GLLayerContent {
	Map<String, OrderedRenderableContent> names = new LinkedHashMap<>();
	PriorityQueue<OrderedRenderableContent> pq;
	PriorityQueue<OrderedRenderableContent> opq;
	GLImageContent oglc=new GLImageContent(null,0,0, 1f, 1f, 0, 0, 1024*FHConfig.CLIENT.getScenarioScale(), 576*FHConfig.CLIENT.getScenarioScale(), 1024*FHConfig.CLIENT.getScenarioScale(), 576*FHConfig.CLIENT.getScenarioScale());
	GLImageContent nglc=new GLImageContent(null,0,0, 1f, 1f, 0, 0, 1024*FHConfig.CLIENT.getScenarioScale(), 576*FHConfig.CLIENT.getScenarioScale(), 1024*FHConfig.CLIENT.getScenarioScale(), 576*FHConfig.CLIENT.getScenarioScale());
	private static ExecutorService executor;
	private Future <PrerenderParams> rendering;
	public static <T> Future<T> submitRenderTask(Runnable r,T result) {
		if(executor==null) {
			AtomicInteger id=new AtomicInteger(0);
			executor=Executors.newFixedThreadPool(FHConfig.CLIENT.scenarioRenderThread.get(),rn->new Thread(rn,"frostedheart-scenario-prerender-"+id.getAndAdd(1)));
		}
		return executor.submit(r,result);
	} 
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
		if(oglc.texture!=null){
			oglc.texture.close();
			oglc.texture=null;
			
		}
		if(nglc.texture!=null) {
			nglc.texture.close();
			nglc.texture=null;
		}
		for(OrderedRenderableContent ctx:names.values())
			if(ctx instanceof LayerManager) {
				((LayerManager) ctx).close();
			}
	}
	boolean prerenderRequested;
	public synchronized void commitChanges(TransitionFunction t, int ticks) {

		if (t != null) {
			this.trans = t;
			this.transTicks = 0;
			this.maxTransTicks = ticks;
			opq = pq;
			
			if(oglc.texture!=null) {
				oglc.texture.close();
				oglc.texture=null;
			}
			oglc.texture=nglc.texture;
			nglc.texture=null;
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
			final PrerenderParams prerender=new PrerenderParams();
			
			pq.forEach(s->s.prerender(prerender));				
			

			TypedDynamicTexture tex=prerender.loadTexture();

			//System.out.println("Loading tex");
			if(nglc.texture!=null) {
				nglc.texture.close();
				nglc.texture=null;
			}
			nglc.texture=tex;
		}
	}

	@Override
	public synchronized void renderContents(RenderParams params) {
		/*if(rendering!=null) {
			Future<PrerenderParams> rendering=this.rendering;
			this.rendering=null;
			PrerenderParams rendered;
			try {
				rendered = rendering.get();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new RuntimeException("scenario rendering failed");
			} catch (ExecutionException e) {
				e.getCause().printStackTrace();
				throw new RuntimeException("scenario rendering failed");
			}
			
			TypedDynamicTexture tex=rendered.loadTexture();

			//System.out.println("Loading tex");
			if(nglc.texture!=null) {
				nglc.texture.close();
				nglc.texture=null;
			}
			nglc.texture=tex;
		}*/
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
