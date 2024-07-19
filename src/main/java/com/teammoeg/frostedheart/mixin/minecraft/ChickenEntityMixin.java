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
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.teammoeg.frostedheart.FHDamageSources;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.heatdevice.chunkheatdata.ChunkHeatData;
import com.teammoeg.frostedheart.util.FHUtils;
import com.teammoeg.frostedheart.util.mixin.IFeedStore;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvents;
import net.minecraft.world.World;

@Mixin(ChickenEntity.class)
public abstract class ChickenEntityMixin extends AnimalEntity implements IFeedStore {
    private final static ResourceLocation chicken_feed = new ResourceLocation(FHMain.MODID, "chicken_feed");

    @Shadow
    public int timeUntilNextEgg;

    byte feeded;
    int digestTimer;
    byte egg;
    short hxteTimer;
    protected ChickenEntityMixin(EntityType<? extends AnimalEntity> type, World worldIn) {
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

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/ChickenEntity;playSound(Lnet/minecraft/util/SoundEvent;FF)V"), method = "livingTick", cancellable = true)
    public void fh$layegg(CallbackInfo cbi) {
        if (egg > 0) {
            egg--;
            this.playSound(SoundEvents.ENTITY_CHICKEN_EGG, 1.0F,
                    (this.rand.nextFloat() - this.rand.nextFloat()) * 0.2F + 1.0F);
            this.entityDropItem(Items.EGG);
            this.timeUntilNextEgg = this.rand.nextInt(6000) + 6000;
        } else if (feeded > 0) {
            this.timeUntilNextEgg = 3000;
        } else
            this.timeUntilNextEgg = 28800;
        cbi.cancel();
    }

    @Inject(at = @At("HEAD"), method = "writeAdditional")
    public void fh$readAdditional(CompoundNBT compound, CallbackInfo cbi) {
        egg = compound.getByte("egg_stored");
        feeded = compound.getByte("feed_stored");
        digestTimer = compound.getInt("feed_digest");
        hxteTimer = compound.getShort("hxthermia");
    }

    @Inject(at = @At("HEAD"), method = "writeAdditional")
    public void fh$writeAdditional(CompoundNBT compound, CallbackInfo cbi) {
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
    public ActionResultType getEntityInteractionResult(PlayerEntity playerIn, Hand hand) {
        ItemStack itemstack = playerIn.getHeldItem(hand);

        if (!this.isChild() && !itemstack.isEmpty() && itemstack.getItem().getTags().contains(chicken_feed)) {
            if (feeded < 4) {

                if (!this.world.isRemote)
                    this.consumeItemFromStack(playerIn, itemstack);
                feeded++;
                return ActionResultType.func_233537_a_(this.world.isRemote);
            }
        }
        return super.getEntityInteractionResult(playerIn, hand);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.world.isRemote) {

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

            if (FHUtils.isBlizzardHarming(world, this.getPosition())) {
                if (hxteTimer < 20) {
                    hxteTimer++;
                } else {
                    this.attackEntityFrom(FHDamageSources.BLIZZARD, 1);
                }
            } else {
                float temp = ChunkHeatData.getTemperature(this.getEntityWorld(), this.getPosition());
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
                        this.attackEntityFrom(temp > 0 ? FHDamageSources.HYPERTHERMIA : FHDamageSources.HYPOTHERMIA, 2);
                    }
                } else if (hxteTimer > 0)
                    hxteTimer--;
            }
        }
    }
}
