package com.teammoeg.chorda.client.cui.theme;

import com.teammoeg.chorda.client.ClientUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Optional;

public interface Theme {

	void drawButton(GuiGraphics graphics, int x, int y, int w, int h, boolean isHighlight, boolean enabled);

	void drawSliderBackground(GuiGraphics graphics, int x, int y, int w, int h, boolean isHighlight);

	void drawTextboxBackground(GuiGraphics graphics, int x, int y, int w, int h, boolean focused);

	void drawSliderBar(GuiGraphics graphics, int x, int y, int w, int h, boolean isHighlight);

	void drawPanel(GuiGraphics graphics, int x, int y, int w, int h);

	void drawSlot(GuiGraphics graphics, int x, int y, int w, int h);

	void drawUIBackground(GuiGraphics graphics, int x, int y, int w, int h);

	default void drawTooltip(GuiGraphics graphics, List<Component> tooltipLines, int mouseX, int mouseY, int zOffset) {
		graphics.pose().pushPose();
		graphics.pose().translate(0, 0, zOffset);
		graphics.setColor(1f, 1f, 1f, 0.8f);
		graphics.renderTooltip(ClientUtils.getMc().font, tooltipLines, Optional.empty(), mouseX, Math.max(mouseY, 18));
		graphics.setColor(1f, 1f, 1f, 1f);
		graphics.pose().popPose();
	}

	/**
     * UI 主文本色调
     */
	int UITextColor();
	/**
     * UI 次要文本色调，用于和主文本区分，一般会更淡
     */
	int UIAltTextColor();
	int UIBGColor();
	int UIBGBorderColor();
	int buttonTextColor();
	int buttonTextOverColor();
	int buttonTextDisabledColor();
	int errorColor();
	int successColor();

	boolean isUITextShadow();

	boolean isButtonTextShadow();
}