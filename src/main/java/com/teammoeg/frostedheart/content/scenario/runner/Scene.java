package com.teammoeg.frostedheart.content.scenario.runner;

public interface Scene {

	void sendTitles(ScenarioThread thread, String title, String subtitle);

	void sendScene(ScenarioThread thread, String text, RunStatus status, boolean wrap, boolean reset, boolean isNowait, boolean noDelay);

}