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
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.reference.FHDamageTypes;
import com.teammoeg.frostedheart.bootstrap.reference.FHTags;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.util.mixin.IFeedStore;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;

@Mixin(Chicken.class)
public abstract class ChickenMixin extends Animal implements IFeedStore {

    @Shadow
    public int eggTime;

    byte feeded;
    int digestTimer;
    byte egg;
    short hxteTimer;
    protected ChickenMixin(EntityType<? extends Animal> type, Level worldIn) {
        super(type, worldIn);
    }

    @Override
    public boolean consumeFeed() {
        if (feeded > 0) {
            feeded--;
            return true;
        }
        if (egg > 0) {
            egg--;
            return true;
        }
        return false;
    }
    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/Chicken;playSound(Lnet/minecraft/sounds/SoundEvent;FF)V"), method = "aiStep", cancellable = true)
    public void fh$layegg(CallbackInfo cbi) {
        if (egg > 0) {
            egg--;
            this.playSound(SoundEvents.CHICKEN_EGG, 1.0F,
                    (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
            this.spawnAtLocation(Items.EGG);
            this.eggTime = this.random.nextInt(6000) + 6000;
        } else if (feeded > 0) {
            this.eggTime = 3000;
        } else
            this.eggTime = 28800;
        cbi.cancel();
    }

    @Inject(at = @At("HEAD"), method = "readAdditionalSaveData")
    public void fh$readAdditional(CompoundTag compound, CallbackInfo cbi) {
        egg = compound.getByte("egg_stored");
        feeded = compound.getByte("feed_stored");
        digestTimer = compound.getInt("feed_digest");
        hxteTimer = compound.getShort("hxthermia");
    }

    @Inject(at = @At("HEAD"), method = "addAdditionalSaveData")
    public void fh$writeAdditional(CompoundTag compound, CallbackInfo cbi) {
        compound.putByte("egg_stored", egg);
        compound.putByte("feed_stored", feeded);
        compound.putInt("feed_digest", digestTimer);
        compound.putShort("hxthermia", hxteTimer);

    }

    /**
     * @author khjxiaogu
     * change to our own milk logic
     */
    @Override
    public InteractionResult mobInteract(Player playerIn, InteractionHand hand) {
        ItemStack itemstack = playerIn.getItemInHand(hand);

        if (!this.isBaby() && !itemstack.isEmpty() && isFood(itemstack)) {
            if (feeded < 4) {

                if (!this.level().isClientSide)
                    this.usePlayerItem(playerIn, hand, itemstack);
                feeded++;
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
        }
        return super.mobInteract(playerIn, hand);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {

            if (digestTimer > 0) {
                digestTimer--;
                if (digestTimer == 0) {
                    if (feeded > 0) {
                        feeded--;
                        if (egg < 4)
                            egg++;
                    }
                }
            } else if (feeded > 0) {
                digestTimer = 6000;
            }

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
                            if (((IFeedStore) this).consumeFeed()) {
                                hxteTimer = -7900;
                                return;
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
