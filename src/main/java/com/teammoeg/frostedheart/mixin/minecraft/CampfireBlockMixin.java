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

import com.teammoeg.frostedheart.content.research.ResearchListeners;
import com.teammoeg.frostedheart.util.lang.Lang;
import com.teammoeg.frostedheart.util.mixin.ICampfireExtra;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.items.ItemHandlerHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * Add time limit for campfire
 * <p>
 * */
@Mixin(CampfireBlock.class)
public abstract class CampfireBlockMixin extends BaseEntityBlock {
    public CampfireBlockMixin(Properties builder) {
        super(builder);
    }

    @Inject(at = @At("RETURN"), method = "getStateForPlacement", cancellable = true)
    public void getStateForPlacement(BlockPlaceContext context, CallbackInfoReturnable<BlockState> callbackInfo) {
        callbackInfo.setReturnValue(callbackInfo.getReturnValue().setValue(CampfireBlock.LIT, false));
    }

    /**
     * @author dashuaibia
     * @reason ignition
     */
    @Overwrite
    public InteractionResult use(BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn, BlockHitResult hit) {
        if (handIn == InteractionHand.MAIN_HAND && !player.isShiftKeyDown()) {
            BlockEntity tileentity = worldIn.getBlockEntity(pos);
            if (tileentity instanceof CampfireBlockEntity) {
                CampfireBlockEntity campfiretileentity = (CampfireBlockEntity) tileentity;
                ItemStack itemstack = player.getItemInHand(handIn);
                RandomSource rand = worldIn.random;
                if (!worldIn.isClientSide) {
                    if (!player.getMainHandItem().isEmpty()) {
                        // moved to events
//                        if (CampfireBlock.canLight(state)) {
//                            if (itemstack.getItem() == Items.FLINT && player.getOffhandItem().getItem() == Items.FLINT) {
//                                player.swing(InteractionHand.MAIN_HAND);
//                                if (rand.nextFloat() < 0.33) {
//                                    worldIn.setBlock(pos, state.setValue(BlockStateProperties.LIT, Boolean.TRUE), 3);
//                                }
//
//                                worldIn.playSound(null, pos, SoundEvents.STONE_STEP, SoundSource.BLOCKS, 1.0F, 2F + rand.nextFloat() * 0.4F);
//
//                                return InteractionResult.SUCCESS;
//                            }
//                        }
                        Optional<CampfireCookingRecipe> optional = campfiretileentity.getCookableRecipe(itemstack);
                        if (optional.isPresent()) {
                            if (ResearchListeners.canUseRecipe(player, optional.get()) && campfiretileentity.placeFood(player,player.getAbilities().instabuild ? itemstack.copy() : itemstack, optional.get().getCookingTime())) {
                                player.awardStat(Stats.INTERACT_WITH_CAMPFIRE);
                                return InteractionResult.CONSUME;
                            }
                        }

                    } else {
                        ICampfireExtra info = (ICampfireExtra) campfiretileentity;
                        if (state.getValue(CampfireBlock.LIT)) {
                            player.displayClientMessage(Lang.translateMessage("campfire.remaining", Integer.toString(info.getLifeTime() / 20)), true);
                        } else if (info.getLifeTime() > 0) {
                            player.displayClientMessage(Lang.translateMessage("campfire.ignition"), true);
                        } else {
                            player.displayClientMessage(Lang.translateMessage("campfire.fuel"), true);
                        }
                        return InteractionResult.SUCCESS;
                    }
                }
                // moved to events
                /*
                else {
                    if (!player.getMainHandItem().isEmpty()) {
                        if (CampfireBlock.canLight(state)) {
                            if (itemstack.getItem() == Items.FLINT && player.getOffhandItem().getItem() == Items.FLINT) {
                                for (int i = 0; i < 5; i++) {
                                    worldIn.addParticle(ParticleTypes.SMOKE, player.getX() + player.getLookAngle().x() + rand.nextFloat() * 0.25, player.getY() + 0.5f + rand.nextFloat() * 0.25, player.getZ() + player.getLookAngle().z() + rand.nextFloat() * 0.25, 0, 0.01, 0);
                                }
                                worldIn.addParticle(ParticleTypes.FLAME, player.getX() + player.getLookAngle().x() + rand.nextFloat() * 0.25, player.getY() + 0.5f + rand.nextFloat() * 0.25, player.getZ() + player.getLookAngle().z() + rand.nextFloat() * 0.25, 0, 0.01, 0);
                                return InteractionResult.SUCCESS;
                            }
                        }
                    }
                }
                 */
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public void stepOn(Level worldIn, BlockPos pos, BlockState state, Entity entityIn) {
        if (entityIn instanceof ItemEntity) {
            ItemEntity item = (ItemEntity) entityIn;
            int rawBurnTime = ForgeHooks.getBurnTime(item.getItem(),RecipeType.CAMPFIRE_COOKING);
            if (worldIn.isClientSide && CampfireBlock.isLitCampfire(state) && rawBurnTime > 0)
                worldIn.addParticle(ParticleTypes.SMOKE, entityIn.getX(), entityIn.getY() + 0.25D, entityIn.getZ(), 0, 0.05D, 0);
            if (!worldIn.isClientSide) {
                if (rawBurnTime > 0) {
                    if (item.getOwner() != null && ((ICampfireExtra) worldIn.getBlockEntity(pos)).getLifeTime() != -1337) {
                        ItemStack is = item.getItem();
                        CampfireBlockEntity tileEntity = (CampfireBlockEntity) worldIn.getBlockEntity(pos);
                        ICampfireExtra lifeTime = ((ICampfireExtra) tileEntity);
                        int maxcs = (19200 - lifeTime.getLifeTime()) / rawBurnTime / 3;
                        int rcs = Math.min(maxcs, is.getCount());
                        int burnTime = rawBurnTime * 3 * rcs;
                        ItemStack container = is.getCraftingRemainingItem();
                        is.shrink(rcs);
                        lifeTime.addLifeTime(burnTime);

                        if (rcs > 0 && !container.isEmpty())
                            Containers.dropItemStack(worldIn, pos.getX(), pos.getY(), pos.getZ(), ItemHandlerHelper.copyStackWithSize(container, rcs));
                        if (is.getCount() <= 0)
                            entityIn.remove(RemovalReason.DISCARDED);
                    }
                }
            }
        }
    }
}