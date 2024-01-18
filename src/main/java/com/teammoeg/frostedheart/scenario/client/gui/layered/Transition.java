package com.teammoeg.frostedheart.scenario.client.gui.layered;

public enum Transition implements TransitionFunction{
	crossfade((o,n,t)->{
		o.opacity=o.opacity*(1-t);
		n.opacity=n.opacity*t;
	}),
	fadein((o,n,t)->{
		n.opacity=n.opacity*t;
	}),
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
	nup((o,n,t)->{
		n.y=(int) (n.y+(1-t)*o.getScreenHeight());
	}),
	ndown((o,n,t)->{
		n.y=(int) (n.y-(1-t)*o.getScreenHeight());
	}),
	nright((o,n,t)->{
		n.x=(int) (n.x+(1-t)*o.getScreenWidth());
	}),
	nleft((o,n,t)->{
		n.x=(int) (n.x-(1-t)*o.getScreenWidth());
	})
	;

	TransitionFunction func;
	private Transition(TransitionFunction func) {
		this.func = func;
	}
	@Override
	public void compute(RenderParams old, RenderParams neo, float time) {
		func.compute(old, neo, time);
	}

}
