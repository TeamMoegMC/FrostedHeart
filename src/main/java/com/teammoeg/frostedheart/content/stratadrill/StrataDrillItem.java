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

package com.teammoeg.frostedheart.content.stratadrill;

import com.teammoeg.chorda.creativeTab.CreativeTabItemHelper;
import com.teammoeg.chorda.creativeTab.ICreativeModeTabItem;
import com.teammoeg.frostedheart.bootstrap.client.FHTabs;
import com.teammoeg.frostedheart.content.stratadrill.client.StrataDrillHands;
import com.teammoeg.frostedheart.mixin.minecraft.accessors.BlockStateBaseAccessor_StrataHardness;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.common.TierSortingRegistry;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.function.Consumer;

/** The FH drill. Optional workbenches may edit its assembly, but never implement its mining rules. */
@ParametersAreNonnullByDefault
public class StrataDrillItem extends Item implements ICreativeModeTabItem {
    public static final String ID = "stratadrill";
    private static final int FUEL_BAR_WIDTH = 13;
    private static final int FUEL_BAR_COLOR = 0xB7C8CE;
    private static final float HARD_ROCK_START = 1.5F;
    private static final float HARD_ROCK_FULL = 3F;
    private static final float HARD_ROCK_MAX_BONUS = 0.30F;

    public StrataDrillItem() {
        // Marks the item as damageable and non-stackable. Stack-aware callers use the actual cutter below.
        super(new Properties().durability(StrataDrillMaterials.get(StrataDrillMaterials.IRON).durability()));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(StrataDrillHands.extension());
    }

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = new ItemStack(this);
        StrataDrillData.ensureIdentity(stack);
        return stack;
    }

    @Override
    public void fillItemCategory(CreativeTabItemHelper helper) {
        // The client copies this palette entry for each pick. Give each real copy its own ID on use.
        if (helper.isType(FHTabs.TOOLS)) helper.accept(new ItemStack(this));
    }

    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player player) {
        if (!level.isClientSide) StrataDrillData.ensureIdentity(stack);
    }

    @Override
    public Component getName(ItemStack stack) {
        boolean steam = StrataDrillData.assembly(StrataDrillData.read(stack)).steam();
        return Component.translatable(getDescriptionId() + (steam ? ".steam" : ".diesel"));
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag unused) {
        return StrataDrillPower.makeCapabilities(stack);
    }

    /** Give a real fluid port first refusal, before its block opens a menu or handles the click. */
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        return StrataDrillPower.refuelDiesel(stack, context);
    }

    static boolean cutterCanRun(@Nullable CompoundTag data) {
        int wear = StrataDrillData.wear(data);
        // All material capacities are at least two. Unworn stock need not resolve the material.
        return wear == 0 || wear < cutter(data).durability() - 1;
    }

    private static StrataDrillMaterials.Cutter cutter(@Nullable CompoundTag data) {
        return StrataDrillMaterials.get(StrataDrillData.storedCutter(data));
    }

    /** Head speed before the drive and target bonuses, Efficiency, and other player modifiers. */
    public static float nominalHeadSpeed(ItemStack stack) {
        CompoundTag data = StrataDrillData.read(stack);
        return nominalHeadSpeed(cutter(data), StrataDrillData.assembly(data));
    }

    private static float nominalHeadSpeed(StrataDrillMaterials.Cutter cutter, StrataDrillAssembly assembly) {
        return Math.max(1F, cutter.efficiency() + (assembly.wheel() ? -1.5F : 3F));
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (!state.is(BlockTags.MINEABLE_WITH_PICKAXE)) return 1F;
        CompoundTag data = StrataDrillData.read(stack);
        StrataDrillAssembly powered = StrataDrillPower.poweredAssembly(data, false);
        if (powered == null) return 1F;
        StrataDrillMaterials.Cutter cutter = cutter(data);
        float speed = nominalHeadSpeed(cutter, powered);
        if (!powered.steam() && speed > 1F) speed *= StrataDrillPower.DIESEL_SPEED_MULTIPLIER;
        if (powered.wheel()) return speed;

        // The item's base speed has no world or position. Read the same baked hardness as
        // Forge 47.3.0's BlockState, without fake context or a global player-speed hook.
        float hardness = ((BlockStateBaseAccessor_StrataHardness) (Object) state).fh$getBakedHardness();
        if (!(hardness > HARD_ROCK_START) || !Float.isFinite(hardness)
                || (state.requiresCorrectToolForDrops() && !hasMiningTier(state, cutter))) return speed;
        float ramp = Math.min(1F, (hardness - HARD_ROCK_START) / (HARD_ROCK_FULL - HARD_ROCK_START));
        return speed * (1F + ramp * HARD_ROCK_MAX_BONUS);
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ToolAction action) {
        return action == ToolActions.PICKAXE_DIG && StrataDrillPower.canRun(stack, false);
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        CompoundTag data = StrataDrillData.read(stack);
        if (StrataDrillPower.poweredAssembly(data, false) == null) return false;
        if (!state.requiresCorrectToolForDrops()) return true;
        return state.is(BlockTags.MINEABLE_WITH_PICKAXE) && hasMiningTier(state, cutter(data));
    }

    private static boolean hasMiningTier(BlockState state, StrataDrillMaterials.Cutter cutter) {
        return TierSortingRegistry.isCorrectTierForDrops(harvestTier(cutter.tier()), state);
    }

    /** FH levels are vanilla material levels, not ordered indices in Forge (or in a workbench's tier list). */
    public static Tiers harvestTier(int level) {
        return switch (level) {
            case 0 -> Tiers.WOOD;
            case 1 -> Tiers.STONE;
            case 3 -> Tiers.DIAMOND;
            case 4 -> Tiers.NETHERITE;
            default -> Tiers.IRON;
        };
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return StrataDrillMaterials.get(StrataDrillData.cutterId(stack)).durability();
    }

    @Override
    public int getDamage(ItemStack stack) {
        CompoundTag data = StrataDrillData.read(stack);
        int wear = StrataDrillData.wear(data);
        return wear == 0 ? 0 : Math.min(wear, StrataDrillMaterials.get(StrataDrillData.storedCutter(data)).durability() - 1);
    }

    @Override
    public void setDamage(ItemStack stack, int damage) {
        CompoundTag data = StrataDrillData.read(stack);
        int stored = StrataDrillData.wear(data);
        if (stored == damage) return; // Forge calls setDamage(getDamage()) on stack construction and load.
        String cutter = StrataDrillData.storedCutter(data);
        int limit = StrataDrillMaterials.get(cutter).durability() - 1;
        // The catalog may arrive after a saved/network stack, even for a pack's override of iron.
        // Writing back the currently visible damage must not erase excess wear from the other catalog.
        int wear = Mth.clamp(damage, 0, limit);
        if (Math.min(stored, limit) != wear) StrataDrillData.setWear(stack, wear);
    }

    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T owner, Consumer<T> onBroken) {
        // ItemStack.hurt tests the requested total, not the result of setDamage. Cap both paths so
        // even a multi-point hit cannot remove a drill at the worn-out threshold.
        return Math.min(amount, stack.getMaxDamage() - 1 - stack.getDamageValue());
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
        if (!StrataDrillPower.canRun(stack, false)) return false;
        if (!level.isClientSide && state.getDestroySpeed(level, pos) > 0) {
            // Vanilla handles Creative, Unbreakable, Unbreaking and durability advancement criteria.
            stack.hurtAndBreak(1, miner, owner -> owner.broadcastBreakEvent(EquipmentSlot.MAINHAND));
            if (stack.getDamageValue() == stack.getMaxDamage() - 1) {
                miner.broadcastBreakEvent(EquipmentSlot.MAINHAND);
                miner.playSound(SoundEvents.SHIELD_BREAK, 1F, 1F);
            }
        }
        return true;
    }

    @Override
    public int getEnchantmentLevel(ItemStack stack, Enchantment enchantment) {
        int level = super.getEnchantmentLevel(stack, enchantment);
        // Forge's break event asks about XP before the game mode selects its bare-hand loot copy.
        if (level > 0 && (enchantment == Enchantments.BLOCK_FORTUNE || enchantment == Enchantments.SILK_TOUCH)
                && !StrataDrillPower.canRun(stack, false)) return 0;
        return level;
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return false;
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return false;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack previous, ItemStack current, boolean slotChanged) {
        return slotChanged;
    }

    @Override
    public boolean shouldCauseBlockBreakReset(ItemStack previous, ItemStack current) {
        return previous != current && super.shouldCauseBlockBreakReset(previous, current);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        boolean steam = StrataDrillData.assembly(StrataDrillData.read(stack)).steam();
        int fuel = steam ? (int) StrataDrillPower.steamStored(stack) : StrataDrillPower.dieselStored(stack);
        int capacity = steam ? StrataDrillPower.STEAM_CAPACITY : StrataDrillPower.DIESEL_CAPACITY;
        if (fuel <= 0) return 0;
        if (fuel >= capacity) return FUEL_BAR_WIDTH;
        return Mth.clamp(Math.round((float) fuel * FUEL_BAR_WIDTH / capacity), 1, FUEL_BAR_WIDTH - 1);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return FUEL_BAR_COLOR;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flags) {
        boolean steam = StrataDrillData.assembly(StrataDrillData.read(stack)).steam();
        tooltip.add(steam ? Component.translatable("tooltip.frostedheart.stratadrill.steam",
                (int) StrataDrillPower.steamStored(stack), StrataDrillPower.STEAM_CAPACITY).withStyle(ChatFormatting.GRAY)
                : Component.translatable("tooltip.frostedheart.stratadrill.diesel",
                StrataDrillPower.dieselStored(stack), StrataDrillPower.DIESEL_CAPACITY).withStyle(ChatFormatting.GRAY));
        int max = stack.getMaxDamage();
        int wear = stack.getDamageValue();
        boolean broken = wear >= max - 1;
        // F3+H appends its own raw durability to damaged items. At max-1 it incorrectly suggests one use remains.
        if (!flags.isAdvanced() || broken || !stack.isDamaged()) {
            tooltip.add(Component.translatable("tooltip.frostedheart.stratadrill.head_durability",
                    broken ? 0 : max - wear, max).withStyle(ChatFormatting.GRAY));
        }
        if (steam) {
            tooltip.add(Component.translatable("tooltip.frostedheart.stratadrill.steam_dock").withStyle(ChatFormatting.GRAY));
        }
    }
}
