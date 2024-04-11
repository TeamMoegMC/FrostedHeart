package com.teammoeg.frostedheart.content.scenario.client.gui.layered;
@FunctionalInterface
public interface TransitionFunction {
	void compute(RenderParams old,RenderParams neo,float time);
}
