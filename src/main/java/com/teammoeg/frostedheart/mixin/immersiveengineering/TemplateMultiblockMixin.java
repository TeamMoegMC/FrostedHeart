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

package com.teammoeg.frostedheart.mixin.immersiveengineering;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import com.teammoeg.frostedheart.base.team.TeamDataManager;
import com.teammoeg.frostedheart.util.mixin.IOwnerTile;
import com.teammoeg.frostedheart.util.mixin.MultiBlockAccess;

import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler.IMultiblock;
import blusunrize.immersiveengineering.api.multiblocks.TemplateMultiblock;
import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

@Mixin(TemplateMultiblock.class)
public abstract class TemplateMultiblockMixin implements IMultiblock, MultiBlockAccess {
    private ServerPlayerEntity pe;

    public TemplateMultiblockMixin() {
    }

    @Invoker(remap = false)
    public abstract void callForm(World world, BlockPos pos, Rotation rot, Mirror mirror, Direction sideHit);

    @Inject(at = @At(value = "INVOKE", target = "Lblusunrize/immersiveengineering/api/multiblocks/TemplateMultiblock;form"), method = "createStructure", remap = false)
    public void fh$on$createStructure(World world, BlockPos pos, Direction side, PlayerEntity player, CallbackInfoReturnable<Boolean> cbi) {
    	pe = null;
    	if (!world.isRemote)
            pe = (ServerPlayerEntity) player;
    }

    @Inject(at = @At("RETURN"), remap = false, method = "form", locals = LocalCapture.CAPTURE_FAILHARD)
    public void fh$on$form(World world, BlockPos pos, Rotation rot, Mirror mirror, Direction sideHit, CallbackInfo cbi, BlockPos master) {
        if (pe != null)
            IOwnerTile.trySetOwner(Utils.getExistingTileEntity(world, master), TeamDataManager.get(pe).getId());
    }
}
