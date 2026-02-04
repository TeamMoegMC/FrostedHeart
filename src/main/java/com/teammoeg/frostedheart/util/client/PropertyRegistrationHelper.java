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

package com.teammoeg.frostedheart.util.client;

import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.function.Supplier;

public class PropertyRegistrationHelper {
    private final FMLClientSetupEvent event;

    public PropertyRegistrationHelper(FMLClientSetupEvent event) {
        this.event = event;
    }

    public PropertyRegistrationHelper register(Supplier<? extends ItemLike> itemSupplier, String propertyName, String nbtKey) {
        event.enqueueWork(() -> {
            ItemLike itemLike = itemSupplier.get();
            Item item = itemLike.asItem();

            ItemPropertyFunction getter = (stack, level, entity, seed) -> {
                CompoundTag tag = stack.getTagElement("BlockStateTag");
                if (tag != null && tag.contains(nbtKey)) {
                    try {
                        return Float.parseFloat(tag.getString(nbtKey));
                    } catch (Exception e) { return 0f; }
                }
                return 0f;
            };

            ItemProperties.register(item, new ResourceLocation(propertyName), getter);
        });
        return this;
    }
}
