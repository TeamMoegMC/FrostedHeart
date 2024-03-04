/*
 * Copyright (c) 2021-2024 TeamMoeg
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

import java.util.Optional;
import java.util.Random;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.teammoeg.frostedheart.content.research.ResearchListeners;
import com.teammoeg.frostedheart.util.client.GuiUtils;
import com.teammoeg.frostedheart.util.mixin.ICampfireExtra;

import net.minecraft.block.BlockState;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.ContainerBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.CampfireCookingRecipe;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.stats.Stats;
import net.minecraft.tileentity.CampfireTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.items.ItemHandlerHelper;
/**
 * Add time limit for campfire
 * 
 * */
@Mixin(CampfireBlock.class)
public abstract class CampfireBlockMixin extends ContainerBlock {
    @Shadow
    public static boolean isLit(BlockState state) {
        return false;
    }

    public CampfireBlockMixin(Properties builder) {
        super(builder);
    }

    @Inject(at = @At("RETURN"), method = "getStateForPlacement", cancellable = true)
    public void getStateForPlacement(BlockItemUseContext context, CallbackInfoReturnable<BlockState> callbackInfo) {
        callbackInfo.setReturnValue(callbackInfo.getReturnValue().with(CampfireBlock.LIT, false));
    }

    /**
     * @author dashuaibia
     * @reason ignition
     */
    @Overwrite
    public ActionResultType onBlockActivated(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult hit) {
        if (handIn == Hand.MAIN_HAND && !player.isSneaking()) {
            TileEntity tileentity = worldIn.getTileEntity(pos);
            if (tileentity instanceof CampfireTileEntity) {
                CampfireTileEntity campfiretileentity = (CampfireTileEntity) tileentity;
                ItemStack itemstack = player.getHeldItem(handIn);
                Random rand = worldIn.rand;
                if (!worldIn.isRemote) {
                    if (!player.getHeldItemMainhand().isEmpty()) {
                        if (CampfireBlock.canBeLit(state)) {
                            if (itemstack.getItem() == Items.FLINT && player.getHeldItemOffhand().getItem() == Items.FLINT) {
                                player.swingArm(Hand.MAIN_HAND);
                                if (rand.nextFloat() < 0.33) {
                                    worldIn.setBlockState(pos, state.with(BlockStateProperties.LIT, Boolean.valueOf(true)), 3);
                                }

                                worldIn.playSound(null, pos, SoundEvents.BLOCK_STONE_STEP, SoundCategory.BLOCKS, 1.0F, 2F + rand.nextFloat() * 0.4F);

                                return ActionResultType.SUCCESS;
                            }
                        }
                        Optional<CampfireCookingRecipe> optional = campfiretileentity.findMatchingRecipe(itemstack);
                        if (optional.isPresent()) {
                            if (ResearchListeners.canUseRecipe(player, optional.get()) && campfiretileentity.addItem(player.abilities.isCreativeMode ? itemstack.copy() : itemstack, optional.get().getCookTime())) {
                                player.addStat(Stats.INTERACT_WITH_CAMPFIRE);
                                return ActionResultType.CONSUME;
                            }
                        }

                    } else {
                        ICampfireExtra info = (ICampfireExtra) campfiretileentity;
                        if (state.get(CampfireBlock.LIT)) {
                            player.sendStatusMessage(GuiUtils.translateMessage("campfire.remaining", Integer.toString(info.getLifeTime() / 20)), true);
                        } else if (info.getLifeTime() > 0) {
                            player.sendStatusMessage(GuiUtils.translateMessage("campfire.ignition"), true);
                        } else {
                            player.sendStatusMessage(GuiUtils.translateMessage("campfire.fuel"), true);
                        }
                        return ActionResultType.SUCCESS;
                    }
                } else {
                    if (!player.getHeldItemMainhand().isEmpty()) {
                        if (CampfireBlock.canBeLit(state)) {
                            if (itemstack.getItem() == Items.FLINT && player.getHeldItemOffhand().getItem() == Items.FLINT) {
                                for (int i = 0; i < 5; i++) {
                                    worldIn.addParticle(ParticleTypes.SMOKE, player.getPosX() + player.getLookVec().getX() + rand.nextFloat() * 0.25, player.getPosY() + 0.5f + rand.nextFloat() * 0.25, player.getPosZ() + player.getLookVec().getZ() + rand.nextFloat() * 0.25, 0, 0.01, 0);
                                }
                                worldIn.addParticle(ParticleTypes.FLAME, player.getPosX() + player.getLookVec().getX() + rand.nextFloat() * 0.25, player.getPosY() + 0.5f + rand.nextFloat() * 0.25, player.getPosZ() + player.getLookVec().getZ() + rand.nextFloat() * 0.25, 0, 0.01, 0);
                                return ActionResultType.SUCCESS;
                            }
                        }
                    }
                }
            }
        }
        return ActionResultType.PASS;
    }

    @Inject(at = @At("HEAD"), method = "onEntityCollision")
    public void onEntityCollision(BlockState state, World worldIn, BlockPos pos, Entity entityIn, CallbackInfo callbackInfo) {
        if (entityIn instanceof ItemEntity) {
            ItemEntity item = (ItemEntity) entityIn;
            int rawBurnTime = ForgeHooks.getBurnTime(item.getItem());
            if (worldIn.isRemote && isLit(state) && rawBurnTime > 0)
                worldIn.addParticle(ParticleTypes.SMOKE, entityIn.getPosX(), entityIn.getPosY() + 0.25D, entityIn.getPosZ(), 0, 0.05D, 0);
            if (!worldIn.isRemote) {
                if (rawBurnTime > 0) {
                    if (item.getThrowerId() != null && ((ICampfireExtra) worldIn.getTileEntity(pos)).getLifeTime() != -1337) {
                        ItemStack is = item.getItem();
                        CampfireTileEntity tileEntity = (CampfireTileEntity) worldIn.getTileEntity(pos);
                        ICampfireExtra lifeTime = ((ICampfireExtra) tileEntity);
                        int maxcs = (19200 - lifeTime.getLifeTime()) / rawBurnTime / 3;
                        int rcs = Math.min(maxcs, is.getCount());
                        int burnTime = rawBurnTime * 3 * rcs;
                        ItemStack container = is.getContainerItem();
                        is.shrink(rcs);
                        lifeTime.addLifeTime(burnTime);

                        if (rcs > 0 && !container.isEmpty())
                            InventoryHelper.spawnItemStack(worldIn, pos.getX(), pos.getY(), pos.getZ(), ItemHandlerHelper.copyStackWithSize(container, rcs));
                        if (is.getCount() <= 0)
                            entityIn.remove();
                    }
                }
            }
        }
    }
}