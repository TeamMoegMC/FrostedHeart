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

package com.teammoeg.frostedheart.mixin.minecraft.thermal;

import com.teammoeg.frostedheart.content.climate.block.campfire.CampfireGameplay;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftThermalInput;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CampfireBlock.class)
public abstract class CampfireBlockMixin_TimeLimit extends BaseEntityBlock {
    protected CampfireBlockMixin_TimeLimit(Properties properties) {
        super(properties);
    }

    @Inject(method = "getStateForPlacement", at = @At("RETURN"), cancellable = true)
    private void frostedheart$placeUnlit(BlockPlaceContext context, CallbackInfoReturnable<BlockState> callback) {
        callback.setReturnValue(callback.getReturnValue().setValue(CampfireBlock.LIT, false));
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos position, BlockState previous, boolean moving) {
        super.onPlace(state, level, position, previous, moving);
        if (level instanceof ServerLevel server)
            MinecraftThermalInput.onCampfireStateChanged(server, position);
    }

    @Inject(method = "onRemove", at = @At("RETURN"))
    private void frostedheart$sourceRemoved(BlockState state, Level level, BlockPos position,
            BlockState next, boolean moving, CallbackInfo callback) {
        if (level instanceof ServerLevel server)
            MinecraftThermalInput.onCampfireStateChanged(server, position);
    }

    /**
     * @author TeamMoeg
     * @reason Preserve the fuel and research interaction contract.
     */
    @Overwrite
    public InteractionResult use(BlockState state, Level level, BlockPos position, Player player,
            InteractionHand hand, BlockHitResult hit) {
        return CampfireGameplay.use(state, level, position, player, hand);
    }

    @Override
    public void stepOn(Level level, BlockPos position, BlockState state, Entity entity) {
        CampfireGameplay.stepOn(level, position, state, entity);
    }
}
