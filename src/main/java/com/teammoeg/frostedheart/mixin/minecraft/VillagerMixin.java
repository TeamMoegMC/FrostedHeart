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

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.teammoeg.frostedheart.content.trade.FHVillagerData;
import com.teammoeg.frostedheart.content.trade.TradeHandler;
import com.teammoeg.frostedheart.util.mixin.VillagerDataHolder;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
/**
 * New trade system
 * */
@Mixin(Villager.class)
public abstract class VillagerMixin extends AbstractVillager implements VillagerDataHolder {
    FHVillagerData fh$data = new FHVillagerData(getThis());

    public VillagerMixin(EntityType<? extends AbstractVillager> type, Level worldIn) {
        super(type, worldIn);
    }

    @Shadow
    protected abstract void displayMerchantGui(Player pe);

    @Inject(at = @At("HEAD"), method = "readAdditional")
    public void fh$readAdditional(CompoundTag compound, CallbackInfo cbi) {
        fh$data.deserialize(compound.getCompound("fhdata"));
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/merchant/villager/VillagerEntity;resetCustomer()V", ordinal = 0), method = "updateAITasks", cancellable = true, require = 1)
    public void fh$updateTask(CallbackInfo cbi) {
        super.customServerAiStep();
        cbi.cancel();
    }

    @Inject(at = @At("HEAD"), method = "writeAdditional")
    public void fh$writeAdditional(CompoundTag compound, CallbackInfo cbi) {
        CompoundTag cnbt = new CompoundTag();
        fh$data.serialize(cnbt);
        compound.put("fhdata", cnbt);
    }

    /**
     * @author khjxiaogu
     * @reason disable villager trade for our system
     */
    @Overwrite
    public InteractionResult mobInteract(Player playerIn, InteractionHand hand) {
        ItemStack itemstack = playerIn.getItemInHand(hand);
        if (itemstack.getItem() != Items.VILLAGER_SPAWN_EGG && this.isAlive() && !this.isTrading()
                && !this.isSleeping() && !playerIn.isSecondaryUseActive()) {
            if (this.isBaby()) {
                this.shakeHead();
                return InteractionResult.sidedSuccess(this.level.isClientSide);
            }
			/*boolean flag = this.getOffers().isEmpty();
			if (hand == Hand.MAIN_HAND) {
				if (flag && !this.world.isRemote) {
					this.shakeHead();
				}

				playerIn.addStat(Stats.TALKED_TO_VILLAGER);
			}*/
			/*if (flag) {
				return ActionResultType.sidedSuccess(this.world.isRemote);
			}*/
            if (!this.level.isClientSide) {
                //return fh$data.trade(playerIn);
            	/*fh$data.update((ServerWorld) super.world, playerIn);
            	RelationList list=fh$data.getRelationShip(playerIn);
            	if(list.sum()<-30) {
	                this.shakeHead();
	                playerIn.sendMessage(GuiUtils.translateMessage("village.unknown"), playerIn.getUniqueID());
            	}*/
                playerIn.awardStat(Stats.TALKED_TO_VILLAGER);
                setTradingPlayer(playerIn);
                //System.out.println(this.getCustomer());
                TradeHandler.openTradeScreen((ServerPlayer) playerIn, fh$data);

            }

            //System.out.println("sent");
			/*if (!this.world.isRemote && !this.offers.isEmpty()) {
				this.displayMerchantGui(playerIn);
			}*/

            return InteractionResult.sidedSuccess(this.level.isClientSide);
        }
        return super.mobInteract(playerIn, hand);
    }

    @Override
    public FHVillagerData getFHData() {
        return fh$data;
    }

    private Villager getThis() {
        return (Villager) (Object) this;
    }

    @Shadow
    public abstract void setTradingPlayer(@Nullable Player player);

    @Shadow
    protected abstract void shakeHead();
}
