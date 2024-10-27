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

package com.teammoeg.frostedheart.mixin.minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.teammoeg.frostedheart.content.utility.DeathInventoryData;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.FakePlayer;
/**
 * Keep equipments for players
 * */
@Mixin(Player.class)
public abstract class PlayerEntityMixin extends LivingEntity {

    protected PlayerEntityMixin(EntityType<? extends LivingEntity> type, Level worldIn) {
        super(type, worldIn);
    }

    @Inject(at = @At("HEAD"), method = "dropEquipment")
    public void fh$dropInventory(CallbackInfo cbi) {
        super.dropEquipment();
        if (((Object) this) instanceof FakePlayer)
            return;
        DeathInventoryData dit = DeathInventoryData.get((Player) ((Object) this));
        dit.startClone();
    }


}
