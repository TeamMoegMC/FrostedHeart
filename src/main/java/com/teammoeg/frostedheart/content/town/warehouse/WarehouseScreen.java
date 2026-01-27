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

package com.teammoeg.frostedheart.content.town.warehouse;

import com.teammoeg.chorda.client.cui.MouseButton;
import com.teammoeg.chorda.client.cui.TabImageButtonElement;
import com.teammoeg.chorda.client.cui.UIElement;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.content.town.AbstractTownWorkerBlockScreen;
import com.teammoeg.frostedheart.content.town.network.WarehouseC2SRequestPacket;
import com.teammoeg.frostedheart.util.client.FHClientUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class WarehouseScreen extends AbstractTownWorkerBlockScreen<WarehouseMenu> {
    private VirtualItemGridElement grid;
    private static final ResourceLocation TEXTURE = FHClientUtils.makeGuiTextureLocation("townworkerblock");
    public static final CIcons.CTextureIcon ALL = CIcons
            .getIcon(new ResourceLocation(FHMain.MODID, "textures/gui/townworkerblock.png"));
    public static final CIcons.CTextureIcon background = ALL.withUV(0, 0, 176, 222, 256, 256);
    public static final CIcons.CTextureIcon activeButton = ALL.withUV(180, 59, 22, 18, 256, 256);
    public static final CIcons.CTextureIcon inactiveButton = ALL.withUV(180 + 22, 59, 22, 18, 256, 256);

    public WarehouseScreen(WarehouseMenu inventorySlotsIn) {
        super(inventorySlotsIn);
        FHNetwork.INSTANCE.sendToServer(new WarehouseC2SRequestPacket());
/*        WarehouseBlockEntity blockEntity = getMenu().getBlock();
        addTabContent((left,top)->{
            this.addRenderableWidget(new Label(left + 10, top + 20, Components.str("Volume: " + (blockEntity.getVolume())), 0xFFFFFF));
            this.addRenderableWidget(new Label(left + 10, top + 40, Components.str("Area: " + (blockEntity.getArea())), 0xFFFFFF));
        });
        addTabContent((left,top)->{
            this.addRenderableWidget(new Label(left + 10, top + 20, Components.str("Capacity: " + BigDecimal.valueOf(blockEntity.getCapacity())
                    .setScale(2, RoundingMode.HALF_UP).doubleValue()), 0xFFFFFF));
        });
        addTabContent((left, top) -> {
            VirtualItemGridWidget gridWidget = new VirtualItemGridWidget(left + 7, top + 18, this::getResources);
            FHNetwork.INSTANCE.sendToServer(new WarehouseC2SRequestPacket());
            this.addRenderableWidget(gridWidget);
        });*/
        addTabContent((layer) -> {

        });
        addTabContent((layer) -> {
            this.grid = new VirtualItemGridElement(layer, 7, 18,
                    () -> ((WarehouseMenu) getCMenu()).getResources()
            );
            layer.add(this.grid);
        });
    }


    @Override
    public void drawBackground(GuiGraphics graphics, int x, int y, int w, int h) {
        background.draw(graphics, x, y, 176, 222);
    }
    public TabImageButtonElement getTabButton(int x, int y, int tabI) {
        return new TabImageButtonElement(this, x, y, 22, 18, tabI, getButtonIcon(0), getButtonIcon(1)){
            @Override
            public void onClicked(MouseButton button) {
                selectTab(tabI);
            }

        };
    }
    public CIcons.CIcon getButtonIcon(int i) {
        if (i==0)
            return activeButton;
        return inactiveButton;
    }
}

