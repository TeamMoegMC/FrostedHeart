/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.block.campfire;

import com.teammoeg.frostedheart.util.Lang;
import com.teammoeg.frostedheart.util.mixin.ICampfireExtra;
import com.teammoeg.frostedresearch.ResearchHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.stats.Stats;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.items.ItemHandlerHelper;

/** Shared campfire gameplay; the real block entity remains the fuel owner. */
public final class CampfireGameplay {
    private static final int MAX_FUEL_TICKS = 19_200;
    private static final int BURN_MULTIPLIER = 3;

    private CampfireGameplay() {}

    // Keep integer division order and let each existing insertion path decide
    // whether a nonpositive capacity is rejected.
    public static int capacityForFuel(int remainingTicks, int rawBurnTime) {
        return (MAX_FUEL_TICKS - remainingTicks) / rawBurnTime / BURN_MULTIPLIER;
    }

    public static int ticksForFuel(int rawBurnTime, int count) {
        return rawBurnTime * BURN_MULTIPLIER * count;
    }

    public static InteractionResult use(BlockState state, Level level, BlockPos position,
            Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || player.isShiftKeyDown()) return InteractionResult.PASS;
        if (!(level.getBlockEntity(position) instanceof CampfireBlockEntity campfire)) return InteractionResult.PASS;
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.isEmpty()) {
            var recipe = campfire.getCookableRecipe(stack);
            if (recipe.isPresent() && ResearchHooks.canUseRecipe(player, recipe.get())
                    && campfire.placeFood(player, player.getAbilities().instabuild ? stack.copy() : stack,
                            recipe.get().getCookingTime())) {
                player.awardStat(Stats.INTERACT_WITH_CAMPFIRE);
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        } else if (!level.isClientSide) {
            var fuel = (ICampfireExtra) campfire;
            if (state.getValue(CampfireBlock.LIT)) {
                player.displayClientMessage(Lang.translateMessage("campfire.remaining",
                        Integer.toString(fuel.getLifeTime() / 20)), true);
            } else if (fuel.getLifeTime() > 0) {
                player.displayClientMessage(Lang.translateMessage("campfire.ignition"), true);
            } else {
                player.displayClientMessage(Lang.translateMessage("campfire.fuel"), true);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    public static void stepOn(Level level, BlockPos position, BlockState state, Entity entity) {
        if (!(entity instanceof ItemEntity item)) return;
        int rawBurnTime = ForgeHooks.getBurnTime(item.getItem(), RecipeType.CAMPFIRE_COOKING);
        if (level.isClientSide && CampfireBlock.isLitCampfire(state) && rawBurnTime > 0)
            level.addParticle(ParticleTypes.SMOKE, entity.getX(), entity.getY() + 0.25D, entity.getZ(), 0, 0.05D, 0);
        if (level.isClientSide || rawBurnTime <= 0 || item.getOwner() == null) return;
        var campfire = (CampfireBlockEntity) level.getBlockEntity(position);
        var fuel = (ICampfireExtra) campfire;
        if (fuel.getLifeTime() == -1337) return;
        ItemStack stack = item.getItem();
        int accepted = Math.min(capacityForFuel(fuel.getLifeTime(), rawBurnTime), stack.getCount());
        ItemStack container = stack.getCraftingRemainingItem();
        stack.shrink(accepted);
        fuel.addLifeTime(ticksForFuel(rawBurnTime, accepted));
        if (accepted > 0 && !container.isEmpty())
            Containers.dropItemStack(level, position.getX(), position.getY(), position.getZ(),
                    ItemHandlerHelper.copyStackWithSize(container, accepted));
        if (stack.getCount() <= 0) entity.remove(Entity.RemovalReason.DISCARDED);
    }
}
