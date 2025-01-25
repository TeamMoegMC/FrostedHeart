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

package com.teammoeg.frostedheart.content.scenario.client;

import com.teammoeg.frostedheart.content.scenario.runner.RunStatus;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

public interface IClientScene {

	void showOneChar();

	void reset();

	void sendContinuePacket(boolean isSkip);

	boolean hasNext();

	boolean isTick();

	void cls();

	void setText(String txt);

	void processClient(Component item, boolean isReline, boolean isNowait);

	void process(String text, boolean isReline, boolean isNowait, boolean resetScene, RunStatus status);

	void setActHud(String title, String subtitle);

	void setPreset(Style style);

	Style getPreset();

	boolean isShowWordMode();

	void setShowWordMode(boolean showWordMode);

	void setTicksBetweenShow(int ticksBetweenShow);

	int getTicksBetweenShow();

	void setCharsPerShow(int charsPerShow);

	int getCharsPerShow();

	void setSpeed(double value);

}