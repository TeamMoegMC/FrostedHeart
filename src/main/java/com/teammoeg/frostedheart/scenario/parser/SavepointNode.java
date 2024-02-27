package com.teammoeg.frostedheart.scenario.parser;

import java.util.Map;

import com.teammoeg.frostedheart.scenario.runner.ScenarioVM;

public class SavepointNode extends ParagraphNode {
	CommandNode cmds;
	public SavepointNode(String command, Map<String, String> params) {
		super(command, params);
		cmds=new CommandNode(command,params);
	}
    @Override
    public String getText() {
        return cmds.getText();
    }
    @Override
    public void run(ScenarioVM runner) {
    	cmds.run(runner);
    	runner.paragraph(nodeNum);
    }
}
