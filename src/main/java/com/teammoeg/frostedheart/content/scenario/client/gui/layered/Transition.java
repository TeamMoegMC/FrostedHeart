package com.teammoeg.frostedheart.content.scenario.client.gui.layered;

public enum Transition implements TransitionFunction{
	crossfade((o,n,t)->{
		o.opacity=o.opacity*(1-t);
		n.opacity=n.opacity*t;
	}),
	fadein((o,n,t)-> n.opacity=n.opacity*t),
	fadeout((o,n,t)->{
		o.opacity=o.opacity*(1-t);
		o.setForceFirst(true);
	}),
	up((o,n,t)->{
		o.y=(int) (o.y-t*o.getScreenHeight());
		n.y=(int) (n.y+(1-t)*o.getScreenHeight());
	}),
	down((o,n,t)->{
		o.y=(int) (o.y+t*o.getScreenHeight());
		n.y=(int) (n.y-(1-t)*o.getScreenHeight());
	}),
	right((o,n,t)->{
		o.x=(int) (o.x-t*o.getScreenWidth());
		n.x=(int) (n.x+(1-t)*o.getScreenWidth());
	}),
	left((o,n,t)->{
		o.x=(int) (o.x+t*o.getScreenWidth());
		n.x=(int) (n.x-(1-t)*o.getScreenWidth());
	}),
	nup((o,n,t)-> n.y=(int) (n.y+(1-t)*o.getScreenHeight())),
	ndown((o,n,t)-> n.y=(int) (n.y-(1-t)*o.getScreenHeight())),
	nright((o,n,t)-> n.x=(int) (n.x+(1-t)*o.getScreenWidth())),
	nleft((o,n,t)-> n.x=(int) (n.x-(1-t)*o.getScreenWidth())),
	overup((o,n,t)->{
		//n.setY((int) ((1-t)*n.height))
		int oldy=n.y;
		n.y=(int) ((1-t)*n.height);
		n.setContentY(oldy);
		n.height=(int) (t*n.height)+1;
		//n.setContentY((int) ((1-t)*n.height));

	}),
	overdown((o,n,t)-> n.height=(int) (t*n.height)),
	overright((o,n,t)-> n.width=(int) (t*n.width)),
	overleft((o,n,t)->{
		int oldx=n.x;
		n.x=(int) ((1-t)*n.width);
		n.setContentX(oldx);
		n.width=(int) (t*n.width)+1;
	})
	;

	final TransitionFunction func;
	private Transition(TransitionFunction func) {
		this.func = func;
	}
	@Override
	public void compute(RenderParams old, RenderParams neo, float time) {
		func.compute(old, neo, time);
	}

}
