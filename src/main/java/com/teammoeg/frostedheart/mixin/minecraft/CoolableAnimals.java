/*
 * Copyright (c) 2022-2024 TeamMoeg
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

import com.teammoeg.frostedheart.FHDamageSources;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.heatdevice.chunkheatdata.ChunkHeatData;
import com.teammoeg.frostedheart.util.FHUtils;
import com.teammoeg.frostedheart.util.mixin.IFeedStore;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

@Mixin({Sheep.class, Bee.class, Pig.class, Rabbit.class})
public class CoolableAnimals extends Mob {
    short hxteTimer;

    protected CoolableAnimals(EntityType<? extends Mob> type, Level worldIn) {
        super(type, worldIn);
    }

    @Inject(at = @At("HEAD"), method = "readAdditionalSaveData")
    public void fh$readAdditional(CompoundTag compound, CallbackInfo cbi) {
        hxteTimer = compound.getShort("hxthermia");
    }

    @Inject(at = @At("HEAD"), method = "addAdditionalSaveData")
    public void fh$writeAdditional(CompoundTag compound, CallbackInfo cbi) {
        compound.putShort("hxthermia", hxteTimer);

    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {

            if (FHUtils.isBlizzardHarming(level(), this.blockPosition())) {
                if (hxteTimer < 20) {
                    hxteTimer++;
                } else {
                    this.hurt(FHDamageSources.createSource(level(), FHDamageSources.BLIZZARD, this), 1);
                }
            } else {
                float temp = ChunkHeatData.getTemperature(this.getCommandSenderWorld(), this.blockPosition());
                if (temp < WorldTemperature.ANIMAL_ALIVE_TEMPERATURE
                        || temp > WorldTemperature.VANILLA_PLANT_GROW_TEMPERATURE_MAX) {
                    if (hxteTimer < 100) {
                        hxteTimer++;
                    } else {
                        if (temp > WorldTemperature.FEEDED_ANIMAL_ALIVE_TEMPERATURE)
                            if (this instanceof IFeedStore) {
                                if (((IFeedStore) this).consumeFeed()) {
                                    hxteTimer = -7900;
                                    return;
                                }
                            }
                        hxteTimer = 0;
                        this.hurt(FHDamageSources.createSource(level(), temp > 0 ? FHDamageSources.HYPERTHERMIA : FHDamageSources.HYPOTHERMIA, this), 2);
                    }
                } else if (hxteTimer > 0)
                    hxteTimer--;
            }
        }
    }
}
