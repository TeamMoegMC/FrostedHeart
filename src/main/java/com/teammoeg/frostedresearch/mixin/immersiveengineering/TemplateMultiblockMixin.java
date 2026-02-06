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

package com.teammoeg.frostedresearch.mixin.immersiveengineering;

import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.chorda.multiblock.CMultiblockHelper;
import com.teammoeg.chorda.multiblock.MultiBlockAccess;
import com.teammoeg.chorda.multiblock.components.IOwnerState;
import com.teammoeg.frostedheart.content.climate.block.generator.OwnedLogic;
import com.teammoeg.frostedresearch.mixinutil.IOwnerTile;

import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler.IMultiblock;
import blusunrize.immersiveengineering.api.multiblocks.TemplateMultiblock;
import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

@Mixin(TemplateMultiblock.class)
public abstract class TemplateMultiblockMixin implements IMultiblock, MultiBlockAccess {
    private UUID user;

    public TemplateMultiblockMixin() {
    }

    @Invoker(remap = false)
    public abstract void callForm(Level world, BlockPos pos, Rotation rot, Mirror mirror, Direction sideHit);

    @Inject(at = @At(value = "INVOKE", target = "Lblusunrize/immersiveengineering/api/multiblocks/TemplateMultiblock;form"), method = "createStructure", remap = false)
    public void fh$on$createStructure(Level world, BlockPos pos, Direction side, Player player, CallbackInfoReturnable<Boolean> cbi) {
    	user = null;
    	if (!world.isClientSide)
    		setPlayer((ServerPlayer) player);
    }

    @Inject(at = @At("RETURN"), remap = false, method = "form", locals = LocalCapture.CAPTURE_FAILHARD)
    public void fh$on$form(Level world, BlockPos pos, Rotation rot, Mirror mirror, Direction sideHit, CallbackInfo cbi, BlockPos master) {
        if (user != null) {
            IOwnerTile.trySetOwner(Utils.getExistingTileEntity(world, master), user);
            CMultiblockHelper.getBEHelperOptional(world, master).ifPresent(t->{
            	if(t.getState() instanceof IOwnerState state) {
            		state.setOwner(user);
            	}
            	if(t.getMultiblock().logic() instanceof OwnedLogic logic){
            		logic.onOwnerChange(t.getContext());
            	}
            });
        }
    }

	@Override
	public void setPlayer(ServerPlayer spe) {
		user= CTeamDataManager.get(spe).getId();
	}
	@Override
	public void setUUID(UUID id) {
		user=id;
	}
}
