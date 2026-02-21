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

package com.teammoeg.frostedheart.content.town.buildings.warehouse;

import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.town.AbstractTownWorkerBlockScreen;
import com.teammoeg.frostedheart.content.town.tabs.TownResourceTab;
import com.teammoeg.frostedheart.content.town.tabs.AbstractTownTab;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class WarehouseScreen extends AbstractTownWorkerBlockScreen<WarehouseMenu> {
    public static final CIcons.CTextureIcon ALL = CIcons
            .getIcon(new ResourceLocation(FHMain.MODID, "textures/gui/townworkerblock.png"));
    public static final CIcons.CTextureIcon background = ALL.withUV(0, 0, 176, 222, 256, 256);
    public static final CIcons.CTextureIcon activeButton = ALL.withUV(180, 59, 22, 18, 256, 256);
    public static final CIcons.CTextureIcon inactiveButton = ALL.withUV(180 + 22, 59, 22, 18, 256, 256);

    public WarehouseScreen(WarehouseMenu inventorySlotsIn) {
        super(inventorySlotsIn);
    }

    @Override
    public void drawBackground(GuiGraphics graphics, int x, int y, int w, int h) {
        background.draw(graphics, x, y, 176, 222);
    }

    @Override
    protected void initTabs() {
        addTab(new AbstractTownTab(this) {
            @Override
            public CIcons.CIcon getIcon() {
                return inactiveButton;
            }
            @Override
            public CIcons.CIcon getActiveIcon() {
                return WarehouseScreen.activeButton;
            }

            @Override
            public void build(UILayer layer) {

            }
        });

        addTab(new TownResourceTab(this));
    }

}

