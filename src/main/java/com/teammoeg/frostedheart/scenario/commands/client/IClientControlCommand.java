package com.teammoeg.frostedheart.scenario.commands.client;

import com.teammoeg.frostedheart.scenario.client.IClientScene;

public interface IClientControlCommand {

	void showTask(IClientScene runner, String q, int t);

	void showTitle(IClientScene runner, String t, String st, Integer i1, Integer i2, Integer i3);


	void speed(IClientScene runner, Double value, Integer s);

}