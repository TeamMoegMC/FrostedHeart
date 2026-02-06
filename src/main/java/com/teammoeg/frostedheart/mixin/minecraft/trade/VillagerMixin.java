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

package com.teammoeg.frostedheart.mixin.minecraft.trade;

import javax.annotation.Nullable;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.trade.*;
import com.teammoeg.frostedheart.util.Lang;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
 */
@Mixin(Villager.class)
public abstract class VillagerMixin extends AbstractVillager implements VillagerDataHolder {
    FHVillagerData fh$data = new FHVillagerData(getThis());

    public VillagerMixin(EntityType<? extends AbstractVillager> type, Level worldIn) {
        super(type, worldIn);
    }


    @Inject(at = @At("HEAD"), method = "readAdditionalSaveData")
    public void fh$readAdditional(CompoundTag compound, CallbackInfo cbi) {
        fh$data.deserialize(compound.getCompound("fhdata"));
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/npc/Villager;isTrading()Z", ordinal = 0), method = "customServerAiStep", cancellable = true, require = 1)
    public void fh$customServerAiStep(CallbackInfo cbi) {
        super.customServerAiStep();
        cbi.cancel();
    }

    @Inject(at = @At("HEAD"), method = "addAdditionalSaveData")
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
        // FHMain.LOGGER.info("Villager mobInteract side = {}", level().isClientSide ? "CLIENT" : "SERVER");
        ItemStack itemstack = playerIn.getItemInHand(hand);
        if (itemstack.getItem() != Items.VILLAGER_SPAWN_EGG && this.isAlive() && !this.isTrading()
                && !this.isSleeping() && !playerIn.isSecondaryUseActive()) {
            if (this.isBaby()) {
                this.setUnhappy();
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            } else {
                if (!this.level().isClientSide) {
                    fh$data.update((ServerLevel) super.level(), playerIn);
                    RelationList list = fh$data.getRelationShip(playerIn);
                    int unknownLanguage = list.get(RelationModifier.UNKNOWN_LANGUAGE);

                    // conversation
                    if (unknownLanguage < 0) {
                        this.setUnhappy();
                        playerIn.displayClientMessage(Lang.translateMessage("trade.language_barrier"), false);
                    } else {
                        if (list.sum() < TradeConstants.RELATION_TO_TRADE) {
                            this.setUnhappy();
                            playerIn.displayClientMessage(Lang.translateMessage("trade.bad_relation"), false);
                        } else if (list.sum() < TradeConstants.RELATION_TO_BARGAIN) {
                            playerIn.displayClientMessage(Lang.translateMessage("trade.normal_relation"), false);
                        } else {
                            playerIn.displayClientMessage(Lang.translateMessage("trade.great_relation"), false);
                        }
                        Brain<Villager> brain = getThis().getBrain();
                        boolean hasHome = brain.hasMemoryValue(MemoryModuleType.HOME);
                        if (!hasHome) {
                            this.setUnhappy();
                            playerIn.displayClientMessage(Lang.translateMessage("trade.no_home"), false);
                        }
                        else {
                            float t = WorldTemperature.block(getThis().level(), getThis().blockPosition());
                            if (t < TradeConstants.TOO_COLD_RECOVER_TEMP) {
                                this.setUnhappy();
                                playerIn.displayClientMessage(Lang.translateMessage("trade.low_temp"), false);
                            }
                        }
                    }

                    // always open screen though
                    playerIn.awardStat(Stats.TALKED_TO_VILLAGER);
                    setTradingPlayer(playerIn);
                    TradeHandler.openTradeScreen((ServerPlayer) playerIn, fh$data);
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
        } else {
            return super.mobInteract(playerIn, hand);
        }

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
    protected abstract void setUnhappy();
}
