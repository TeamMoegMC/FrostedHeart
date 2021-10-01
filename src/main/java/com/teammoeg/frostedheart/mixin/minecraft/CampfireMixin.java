/*
 * Copyright (c) 2021 TeamMoeg
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
 */

package com.teammoeg.frostedheart.mixin.minecraft;

import com.teammoeg.frostedheart.bridge.ICampfireExtra;
import net.minecraft.block.BlockState;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.ContainerBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.tileentity.CampfireTileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

@Mixin(CampfireBlock.class)
public abstract class CampfireMixin extends ContainerBlock {
    private static boolean isSoul(BlockState state) {
        return (state.getBlock().getRegistryName().toString().indexOf("soul") != -1) || (state.getBlock().getRegistryName().toString().indexOf("ender") != -1);
    }

    @Shadow
    public static boolean isLit(BlockState state) {
        return false;
    }

    protected CampfireMixin(Properties builder) {
        super(builder);
    }

    @Inject(at = @At("RETURN"), method = "getStateForPlacement(Lnet/minecraft/item/BlockItemUseContext;)Lnet/minecraft/block/BlockState;", cancellable = true)
    protected void getStateForPlacement(BlockItemUseContext context, CallbackInfoReturnable<BlockState> callbackInfo) {

        if (isSoul(getDefaultState())) {
            callbackInfo.setReturnValue(callbackInfo.getReturnValue().with(CampfireBlock.LIT, callbackInfo.getReturnValue().get(CampfireBlock.LIT)));
        } else
            callbackInfo.setReturnValue(callbackInfo.getReturnValue().with(CampfireBlock.LIT, callbackInfo.getReturnValue().get(CampfireBlock.LIT)));
    }

    @Inject(at = @At("HEAD"), method = "onEntityCollision(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/Entity;)V", cancellable = true)
    protected void onEntityCollision(BlockState state, World worldIn, BlockPos pos, Entity entityIn, CallbackInfo callbackInfo) {
        if (entityIn instanceof ItemEntity) {
            Random rand = worldIn.rand;
            int rawBurnTime = ForgeHooks.getBurnTime(((ItemEntity) entityIn).getItem());
            if (worldIn.isRemote && isLit(state) && rawBurnTime > 0)
                worldIn.addParticle(ParticleTypes.SMOKE, entityIn.getPosX(), entityIn.getPosY() + 0.25D, entityIn.getPosZ(), 0, 0.05D, 0);

            if (rawBurnTime > 0) {
                if (((ItemEntity) entityIn).getThrowerId() != null && ((ICampfireExtra) worldIn.getTileEntity(pos)).getLifeTime() != -1337) {
                    if (!worldIn.isRemote) {
                        int burnTime = rawBurnTime * 2000 * ((ItemEntity) entityIn).getItem().getCount();
                        CampfireTileEntity tileEntity = (CampfireTileEntity) worldIn.getTileEntity(pos);
                        ICampfireExtra lifeTime = ((ICampfireExtra) tileEntity);
                        System.out.println(lifeTime.getLifeTime());
                        if (isSoul(state)) {
                            if (lifeTime.getLifeTime() < 4000 && lifeTime.getLifeTime() >= 0) {
                                lifeTime.addLifeTime(burnTime);
                                if (((ItemEntity) entityIn).getItem().getItem() == Items.LAVA_BUCKET)
                                    InventoryHelper.spawnItemStack(worldIn, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(Items.BUCKET));
                                entityIn.remove();
                            }
                        } else {
                            if (lifeTime.getLifeTime() < 2147483647 && lifeTime.getLifeTime() >= 0) {
                                lifeTime.addLifeTime(burnTime);
                                if (((ItemEntity) entityIn).getItem().getItem() == Items.LAVA_BUCKET)
                                    InventoryHelper.spawnItemStack(worldIn, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(Items.BUCKET));
                                entityIn.remove();
                            }
                        }
                    }
                }
            }
        }
    }
}