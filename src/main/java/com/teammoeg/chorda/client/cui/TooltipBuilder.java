package com.teammoeg.chorda.client.cui;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.lang.Components;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class TooltipBuilder implements Consumer<Component> {
	private List<Component> tooltip = new ArrayList<>();
	private int zOffset = 600;

	public TooltipBuilder(int initZ) {
		zOffset=initZ;
	}

	@Override
	public void accept(Component t) {
		tooltip.add(t);
	}
	public TooltipBuilder add(Component t) {
		tooltip.add(t);
		return this;
	}
	public TooltipBuilder addString(String str) {
		tooltip.add(Components.str(str));
		return this;
	}
	public TooltipBuilder addTranslation(String str) {
		tooltip.add(Components.translatable(str));
		return this;
	}
	public TooltipBuilder translateZ(int value) {
		zOffset+=value;
		return this;
	}
	public void draw(GuiGraphics graphics, int mouseX, int mouseY) {
		if (!tooltip.isEmpty()) {
			graphics.pose().translate(0, 0, zOffset);

			graphics.setColor(1f, 1f, 1f, 0.8f);
			graphics.renderTooltip(ClientUtils.getMc().font, tooltip, Optional.empty(), mouseX, Math.max(mouseY, 18));
			graphics.setColor(1f, 1f, 1f, 1f);
		}
	}
}
