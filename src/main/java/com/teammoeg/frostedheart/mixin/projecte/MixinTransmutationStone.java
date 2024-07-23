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

import javax.annotation.Nonnull;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.teammoeg.frostedheart.util.FHUtils;
import com.teammoeg.frostedheart.util.TranslateUtils;

import moze_intel.projecte.gameObjs.blocks.TransmutationStone;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundSetTitlesPacket;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;

@Mixin(TransmutationStone.class)
public class MixinTransmutationStone {

    @Inject(method = "onBlockActivated", at = @At(value = "HEAD"), cancellable = true)
    public void hibernation(@Nonnull BlockState state, Level world, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand hand,
                            @Nonnull BlockHitResult rtr, CallbackInfoReturnable<InteractionResult> cir) {
        if (!world.isClientSide) {
            ServerPlayer serverPlayerEntity = (ServerPlayer) player;

            ServerLevel serverWorld = (ServerLevel) world;
            serverPlayerEntity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, (int) (1000 * (world.random.nextDouble() + 0.5)), 3));
            serverPlayerEntity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, (int) (1000 * (world.random.nextDouble() + 0.5)), 5));
            serverPlayerEntity.connection.send(new ClientboundSetTitlesPacket(ClientboundSetTitlesPacket.Type.TITLE, TranslateUtils.translateMessage("too_cold_to_transmute")));
            serverPlayerEntity.connection.send(new ClientboundSetTitlesPacket(ClientboundSetTitlesPacket.Type.SUBTITLE, TranslateUtils.translateMessage("magical_backslash")));

            double posX = pos.getX() + (world.random.nextDouble() - world.random.nextDouble()) * 4.5D;
            double posY = pos.getY() + world.random.nextInt(3) - 1;
            double posZ = pos.getZ() + (world.random.nextDouble() - world.random.nextDouble()) * 4.5D;
            if (world.noCollision(EntityType.WITCH.getAABB(posX, posY, posZ))
                    && SpawnPlacements.checkSpawnRules(EntityType.WITCH, serverWorld, MobSpawnType.NATURAL, new BlockPos(posX, posY, posZ), world.getRandom())) {
                FHUtils.spawnMob(serverWorld, new BlockPos(posX, posY, posZ), new CompoundTag(), new ResourceLocation("minecraft", "witch"));
            }
        }
        cir.setReturnValue(InteractionResult.SUCCESS);
    }
}
