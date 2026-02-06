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

package com.teammoeg.frostedheart.util;

import com.teammoeg.frostedheart.bootstrap.reference.FHTags;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.Tags;

public class IgnitionHandler {
    public static boolean tryIgnition(RandomSource rand, ItemStack handStack, ItemStack offHandStack) {
        if (handStack.is(Tags.Items.RODS_WOODEN) && offHandStack.is(Tags.Items.RODS_WOODEN)) {
            if (rand.nextFloat() < FHConfig.SERVER.FIRE_IGNITION.stickIgnitionChance.get()) {
                handStack.shrink(1);
                offHandStack.shrink(1);
                return true;
            } else if (rand.nextFloat() < FHConfig.SERVER.FIRE_IGNITION.consumeChanceWhenIgnited.get()) {
                handStack.shrink(1);
                offHandStack.shrink(1);
                return false;
            }
        } else if (handStack.is(FHTags.Items.IGNITION_METAL.tag) && offHandStack.is(FHTags.Items.IGNITION_MATERIAL.tag)) {
            if (rand.nextFloat() < FHConfig.SERVER.FIRE_IGNITION.flintIgnitionChance.get()) {
                offHandStack.shrink(1);
                if (rand.nextFloat() < FHConfig.SERVER.FIRE_IGNITION.consumeChanceWhenIgnited.get()) {
                    handStack.shrink(1);
                }
                return true;
            }
        } else if (handStack.is(FHTags.Items.IGNITION_MATERIAL.tag) && offHandStack.is(FHTags.Items.IGNITION_METAL.tag)) {
            if (rand.nextFloat() < FHConfig.SERVER.FIRE_IGNITION.flintIgnitionChance.get()) {
                handStack.shrink(1);
                if (rand.nextFloat() < FHConfig.SERVER.FIRE_IGNITION.consumeChanceWhenIgnited.get()) {
                    offHandStack.shrink(1);
                }
                return true;
            }
        }
        return false;
    }
//    public static void addIgnitionTooltips(ItemStack stack, List<MutableComponent> text) {
//        if (stack.is(Tags.Items.RODS_WOODEN)) {
//            text.add(Lang.translateTooltip("double_stick_ignition").withStyle(ChatFormatting.RED));
//        } else if (stack.is(FHTags.Items.IGNITION_MATERIAL)) {
//            text.add(Lang.translateTooltip("ignition_material").withStyle(ChatFormatting.GRAY));
//            text.add(Lang.translateTooltip("ignition_tutorial").withStyle(ChatFormatting.GRAY));
//            List<Item> metals = ForgeRegistries.ITEMS.getValues().stream()
//                    .filter(item -> item.builtInRegistryHolder().is(FHTags.Items.IGNITION_METAL))
//                    .toList();
//            for (Item item : metals) {
//                text.add(item.getDescription().copy().withStyle(ChatFormatting.GRAY));
//            }
//        } else if (stack.is(FHTags.Items.IGNITION_METAL)) {
//            text.add(Lang.translateTooltip("ignition_metal").withStyle(ChatFormatting.RED));
//            text.add(Lang.translateTooltip("ignition_tutorial").withStyle(ChatFormatting.GRAY));
//            // append the localized names of ignition materials from the tag
//            // get all items in the tag
//            List<Item> materials = ForgeRegistries.ITEMS.getValues().stream()
//                    .filter(item -> item.builtInRegistryHolder().is(FHTags.Items.IGNITION_MATERIAL))
//                    .toList();
//            for (Item item : materials) {
//                text.add(item.getDescription().copy().withStyle(ChatFormatting.GRAY));
//            }
//        }
//    }
}
