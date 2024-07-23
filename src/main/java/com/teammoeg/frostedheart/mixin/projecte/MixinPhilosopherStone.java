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
import com.teammoeg.frostedheart.util.TranslateUtils;

import moze_intel.projecte.gameObjs.items.PhilosophersStone;
import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.play.server.STitlePacket;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

@Mixin(PhilosophersStone.class)
public class MixinPhilosopherStone {
    @Inject(method = "onItemUse", at = @At(value = "HEAD"), cancellable = true)
    public void hibernation(ItemUseContext ctx, CallbackInfoReturnable<ActionResultType> cir) {
        World world = ctx.getLevel();
        PlayerEntity player = ctx.getPlayer();
        BlockPos pos = ctx.getClickedPos();
        if (!world.isClientSide && player != null) {
            ServerWorld serverWorld = (ServerWorld) world;
            ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity) player;

            serverPlayerEntity.addEffect(new EffectInstance(Effects.BLINDNESS, (int) (100 * (world.random.nextDouble() + 0.5)), 3));
            serverPlayerEntity.addEffect(new EffectInstance(Effects.CONFUSION, (int) (1000 * (world.random.nextDouble() + 0.5)), 5));

            serverPlayerEntity.connection.send(new STitlePacket(STitlePacket.Type.TITLE, TranslateUtils.translateMessage("too_cold_to_transmute")));
            serverPlayerEntity.connection.send(new STitlePacket(STitlePacket.Type.SUBTITLE, TranslateUtils.translateMessage("magical_backslash")));

            double posX = pos.getX() + (world.random.nextDouble() - world.random.nextDouble()) * 4.5D;
            double posY = pos.getY() + world.random.nextInt(3) - 1;
            double posZ = pos.getZ() + (world.random.nextDouble() - world.random.nextDouble()) * 4.5D;
            if (world.noCollision(EntityType.WITCH.getAABB(posX, posY, posZ))
                    && EntitySpawnPlacementRegistry.checkSpawnRules(EntityType.WITCH, serverWorld, SpawnReason.NATURAL, new BlockPos(posX, posY, posZ), world.getRandom())) {
                FHUtils.spawnMob(serverWorld, new BlockPos(posX, posY, posZ), new CompoundNBT(), new ResourceLocation("minecraft", "witch"));
            }
        }
        cir.setReturnValue(ActionResultType.SUCCESS);
    }
}
