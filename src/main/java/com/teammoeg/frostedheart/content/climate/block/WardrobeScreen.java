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

package com.teammoeg.frostedheart.content.climate.block;
import blusunrize.immersiveengineering.client.gui.IEContainerScreen;
import com.teammoeg.frostedheart.content.climate.block.WardrobeContainer;
import com.teammoeg.frostedheart.util.client.FHClientUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class WardrobeScreen extends IEContainerScreen<WardrobeContainer> {
    private static final ResourceLocation TEXTURE = FHClientUtils.makeTextureLocation("wardrobe");

    public WardrobeScreen(WardrobeContainer inventorySlotsIn, Inventory inv, Component title) {
        super(inventorySlotsIn, inv, title,TEXTURE);
    }
}
