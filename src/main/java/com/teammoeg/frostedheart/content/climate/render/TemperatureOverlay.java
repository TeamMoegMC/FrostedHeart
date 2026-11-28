/*
 * Copyright (c) 2026 TeamMoeg
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

package com.teammoeg.frostedheart.content.climate.render;

import java.util.List;
import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.PrimaryLayer;
import com.teammoeg.chorda.client.cui.base.UIElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** CUI primary layer for the goggles readout, following KeyHintOverlay and TipOverlay's lifecycle. */
public final class TemperatureOverlay extends PrimaryLayer {
    public static final TemperatureOverlay INSTANCE = new TemperatureOverlay();
    private final ReadoutElement readout = new ReadoutElement(this);

    private TemperatureOverlay() {}

    @Override
    public void addChildUIElements() {
        clearElement();
        add(readout);
    }

    @Override
    public void onBeforeRender() {
        super.onBeforeRender();
        readout.lines = TemperatureGoogleRenderer.collectFrameInformation();
        // Styles own screen-relative placement; the element supplies their full viewport.
        int width = ClientUtils.screenWidth();
        int height = ClientUtils.screenHeight();
        setSize(width, height);
        readout.setSize(width, height);
    }

    @Override
    public boolean shouldRenderGradient() { return false; }

    /** Persistent CUI leaf that renders the selected style from the current frame's data. */
    private static final class ReadoutElement extends UIElement {
        private List<Component> lines = List.of();
        private float framePartialTick;

        private ReadoutElement(PrimaryLayer parent) { super(parent); }

        @Override
        public boolean isVisible() { return !lines.isEmpty(); }

        @Override
        public void updateRenderInfo(int x, int y, double mouseX, double mouseY, float partialTick) {
            super.updateRenderInfo(x, y, mouseX, mouseY, partialTick);
            // UIElement ignores zero interpolation; the fade needs this frame's exact value.
            framePartialTick = partialTick;
        }

        @Override
        public void render(GuiGraphics graphics, int x, int y, int width, int height, RenderingHint hint) {
            TemperatureGoogleRenderer.drawReadout(graphics, lines, framePartialTick, width, height);
        }
    }
}
