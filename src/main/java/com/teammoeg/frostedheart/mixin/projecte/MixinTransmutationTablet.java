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

package com.teammoeg.frostedheart.mixin.projecte;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.teammoeg.frostedheart.util.FHUtils;
import com.teammoeg.frostedheart.util.client.GuiUtils;

import moze_intel.projecte.gameObjs.items.TransmutationTablet;
import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.play.server.STitlePacket;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

@Mixin(TransmutationTablet.class)
public class MixinTransmutationTablet {
    @Inject(method = "onItemRightClick", at = @At("HEAD"), cancellable = true)
    public void onItemRightClick(World world, PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cbi) {
        if (!world.isRemote) {
            ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity) player;

            ServerWorld serverWorld = (ServerWorld) world;
            serverPlayerEntity.addPotionEffect(new EffectInstance(Effects.BLINDNESS, (int) (1000 * (world.rand.nextDouble() + 0.5)), 3));
            serverPlayerEntity.addPotionEffect(new EffectInstance(Effects.NAUSEA, (int) (1000 * (world.rand.nextDouble() + 0.5)), 5));
            serverPlayerEntity.connection.sendPacket(new STitlePacket(STitlePacket.Type.TITLE, GuiUtils.translateMessage("too_cold_to_transmute")));
            serverPlayerEntity.connection.sendPacket(new STitlePacket(STitlePacket.Type.SUBTITLE, GuiUtils.translateMessage("magical_backslash")));

            double posX = serverPlayerEntity.getPosX() + (world.rand.nextDouble() - world.rand.nextDouble()) * 4.5D;
            double posY = serverPlayerEntity.getPosY() + world.rand.nextInt(3) - 1;
            double posZ = serverPlayerEntity.getPosZ() + (world.rand.nextDouble() - world.rand.nextDouble()) * 4.5D;
            if (world.hasNoCollisions(EntityType.WITCH.getBoundingBoxWithSizeApplied(posX, posY, posZ))
                    && EntitySpawnPlacementRegistry.canSpawnEntity(EntityType.WITCH, serverWorld, SpawnReason.NATURAL, new BlockPos(posX, posY, posZ), world.getRandom())) {
                FHUtils.spawnMob(serverWorld, new BlockPos(posX, posY, posZ), new CompoundNBT(), new ResourceLocation("minecraft", "witch"));
            }
        }
        cbi.setReturnValue(ActionResult.resultConsume(player.getHeldItem(hand)));
    }
}
