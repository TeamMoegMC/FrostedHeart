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

package com.teammoeg.frostedheart.content.utility.transportation;

import com.teammoeg.frostedheart.FHAttributes;
import com.teammoeg.frostedheart.FHItems;
import com.teammoeg.frostedheart.FHTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;

public class MovementModificationHandler {
    /*
     * Movement modifiers for snowshoes and ice skates
     */
    public static void movementModifier(TickEvent.PlayerTickEvent event) {
        Level world = event.player.level();
        Player player = event.player;
        BlockPos pos;
        if (player.getY() % 1 < 0.5) {
            pos = player.blockPosition().below();
        } else {
            pos = player.blockPosition();
        }
        Block ground = world.getBlockState(pos).getBlock();

        AttributeInstance movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        AttributeInstance swimSpeed = player.getAttribute(ForgeMod.SWIM_SPEED.get());
        Item feetEquipment = player.getItemBySlot(EquipmentSlot.FEET).getItem();
        Item headEquipment = player.getItemBySlot(EquipmentSlot.HEAD).getItem();

        if (movementSpeed == null || swimSpeed == null) {
            return;
        }

        // check if match FHTags.Blocks.SNOW_MOVEMENT
        boolean isSnowBlock = world.getBlockState(player.blockPosition()).getTags().anyMatch(t -> t == FHTags.Blocks.SNOW_MOVEMENT);
        boolean isSnowBlockBelow = world.getBlockState(player.blockPosition().below()).getTags().anyMatch(t -> t == FHTags.Blocks.SNOW_MOVEMENT);
        boolean isIceBlock = world.getBlockState(player.blockPosition()).getTags().anyMatch(t -> t == FHTags.Blocks.ICE_MOVEMENT);
        boolean isIceBlockBelow = world.getBlockState(player.blockPosition().below()).getTags().anyMatch(t -> t == FHTags.Blocks.ICE_MOVEMENT);

        if (feetEquipment == FHItems.SNOWSHOES.get()) {
            if ((isSnowBlock || isSnowBlockBelow) && !movementSpeed.hasModifier(FHAttributes.SNOW_DRIFTER)) {
                movementSpeed.addTransientModifier(FHAttributes.SNOW_DRIFTER);
                player.setMaxUpStep(1.0f);
            }
        } else if (feetEquipment != FHItems.SNOWSHOES.get() && movementSpeed.hasModifier(FHAttributes.SNOW_DRIFTER)) {
            movementSpeed.removeModifier(FHAttributes.SNOW_DRIFTER);
            player.setMaxUpStep(0.5f);
        }
        if ((!isSnowBlock && !isSnowBlockBelow) && ground != Blocks.AIR && movementSpeed.hasModifier(FHAttributes.SNOW_DRIFTER)) {
            movementSpeed.removeModifier(FHAttributes.SNOW_DRIFTER);
            player.setMaxUpStep(0.5f);
        }
        if (feetEquipment == FHItems.ICE_SKATES.get()) {
            if ((isIceBlock || isIceBlockBelow) && !movementSpeed.hasModifier(FHAttributes.SPEED_SKATER)) {
                movementSpeed.addTransientModifier(FHAttributes.SPEED_SKATER);
                player.setMaxUpStep(1.0f);
            }
        } else if (feetEquipment != FHItems.ICE_SKATES.get() && movementSpeed.hasModifier(FHAttributes.SPEED_SKATER)) {
            movementSpeed.removeModifier(FHAttributes.SPEED_SKATER);
            player.setMaxUpStep(0.5f);
        }
        if ((!isIceBlock && !isIceBlockBelow) && ground != Blocks.AIR && movementSpeed.hasModifier(FHAttributes.SPEED_SKATER)) {
            movementSpeed.removeModifier(FHAttributes.SPEED_SKATER);
            player.setMaxUpStep(0.5f);
        }
    }
}
