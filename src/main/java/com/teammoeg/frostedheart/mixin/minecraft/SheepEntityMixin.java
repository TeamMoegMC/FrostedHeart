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
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.util.mixin.IFeedStore;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;

@Mixin(Sheep.class)
public abstract class SheepEntityMixin extends Animal implements IFeedStore {


    private final static TagKey<Item> cow_feed = ItemTags.create(new ResourceLocation(FHMain.MODID, "cow_feed"));

    byte feeded = 0;
    protected SheepEntityMixin(EntityType<? extends Animal> type, Level worldIn) {
        super(type, worldIn);
    }

    @Override
    public boolean consumeFeed() {
        if (feeded > 0) {
            feeded--;
            return true;
        }
        return false;
    }

    @Inject(at = @At("HEAD"), method = "ate")
    public void fh$eatGrass(CallbackInfo cbi) {
        if (feeded < 2)
            feeded++;
    }

    @Inject(at = @At("HEAD"), method = "mobInteract", cancellable = true)
    public void fh$getEntityInteractionResult(Player playerIn, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cbi) {
        ItemStack itemstack = playerIn.getItemInHand(hand);

        if (!this.isBaby() && !itemstack.isEmpty() && itemstack.is(cow_feed)) {
            if (feeded < 2) {
                feeded++;
                if (!this.level().isClientSide)
                    this.usePlayerItem(playerIn, hand, itemstack);
                cbi.setReturnValue(InteractionResult.sidedSuccess(this.level().isClientSide));
            }
        }
    }

    @Inject(at = @At("HEAD"), method = "readAdditionalSaveData")
    public void fh$readAdditional(CompoundTag compound, CallbackInfo cbi) {
        feeded = compound.getByte("feed_stored");
    }

    @Inject(at = @At("HEAD"), method = "addAdditionalSaveData")
    public void fh$writeAdditional(CompoundTag compound, CallbackInfo cbi) {
        compound.putByte("feed_stored", feeded);

    }


    @Shadow
    public abstract boolean readyForShearing();

    @Shadow
    public abstract void shear(SoundSource category);
}
