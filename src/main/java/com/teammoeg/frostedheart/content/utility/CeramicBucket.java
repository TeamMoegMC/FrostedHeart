/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.utility;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.teammoeg.frostedheart.item.FHBaseItem;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStackSimple;

public class CeramicBucket extends FHBaseItem {
	public CeramicBucket(Properties properties) {
		super(properties);
	}

	private ItemStack emptyBucket(ItemStack stack, Player playerIn) {
		if (playerIn.getAbilities().instabuild) {
			return stack;
		}
		ItemStack emptyStack = stack.copy();
		emptyStack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).ifPresent(handler -> handler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE));
		return emptyStack;
	}

	private Fluid getFluid(ItemStack stack) {
		return stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).map(handler -> handler.getFluidInTank(0).getFluid()).orElse(Fluids.EMPTY);
	}

	@Override
	public int getMaxStackSize(ItemStack stack) {
		return this.getFluid(stack) == Fluids.EMPTY ? 16 : 1;
	}

	@Override
	public net.minecraftforge.common.capabilities.ICapabilityProvider initCapabilities(ItemStack stack, @Nullable net.minecraft.nbt.CompoundTag nbt) {
		return new FluidHandlerItemStackSimple(stack, 1000) {
			@Override
			public boolean canFillFluidType(FluidStack fluid) {
				return isFluidValid(0, fluid);
			}

			@Override
			public boolean isFluidValid(int tank, @Nonnull FluidStack stack) {
				return !stack.getFluid().getFluidType().isLighterThanAir();
			}

		};
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pHand) {
		ItemStack itemstack = pPlayer.getItemInHand(pHand);
		Fluid content = itemstack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM)
				.map(t -> t.getFluidInTank(0).getFluid())
				.orElse(Fluids.EMPTY);
		BlockHitResult blockhitresult = getPlayerPOVHitResult(pLevel, pPlayer,
				content == Fluids.EMPTY ? ClipContext.Fluid.SOURCE_ONLY : ClipContext.Fluid.NONE);

		InteractionResultHolder<ItemStack> ret = net.minecraftforge.event.ForgeEventFactory.onBucketUse(pPlayer, pLevel, itemstack, blockhitresult);
		if (ret != null) return ret;

		if (blockhitresult.getType() == HitResult.Type.BLOCK) {
			BlockPos blockpos = blockhitresult.getBlockPos();
			Direction direction = blockhitresult.getDirection();
			BlockPos blockpos1 = blockpos.relative(direction);

			if (pLevel.mayInteract(pPlayer, blockpos) && pPlayer.mayUseItemAt(blockpos1, direction, itemstack)) {
				if (content == Fluids.EMPTY) {
					BlockState blockstate = pLevel.getBlockState(blockpos);
					if (blockstate.getBlock() instanceof BucketPickup) {
						BucketPickup bucketpickup = (BucketPickup) blockstate.getBlock();
						ItemStack pickedUpVanilla = bucketpickup.pickupBlock(pLevel, blockpos, blockstate);

						if (!pickedUpVanilla.isEmpty() && pickedUpVanilla.getItem() instanceof BucketItem) {
							// Get fluid from vanilla bucket
							Fluid fluid = ((BucketItem) pickedUpVanilla.getItem()).getFluid();

							if (fluid.isSame(Fluids.WATER) || fluid.isSame(Fluids.LAVA)) {
								// Create new ceramic bucket with fluid
								ItemStack filledCeramic = itemstack.copyWithCount(1);
								filledCeramic.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).ifPresent(handler -> {
									handler.fill(new FluidStack(fluid, 1000), IFluidHandler.FluidAction.EXECUTE);
								});

								// Play effects
								pPlayer.awardStat(Stats.ITEM_USED.get(this));
								bucketpickup.getPickupSound(blockstate).ifPresent(sound ->
										pPlayer.playSound(sound, 1.0F, 1.0F));
								pLevel.gameEvent(pPlayer, GameEvent.FLUID_PICKUP, blockpos);

								// Return modified ceramic bucket
								ItemStack resultStack = ItemUtils.createFilledResult(itemstack, pPlayer, filledCeramic,false);
								if (!pLevel.isClientSide) {
									CriteriaTriggers.FILLED_BUCKET.trigger((ServerPlayer) pPlayer, filledCeramic);
								}
								return InteractionResultHolder.sidedSuccess(resultStack, pLevel.isClientSide());
							}
						}
					}
					return InteractionResultHolder.fail(itemstack);
				} else {
					// Existing fluid placement logic
					BlockState blockstate = pLevel.getBlockState(blockpos);
					BlockPos targetPos = canBlockContainFluid(pLevel, blockpos, blockstate, content) ?
							blockpos : blockpos1;

					if (this.emptyContents(pPlayer, pLevel, targetPos, blockhitresult, itemstack, content)) {
						this.checkExtraContent(pPlayer, pLevel, itemstack, targetPos);
						if (pPlayer instanceof ServerPlayer) {
							CriteriaTriggers.PLACED_BLOCK.trigger((ServerPlayer) pPlayer, targetPos, itemstack);
						}
						pPlayer.awardStat(Stats.ITEM_USED.get(this));
						return InteractionResultHolder.sidedSuccess(emptyBucket(itemstack, pPlayer), pLevel.isClientSide());
					}
					return InteractionResultHolder.fail(itemstack);
				}
			}
		}
		return InteractionResultHolder.pass(itemstack);
	}

	public boolean emptyContents(@Nullable Player pPlayer, Level pLevel, BlockPos pPos, @Nullable BlockHitResult pResult, @Nullable ItemStack container,Fluid content) {
		if (!(content instanceof FlowingFluid)) {
			return false;
		} else {
			BlockState blockstate = pLevel.getBlockState(pPos);
			Block block = blockstate.getBlock();
			boolean flag = blockstate.canBeReplaced(content);
			boolean flag1 = blockstate.isAir() || flag || block instanceof LiquidBlockContainer && ((LiquidBlockContainer) block).canPlaceLiquid(pLevel, pPos, blockstate, content);
			java.util.Optional<net.minecraftforge.fluids.FluidStack> containedFluidStack = java.util.Optional.ofNullable(container).flatMap(net.minecraftforge.fluids.FluidUtil::getFluidContained);
			if (!flag1) {
				return pResult != null && this.emptyContents(pPlayer, pLevel, pResult.getBlockPos().relative(pResult.getDirection()), (BlockHitResult) null, container,content);
			} else if (containedFluidStack.isPresent() && content.getFluidType().isVaporizedOnPlacement(pLevel, pPos, containedFluidStack.get())) {
				content.getFluidType().onVaporize(pPlayer, pLevel, pPos, containedFluidStack.get());
				return true;
			} else if (pLevel.dimensionType().ultraWarm() && content.is(FluidTags.WATER)) {
				int i = pPos.getX();
				int j = pPos.getY();
				int k = pPos.getZ();
				pLevel.playSound(pPlayer, pPos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F + (pLevel.random.nextFloat() - pLevel.random.nextFloat()) * 0.8F);

				for (int l = 0; l < 8; ++l) {
					pLevel.addParticle(ParticleTypes.LARGE_SMOKE, (double) i + Math.random(), (double) j + Math.random(), (double) k + Math.random(), 0.0D, 0.0D, 0.0D);
				}

				return true;
			} else if (block instanceof LiquidBlockContainer && ((LiquidBlockContainer) block).canPlaceLiquid(pLevel, pPos, blockstate, content)) {
				((LiquidBlockContainer) block).placeLiquid(pLevel, pPos, blockstate, ((FlowingFluid) content).getSource(false));
				this.playEmptySound(pPlayer, pLevel, pPos,content);
				return true;
			} else {
				if (!pLevel.isClientSide && flag && !blockstate.liquid()) {
					pLevel.destroyBlock(pPos, true);
				}

				if (!pLevel.setBlock(pPos, content.defaultFluidState().createLegacyBlock(), 11) && !blockstate.getFluidState().isSource()) {
					return false;
				} else {
					this.playEmptySound(pPlayer, pLevel, pPos,content);
					return true;
				}
			}
		}
	}

	protected void playEmptySound(@Nullable Player pPlayer, LevelAccessor pLevel, BlockPos pPos, Fluid content) {
		SoundEvent soundevent = content.getFluidType().getSound(pPlayer, pLevel, pPos, net.minecraftforge.common.SoundActions.BUCKET_EMPTY);
		if (soundevent == null) soundevent = content.is(FluidTags.LAVA) ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.BUCKET_EMPTY;
		pLevel.playSound(pPlayer, pPos, soundevent, SoundSource.BLOCKS, 1.0F, 1.0F);
		pLevel.gameEvent(pPlayer, GameEvent.FLUID_PLACE, pPos);
	}

	protected boolean canBlockContainFluid(Level worldIn, BlockPos posIn, BlockState blockstate,Fluid content) {
		return blockstate.getBlock() instanceof LiquidBlockContainer && ((LiquidBlockContainer) blockstate.getBlock()).canPlaceLiquid(worldIn, posIn, blockstate, content);
	}

	public static ItemStack getEmptySuccessItem(ItemStack pBucketStack, Player pPlayer) {
		return !pPlayer.getAbilities().instabuild ? new ItemStack(Items.BUCKET) : pBucketStack;
	}

	public void checkExtraContent(@Nullable Player pPlayer, Level pLevel, ItemStack pContainerStack, BlockPos pPos) {
	}
}
