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

import com.teammoeg.chorda.lang.Components;
import com.teammoeg.frostedheart.content.town.AbstractTownWorkerBlockScreen;
import com.teammoeg.frostedheart.util.client.FHClientUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class WarehouseScreen extends AbstractTownWorkerBlockScreen<WarehouseMenu> {
    private static final ResourceLocation TEXTURE = FHClientUtils.makeGuiTextureLocation("townworkerblock");
    public WarehouseScreen(WarehouseMenu inventorySlotsIn, Inventory inv, Component title) {
        super(inventorySlotsIn, inv, title, TEXTURE);

        WarehouseBlockEntity blockEntity = getMenu().getBlock();
        addTabContent((left,top)->{
            this.addRenderableWidget(new Label(left + 10, top + 20, Components.str("Volume: " + (blockEntity.getVolume())), 0xFFFFFF));
            this.addRenderableWidget(new Label(left + 10, top + 40, Components.str("Area: " + (blockEntity.getArea())), 0xFFFFFF));
        });
        addTabContent((left,top)->{
            this.addRenderableWidget(new Label(left + 10, top + 20, Components.str("Capacity: " + BigDecimal.valueOf(blockEntity.getCapacity())
                    .setScale(2, RoundingMode.HALF_UP).doubleValue()), 0xFFFFFF));
        });

    }
}

