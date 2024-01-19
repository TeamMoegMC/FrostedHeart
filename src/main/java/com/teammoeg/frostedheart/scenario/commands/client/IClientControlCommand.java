package com.teammoeg.frostedheart.scenario.commands.client;

import com.teammoeg.frostedheart.scenario.client.IClientScene;

public interface IClientControlCommand {

	void showTask(IClientScene runner, String q, int t);

	void showTitle(IClientScene runner, String t, String st, Integer i1, Integer i2, Integer i3);




	void startLayer(IClientScene runner, String name);

	void freeLayer(IClientScene runner, String name);

	void bgm(IClientScene runner, String name);

	void stopbgm(IClientScene runner);

	void sound(IClientScene runner, String name, int rep);



	void showLayer(IClientScene runner, String name, String transition, int time, float x, float y, Float w, Float h);

	void fullScreenDialog(IClientScene runner, Integer show, Float x, Float y, Float w, Integer m);

	void stopAllsounds(IClientScene runner);

	void speed(IClientScene runner, double value, Integer s);




	void TextLayer(IClientScene runner, String name, String text, float x, float y, Float w, Float h, int z,
			Float opacity, int shadow, float resize, int cv, int ch,Integer color);


	void ImageLayer(IClientScene runner, String name, String path, float x, float y, Float w, Float h, int u, int v, Integer uw, Integer uh, int z, Float opacity);



}