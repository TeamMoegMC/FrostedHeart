package com.teammoeg.frostedheart.scenario.parser;

import com.teammoeg.frostedheart.scenario.runner.ScenarioRunner;

public interface Node {
    String getText();

    String getDisplay(ScenarioRunner runner);

    boolean isLiteral();

    void run(ScenarioRunner runner);
}
