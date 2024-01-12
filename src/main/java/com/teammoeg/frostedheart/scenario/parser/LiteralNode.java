package com.teammoeg.frostedheart.scenario.parser;

import com.teammoeg.frostedheart.scenario.runner.ScenarioRunner;

public class LiteralNode implements Node {
    String text;

    public LiteralNode(String text) {
        super();
        this.text = text;
    }


    @Override
    public String getText() {
        return text;
    }

    @Override
    public boolean isLiteral() {
        return true;
    }


    @Override
    public void run(ScenarioRunner runner) {
    }


    @Override
    public String getDisplay(ScenarioRunner runner) {
        return text;
    }


}
