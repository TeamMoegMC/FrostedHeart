package com.teammoeg.frostedheart.content.scenario.commands.client;

import com.teammoeg.chorda.client.ui.Point;
import com.teammoeg.chorda.client.ui.Rect;
import com.teammoeg.frostedheart.content.scenario.client.IClientScene;

import dev.ftb.mods.ftblibrary.icon.Color4I;

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




	void TextLayer(IClientScene runner, String name, String text, Rect rect, int z, Float opacity, int shadow, int resize);


	void ImageLayer(IClientScene runner, String name, String path, Rect drect, Rect srect, int z, Float opacity);

	void FillRect(IClientScene runner, String name, Rect rect, int z, Color4I color);

	void DrawLine(IClientScene runner, String name, Point start, Point end, int w, int z, Color4I color);

	void hudDialog(IClientScene runner, Integer show);



}