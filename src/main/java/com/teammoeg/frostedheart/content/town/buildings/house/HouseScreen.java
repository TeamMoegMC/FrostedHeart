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

package com.teammoeg.frostedheart.content.town.buildings.house;

import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.content.town.AbstractTownWorkerBlockScreen;
import com.teammoeg.frostedheart.content.town.tabs.AbstractTownTab;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

public class HouseScreen extends AbstractTownWorkerBlockScreen<HouseMenu> {
    private static final CIcons.CTextureIcon ALL = CIcons
            .getIcon(new ResourceLocation(FHMain.MODID, "textures/gui/townworkerblock.png"));
    private static final CIcons.CTextureIcon BACKGROUND =
            ALL.withUV(0, 0, 176, 222, 256, 256);
    private static final CIcons.CTextureIcon ACTIVE_TAB =
            ALL.withUV(180, 59, 22, 18, 256, 256);
    private static final CIcons.CTextureIcon INACTIVE_TAB =
            ALL.withUV(202, 59, 22, 18, 256, 256);

    public HouseScreen(HouseMenu menu) {
        super(menu);
    }

    @Override
    public void drawBackground(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            RenderingHint hint
    ) {
        BACKGROUND.draw(graphics, x, y, 176, 222);
    }

    @Override
    protected void initTabs() {
        addTab(new OverviewTab(this));
        addTab(new ResidentsTab(this));
    }

    private static final class OverviewTab extends AbstractTownTab<HouseMenu> {
        private OverviewTab(AbstractTownWorkerBlockScreen<HouseMenu> screen) {
            super(screen);
        }

        @Override
        public CIcons.CIcon getIcon() {
            return INACTIVE_TAB;
        }

        @Override
        public CIcons.CIcon getActiveIcon() {
            return ACTIVE_TAB;
        }

        @Override
        public CIcons.CIcon getContentIcon() {
            return CIcons.getIcon(FHBlocks.HOUSE.get());
        }

        @Override
        public Component getTitle() {
            return Component.translatable("gui.frostedheart.house.overview");
        }

        @Override
        public void build(UILayer layer) {
            layer.add(new HouseOverviewElement(layer, 8, 4, getMenu()));
        }
    }

    private static final class ResidentsTab extends AbstractTownTab<HouseMenu> {
        private ResidentsTab(AbstractTownWorkerBlockScreen<HouseMenu> screen) {
            super(screen);
        }

        @Override
        public CIcons.CIcon getIcon() {
            return INACTIVE_TAB;
        }

        @Override
        public CIcons.CIcon getActiveIcon() {
            return ACTIVE_TAB;
        }

        @Override
        public CIcons.CIcon getContentIcon() {
            return CIcons.getIcon(Items.RED_BED);
        }

        @Override
        public Component getTitle() {
            return Component.translatable("gui.frostedheart.house.residents");
        }

        @Override
        public void build(UILayer layer) {
            layer.add(new HouseResidentPanel(layer, 8, 4, getMenu()));
        }
    }
}
