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

package com.teammoeg.frostedheart.mixin.minecraft.misc;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import com.teammoeg.frostedheart.content.utility.DeathInventoryData;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.Container;
import net.minecraft.world.Nameable;
import net.minecraftforge.common.util.FakePlayer;
/**
 * Keep equipments for players
 * */
@Mixin(Inventory.class)
public abstract class PlayerInventoryMixin_KeepEquipment implements Container, Nameable {

    public PlayerInventoryMixin_KeepEquipment() {
    }

    @Inject(at = @At("HEAD"), method = "dropAll", cancellable = true)
    public void fh$dropAllItems(CallbackInfo cbi) {
        if (getThis().player instanceof FakePlayer)
            return;
        DeathInventoryData dit = DeathInventoryData.get(getThis().player);
        if (FHConfig.SERVER.MISC.keepEquipments.get()) {
            if (dit != null)
                dit.death(getThis());
        }
    }

    private Inventory getThis() {
        return (Inventory) (Object) this;
    }

}
