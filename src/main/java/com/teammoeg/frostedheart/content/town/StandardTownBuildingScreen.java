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
 */

package com.teammoeg.frostedheart.content.town;

import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.menu.CBlockEntityMenu;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.town.block.AbstractTownBuildingBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Shared 176x222 town-building frame. Its upper 130 pixels are information;
 * the lower part remains the normal player inventory and hotbar.
 */
public abstract class StandardTownBuildingScreen<
        C extends CBlockEntityMenu<? extends AbstractTownBuildingBlockEntity>>
        extends AbstractTownWorkerBlockScreen<C> {
    protected static final CIcons.CTextureIcon ALL = CIcons
            .getIcon(new ResourceLocation(FHMain.MODID, "textures/gui/townworkerblock.png"));
    protected static final CIcons.CTextureIcon BACKGROUND =
            ALL.withUV(0, 0, 176, 222, 256, 256);
    protected static final CIcons.CTextureIcon ACTIVE_TAB =
            ALL.withUV(180, 59, 22, 18, 256, 256);
    protected static final CIcons.CTextureIcon INACTIVE_TAB =
            ALL.withUV(202, 59, 22, 18, 256, 256);

    protected StandardTownBuildingScreen(C menu) {
        super(menu);
    }

    @Override
    public void drawBackground(
            GuiGraphics graphics, int x, int y, int width, int height,
            RenderingHint hint
    ) {
        BACKGROUND.draw(graphics, x, y, 176, 222);
    }
}
