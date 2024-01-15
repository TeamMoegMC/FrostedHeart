package com.teammoeg.frostedheart.scenario.commands;

import com.teammoeg.frostedheart.scenario.Param;
import com.teammoeg.frostedheart.scenario.runner.ActNamespace;
import com.teammoeg.frostedheart.scenario.runner.ScenarioConductor;
import com.teammoeg.frostedheart.scenario.runner.target.ActTarget;
import com.teammoeg.frostedheart.scenario.runner.target.ExecuteTarget;

public class SceneCommand {
	public void startAct(ScenarioConductor runner,@Param("c")String c,@Param("a")String a) {
		runner.endAct();
		runner.jump(new ActTarget(new ActNamespace(c,a),runner.getCurrentAct().getCurrentPosition().next()));
	}
	public void endAct(ScenarioConductor runner) {
		runner.endAct();
	}
	public void queueAct(ScenarioConductor runner,@Param("s")String s,@Param("l")String l,@Param("c")String c,@Param("a")String a) {
		runner.queue(new ActTarget(new ActNamespace(c,a),new ExecuteTarget(s,l)));
	}
	public void actTitle(ScenarioConductor runner,@Param("t")String t,@Param("st")String st) {
		if(t!=null)
			runner.getCurrentAct().title=t;
		if(st!=null)
			runner.getCurrentAct().subtitle=st;
	}
}
