/*
 * Copyright (c) 2024 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.content.scenario.commands.client;

import com.teammoeg.chorda.client.ui.Point;
import com.teammoeg.chorda.client.ui.Rect;
import com.teammoeg.frostedheart.content.scenario.client.IClientScene;

public interface IClientControlCommand {

	void showTask(IClientScene runner, String q, int t);

	void showTitle(IClientScene runner, String t, String st, Integer i1, Integer i2, Integer i3);




	void startLayer(IClientScene runner, String name);

	void freeLayer(IClientScene runner, String name);

	void bgm(IClientScene runner, String name);

	void stopbgm(IClientScene runner);

	void sound(IClientScene runner, String name, int rep);




	void fullScreenDialog(IClientScene runner, Integer show, Float x, Float y, Float w, Integer m);

	void stopAllsounds(IClientScene runner);

	void speed(IClientScene runner, double value, Integer s);




	void TextLayer(IClientScene runner, String name, String text, Rect rect, int z, Float opacity, int shadow, int resize);


	void ImageLayer(IClientScene runner, String name, String path, Rect drect, Rect srect, int z, Float opacity);

	void FillRect(IClientScene runner, String name, Rect rect, int z, int color);

	void DrawLine(IClientScene runner, String name, Point start, Point end, int w, int z, int color);

	void hudDialog(IClientScene runner, Integer show);


	void textElement(IClientScene runner, String name, String text, float x, float y, float w, float h, int z, Float opacity, int shadow, int resize);

	void showLayer(IClientScene runner, String name, String transition, int time, float x, float y, Float w, Float h,
			boolean cached);




}