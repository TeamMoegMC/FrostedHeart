package com.teammoeg.frostedheart.content.scenario.parser;

import java.util.Map;

import com.teammoeg.frostedheart.content.scenario.runner.ScenarioCommandContext;

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
    public void run(ScenarioCommandContext runner) {
    	cmds.run(runner);
    	super.run(runner);
    }
}
