package com.teammoeg.frostedheart.scenario.client;

import com.teammoeg.frostedheart.scenario.runner.RunStatus;

import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;

public interface IClientScene {

	void showOneChar();

	void reset();

	void sendContinuePacket(boolean isSkip);

	boolean hasNext();

	boolean isTick();

	void cls();

	void setText(String txt);

	void processClient(ITextComponent item, boolean isReline, boolean isNowait);

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