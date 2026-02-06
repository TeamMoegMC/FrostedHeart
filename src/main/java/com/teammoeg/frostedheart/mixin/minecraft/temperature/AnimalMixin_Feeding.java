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

package com.teammoeg.frostedheart.mixin.minecraft.temperature;

import com.teammoeg.frostedheart.bootstrap.reference.FHDamageSources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.teammoeg.frostedheart.bootstrap.reference.FHDamageTypes;
import com.teammoeg.frostedheart.content.climate.AttractedByGeneratorGoal;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.util.mixin.IFeedStore;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

@Mixin({Sheep.class, Bee.class, Pig.class})
public class AnimalMixin_Feeding extends Mob {
    short hxteTimer;

    protected AnimalMixin_Feeding(EntityType<? extends Mob> type, Level worldIn) {
        super(type, worldIn);
    }
    @Inject(at=@At("TAIL"),method="registerGoals",remap=true,require=1)
    public void fh$addGeneratorGoal(CallbackInfo cbi) {
    	   super.goalSelector.addGoal(5, new AttractedByGeneratorGoal(this,1.0D));
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

            if (WorldTemperature.isBlizzardHarming(level(), this.blockPosition())) {
                if (hxteTimer < 20) {
                    hxteTimer++;
                } else {
                    this.hurt(FHDamageSources.blizzard(level()), 1);
                }
            } else {
                float temp = WorldTemperature.block(this.getCommandSenderWorld(), this.blockPosition());
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
                        if (temp > 0) {
                            this.hurt(FHDamageSources.hyperthermia(level()), 2);
                        } else {
                            this.hurt(FHDamageSources.hypothermia(level()), 2);
                        }
                    }
                } else if (hxteTimer > 0)
                    hxteTimer--;
            }
        }
    }
}
