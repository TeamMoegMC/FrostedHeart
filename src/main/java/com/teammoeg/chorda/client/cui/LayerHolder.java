package com.teammoeg.chorda.client.cui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
/**
 * Modifies vanilla color palette
 * Interface method for minecraft screen
 * */
public interface LayerHolder {
	void focusOn(UIElement elm);
	Font getFont();
	int getErrorColor();
	int getHighlightColor();
	int getFontColor();
	int getBackgroundColor();
	int getFrameColor();
	int getButtonFaceColor();
	int getButtonShadowColor();
	void refreshWidgets();
	/**
	 * @return if the GUI should render a blur effect behind it
	 */
	boolean shouldRenderGradient();
	boolean onCloseQuery();
	Screen getPrevScreen();
	boolean isPauseScreen();
	void closeGui(boolean openPrevScreen);
	void updateGui(double mx, double my, float pt);
}
