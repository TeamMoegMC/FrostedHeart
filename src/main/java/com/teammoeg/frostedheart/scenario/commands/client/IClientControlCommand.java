package com.teammoeg.frostedheart.scenario.commands.client;

import com.teammoeg.frostedheart.scenario.client.IClientScene;

public interface IClientControlCommand {

	void showTask(IClientScene runner, String q, int t);

	void showTitle(IClientScene runner, String t, String st, Integer i1, Integer i2, Integer i3);


	void fullScreenDialog(IClientScene runner, Integer show, Integer x, Integer y, Integer w, Integer m);

	void TextLayer(IClientScene runner, String name, String text, int x, int y, Integer w, Integer h, int z, Integer opacity, int shadow);

	void ImageLayer(IClientScene runner, String name, String path, int x, int y, Integer w, Integer h, int u, int v, int uw, int uh, int tw, int th, int z, Integer opacity);

	void showLayer(IClientScene runner, String name);

	void startLayer(IClientScene runner, String name);

	void freeLayer(IClientScene runner, String name);

	void bgm(IClientScene runner, String name);

	void stopbgm(IClientScene runner);

	void sound(IClientScene runner, String name, int rep);

	void stopAllsounds(IClientScene runner);

	void speed(IClientScene runner, double value, Integer s);


}