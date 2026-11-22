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
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import se.mickelus.tetra.items.modular.IModularItem;
import se.mickelus.tetra.items.modular.ItemModularHandheld;
import se.mickelus.tetra.module.ItemModule;
import se.mickelus.tetra.module.ItemModuleMajor;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/** S04's two Tetra slots. Mining and drops use Tetra's ordinary pickaxe behavior. */
@ParametersAreNonnullByDefault
public final class TetraStrataDrillItem extends ItemModularHandheld
        implements StrataDrillAssembly.ItemView, ICreativeModeTabItem {

    public static final String ID = "stratadrill";
    private static final String HEAD_SLOT = "stratadrill/head";
    private static final String DRIVE_SLOT = "stratadrill/drive";
    private static final String SPIRAL = "stratadrill/spiral";
    private static final String WHEEL = "stratadrill/wheel";
    private static final String CUTTING_EDGE = "stratadrill/cutting_edge";
    private static final String CUTTING_EDGE_TAG_PREFIX = HEAD_SLOT + ":" + CUTTING_EDGE + "/";
    private static final String STEAM = "stratadrill/steam";
    private static final String DIESEL = "stratadrill/diesel";

    public TetraStrataDrillItem() {
        super(new Properties().stacksTo(1));
        majorModuleKeys = new String[]{HEAD_SLOT, DRIVE_SLOT};
        minorModuleKeys = new String[0];
        requiredModules = majorModuleKeys;
        // S04 does not add progression or new honing outcomes in this version.
        canHone = false;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(StrataDrillHands.extension());
    }

    @Override
    public ItemStack getDefaultInstance() {
        return getDefaultStack();
    }

    @Override
    public ItemStack getDefaultStack() {
        ItemStack stack = new ItemStack(this);
        IModularItem.putModuleInSlot(stack, HEAD_SLOT, HEAD_SLOT, SPIRAL);
        IModularItem.putModuleInSlot(stack, DRIVE_SLOT, DRIVE_SLOT, STEAM);
        ensureIdentity(stack);
        return stack;
    }

    @Override
    public void fillItemCategory(CreativeTabItemHelper helper) {
        if (helper.isType(FHTabs.TOOLS)) {
            helper.accept(getDefaultStack());
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        String key = getDescriptionId();
        CompoundTag tag = stack.getTag();
        if (tag == null || !DRIVE_SLOT.equals(tag.getString(DRIVE_SLOT))) return Component.translatable(key);

        // A missing or unknown variant can fall back in Tetra, but must not be named as an installed drive.
        String variant = tag.getString(DRIVE_SLOT + "_material");
        String suffix = switch (variant) {
            case STEAM -> ".steam";
            case DIESEL -> ".diesel";
            default -> null;
        };
        if (suffix != null) {
            ItemModule drive = getModuleFromSlot(stack, DRIVE_SLOT);
            if (drive != null && DRIVE_SLOT.equals(drive.getKey()) && variantIs(stack, drive, variant)) {
                key += suffix;
            }
        }
        return Component.translatable(key);
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag unused) {
        return StrataDrillPower.makeCapabilities(stack);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        InteractionResult refueled = StrataDrillPower.refuelDiesel(stack, context);
        return refueled == InteractionResult.PASS ? super.onItemUseFirst(stack, context) : refueled;
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        // Raw vanilla empty-hand speed. Player haste/fatigue, environment and Forge events still apply.
        StrataDrillAssembly powered = StrataDrillPower.poweredAssembly(stack, false);
        if (powered == null) return 1F;
        float speed = super.getDestroySpeed(stack, state);
        // Do not give Efficiency's vanilla speed > 1 condition to a non-pickaxe block by accident.
        return !powered.steam() && speed > 1F ? speed * StrataDrillPower.DIESEL_SPEED_MULTIPLIER : speed;
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        if (!state.requiresCorrectToolForDrops()) {
            // Vanilla separately allows bare-hand drops; Tetra does not call getHarvestTier here.
            return StrataDrillPower.canRun(stack, false);
        }
        // Tetra 6.7 checks power through our getHarvestTier for required-tool blocks.
        return StrataDrillPower.hasAnyAffordableReserve(stack) && super.isCorrectToolForDrops(stack, state);
    }

    @Override
    public int getHarvestTier(ItemStack stack, ToolAction action) {
        return StrataDrillPower.canRun(stack, false) ? super.getHarvestTier(stack, action) : -1;
    }

    @Override
    public Set<ToolAction> getToolActions(ItemStack stack) {
        return StrataDrillPower.canRun(stack, false) ? super.getToolActions(stack) : Set.of();
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
        // Do not pay Tetra's tool wear or fire its powered tool effects when used as a fist.
        return StrataDrillPower.canRun(stack, false) && super.mineBlock(stack, level, state, pos, miner);
    }

    @Override
    public int getEnchantmentLevel(ItemStack stack, Enchantment enchantment) {
        // Forge's BreakEvent reads the held stack for XP before drop context is selected.
        // These two mining enchantments are inactive when dry; their data is not removed.
        if ((enchantment == Enchantments.BLOCK_FORTUNE || enchantment == Enchantments.SILK_TOUCH)
                && !StrataDrillPower.canRun(stack, false)) return 0;
        return super.getEnchantmentLevel(stack, enchantment);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flags) {
        super.appendHoverText(stack, level, tooltip, flags);
        StrataDrillAssembly assembly = resolveAssembly(stack, true);
        if (assembly == null) return;
        if (assembly.steam()) {
            tooltip.add(Component.translatable("tooltip.frostedheart.stratadrill.steam",
                    (int) StrataDrillPower.steamStored(stack), StrataDrillPower.STEAM_CAPACITY));
            tooltip.add(Component.translatable("tooltip.frostedheart.stratadrill.steam_dock"));
        } else {
            tooltip.add(Component.translatable("tooltip.frostedheart.stratadrill.diesel",
                    StrataDrillPower.dieselStored(stack), StrataDrillPower.DIESEL_CAPACITY));
        }
    }

    @Override
    @Nullable
    public StrataDrillAssembly resolveAssembly(ItemStack stack, boolean requireOperable) {
        // Use the same module registry and variant rules as the workbench, without resolving
        // the two modules separately for validity, fuel selection and later assembly use.
        ItemModule drive = getModuleFromSlot(stack, DRIVE_SLOT);
        ItemModule head = getModuleFromSlot(stack, HEAD_SLOT);
        boolean installedDrive = drive != null && DRIVE_SLOT.equals(drive.getKey());
        boolean installedHead = head != null && HEAD_SLOT.equals(head.getKey());
        if (requireOperable && (!installedDrive || !installedHead || isBroken(stack))) return null;
        return StrataDrillAssembly.of(!(installedDrive && variantIs(stack, drive, DIESEL)),
                installedHead && variantIs(stack, head, WHEEL));
    }

    /** Use Tetra's existing invalidation scheme; never replace an ID while using this stack. */
    @Override
    public void ensureIdentity(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || tag.getString(IModularItem.identifierKey).isEmpty()) {
            IModularItem.updateIdentifier(stack);
        }
    }

    /** A client display stack uses the same item model but omits fuel, damage, name and cache ID. */
    @Override
    public CompoundTag dockAppearance(ItemStack source) {
        ItemStack display = new ItemStack(this);
        CompoundTag input = source.getTag();
        CompoundTag tags = display.getOrCreateTag();
        if (input != null) {
            for (String slot : majorModuleKeys) {
                IModularItem.putModuleInSlot(display, slot, input.getString(slot), input.getString(slot + "_material"));
            }
            for (String key : input.getAllKeys()) {
                if (key.startsWith(CUTTING_EDGE_TAG_PREFIX) && input.contains(key, Tag.TAG_ANY_NUMERIC)) {
                    tags.putInt(key, input.getInt(key));
                }
            }
        }
        if (source.hasFoil()) {
            // The stock ItemRenderer only needs a nonempty enchantment list for its foil layer.
            ListTag foil = new ListTag();
            foil.add(new CompoundTag());
            tags.put(ItemStack.TAG_ENCH, foil);
        }
        return display.save(new CompoundTag());
    }

    /**
     * Drop dormant cutter tags only on actual head work, after Tetra has applied the new cutter.
     * Tetra's exclusivity group removes loaded improvements but cannot see an unloaded profile.
     */
    public void pruneInactiveCuttersAfterCraft(ItemStack result, String slot, String schematic) {
        if (!HEAD_SLOT.equals(slot)) return;
        boolean replaceHead = SPIRAL.equals(schematic) || WHEEL.equals(schematic);
        if (!replaceHead && !CUTTING_EDGE.equals(schematic)) return;

        CompoundTag tag = result.getTag();
        if (tag == null || !(getModuleFromSlot(result, HEAD_SLOT) instanceof ItemModuleMajor head)
                || !HEAD_SLOT.equals(head.getKey())) return;
        if (!replaceHead && tag.getAllKeys().stream().noneMatch(key ->
                key.startsWith(CUTTING_EDGE_TAG_PREFIX) && isLoadedCutter(head, tag, key))) return;
        tag.getAllKeys().removeIf(key -> key.startsWith(CUTTING_EDGE_TAG_PREFIX) && !isLoadedCutter(head, tag, key));
    }

    private static boolean isLoadedCutter(ItemModuleMajor head, CompoundTag tag, String key) {
        return head.acceptsImprovementLevel(key.substring(HEAD_SLOT.length() + 1), tag.getInt(key));
    }

    private static boolean variantIs(ItemStack stack, ItemModule module, String variant) {
        var data = module.getVariantData(stack);
        return data != null && variant.equals(data.key);
    }
}
