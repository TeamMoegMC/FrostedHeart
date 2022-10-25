/*
 * Copyright (c) 2022 TeamMoeg
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

package com.teammoeg.frostedheart.compat;

import com.teammoeg.frostedheart.FHBlocks;
import com.teammoeg.frostedheart.FHTileTypes;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber
public class CMUpdate {
    @SubscribeEvent
    public void onMissingTE(final RegistryEvent.MissingMappings<TileEntityType<?>> ev) {
        ev.getMappings("custommachinery").forEach(e -> e.remap(FHTileTypes.CMUPDATE.get()));
        ;
        System.out.println("mmtfired");
    }

    @SubscribeEvent
    public void onMissing(final RegistryEvent.MissingMappings<Block> ev) {
        ev.getMappings("custommachinery").forEach(e -> {
            e.remap(FHBlocks.cmupdate);
            System.out.println(e.key);
        });
        ;
        System.out.println("mmbfired");
    }

    @SubscribeEvent
    public void onMissingIT(final RegistryEvent.MissingMappings<Item> ev) {
        ev.getMappings("custommachinery").forEach(e -> {
            e.remap(ForgeRegistries.ITEMS.getValue(FHBlocks.cmupdate.getRegistryName()));
            System.out.println(e.key);
        });
        ;
        System.out.println("mmbfired");
    }
}
