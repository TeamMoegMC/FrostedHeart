package com.teammoeg.frostedheart.scenario.client.gui.layered;
@FunctionalInterface
public interface TransitionFunction {
	void compute(RenderParams old,RenderParams neo,float time);
}
