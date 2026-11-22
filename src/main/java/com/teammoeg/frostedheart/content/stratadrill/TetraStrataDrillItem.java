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
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.client.FHTabs;
import com.teammoeg.frostedheart.content.stratadrill.client.StrataDrillHands;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.locale.Language;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
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
import se.mickelus.tetra.module.BasicMajorModule;
import se.mickelus.tetra.module.ItemModule;
import se.mickelus.tetra.module.ItemModuleMajor;
import se.mickelus.tetra.module.ModuleRegistry;
import se.mickelus.tetra.module.data.ModuleData;

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
    private static final int FUEL_BAR_WIDTH = 13;
    private static final int FUEL_BAR_COLOR = 0xB7C8CE;

    public TetraStrataDrillItem() {
        super(new Properties().stacksTo(1));
        majorModuleKeys = new String[]{HEAD_SLOT, DRIVE_SLOT};
        minorModuleKeys = new String[0];
        requiredModules = majorModuleKeys;
        // S04 does not add progression or new honing outcomes in this version.
        canHone = false;
    }

    /** Register the S04-only display subtype after Tetra has constructed its module registry. */
    public static void registerHeadModule() {
        ModuleRegistry.instance.registerModuleType(FHMain.rl("stratadrill_head"), HeadModule::new);
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
        String drive = getInstalledDriveVariant(stack);
        if (STEAM.equals(drive)) key += ".steam";
        else if (DIESEL.equals(drive)) key += ".diesel";
        return Component.translatable(key);
    }

    /** Display the installed drive independently of the head, wear and fuel level. */
    @Nullable
    private String getInstalledDriveVariant(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !DRIVE_SLOT.equals(tag.getString(DRIVE_SLOT))) return null;

        // Do not display Tetra's default variant for a missing or unknown drive.
        String variant = tag.getString(DRIVE_SLOT + "_material");
        if (!STEAM.equals(variant) && !DIESEL.equals(variant)) return null;
        ItemModule drive = getModuleFromSlot(stack, DRIVE_SLOT);
        return drive != null && DRIVE_SLOT.equals(drive.getKey()) && variantIs(stack, drive, variant) ? variant : null;
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
    public boolean isBarVisible(ItemStack stack) {
        return getInstalledDriveVariant(stack) != null;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        String drive = getInstalledDriveVariant(stack);
        if (STEAM.equals(drive)) {
            return fuelBarWidth((int) StrataDrillPower.steamStored(stack), StrataDrillPower.STEAM_CAPACITY);
        }
        if (DIESEL.equals(drive)) {
            return fuelBarWidth(StrataDrillPower.dieselStored(stack), StrataDrillPower.DIESEL_CAPACITY);
        }
        return 0;
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return FUEL_BAR_COLOR;
    }

    private static int fuelBarWidth(int fuel, int capacity) {
        // Leave the vanilla track visible at empty; reserve the full-width fill for a full tank.
        if (fuel <= 0) return 0;
        if (fuel >= capacity) return FUEL_BAR_WIDTH;
        return Mth.clamp(Math.round((float) fuel * FUEL_BAR_WIDTH / capacity), 1, FUEL_BAR_WIDTH - 1);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flags) {
        String drive = getInstalledDriveVariant(stack);
        boolean steam = STEAM.equals(drive);
        // ItemStack has appended the name; add fuel before Tetra's tooltip details.
        if (steam) {
            tooltip.add(Component.translatable("tooltip.frostedheart.stratadrill.steam",
                    (int) StrataDrillPower.steamStored(stack), StrataDrillPower.STEAM_CAPACITY)
                    .withStyle(ChatFormatting.GRAY));
        } else if (DIESEL.equals(drive)) {
            tooltip.add(Component.translatable("tooltip.frostedheart.stratadrill.diesel",
                    StrataDrillPower.dieselStored(stack), StrataDrillPower.DIESEL_CAPACITY)
                    .withStyle(ChatFormatting.GRAY));
        }
        appendHeadDurability(stack, tooltip, flags);
        super.appendHoverText(stack, level, tooltip, flags);
        if (steam) tooltip.add(Component.translatable("tooltip.frostedheart.stratadrill.steam_dock")
                .withStyle(ChatFormatting.GRAY));
    }

    private void appendHeadDurability(ItemStack stack, List<Component> tooltip, TooltipFlag flags) {
        ItemModule head = getModuleFromSlot(stack, HEAD_SLOT);
        if (head == null || !HEAD_SLOT.equals(head.getKey())) return;
        int max = stack.getMaxDamage();
        if (max <= 0) return;
        int damage = stack.getDamageValue();
        boolean broken = isBroken(damage, max);
        // F3+H adds the vanilla raw value to worn items later. Only repeat it if the drill has stopped at max − 1.
        if (flags.isAdvanced() && !broken && stack.isDamaged()) return;
        tooltip.add(Component.translatable("tooltip.frostedheart.stratadrill.head_durability",
                broken ? 0 : max - damage, max).withStyle(ChatFormatting.GRAY));
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

    @Override
    public boolean sameVisualIdentity(ItemStack previous, ItemStack current) {
        String id = getIdentifier(previous);
        return id != null && !id.isEmpty() && id.equals(getIdentifier(current));
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

    /** Check the selected S04 part against the current stack, not the workbench's earlier selection. */
    public boolean allowsPartSwap(ItemStack stack, @Nullable String slot, String schematic) {
        String targetSlot = switch (schematic) {
            case SPIRAL, WHEEL -> HEAD_SLOT;
            case STEAM, DIESEL -> DRIVE_SLOT;
            default -> null;
        };
        if (targetSlot == null) return true;
        if (!targetSlot.equals(slot)) return false;

        ItemModule installed = getModuleFromSlot(stack, targetSlot);
        return installed == null || !targetSlot.equals(installed.getKey()) || !variantIs(stack, installed, schematic);
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

    /** Keeps the head variant independent of the installed cutting-edge improvement. */
    private static final class HeadModule extends BasicMajorModule {
        private HeadModule(ResourceLocation id, ModuleData data) {
            super(id, data);
        }

        @Override
        public String getName(ItemStack stack) {
            Language language = Language.getInstance();
            String title = "tetra.variant." + getVariantData(stack).key + ".material_name";
            if (!language.has(title)) return super.getName(stack);

            String material = cutterMaterial(stack);
            String prefix = "frostedheart.stratadrill.cutter." + material;
            if (!language.has(prefix)) prefix = "tetra.material." + material + ".prefix";
            return language.has(prefix) ? Component.translatable(title, Component.translatable(prefix)).getString()
                    : super.getName(stack);
        }

        private String cutterMaterial(ItemStack stack) {
            CompoundTag tag = stack.getTag();
            if (tag != null) {
                for (String key : tag.getAllKeys()) {
                    if (key.startsWith(CUTTING_EDGE_TAG_PREFIX) && tag.contains(key, Tag.TAG_ANY_NUMERIC)
                            && isLoadedCutter(this, tag, key)) {
                        return key.substring(CUTTING_EDGE_TAG_PREFIX.length());
                    }
                }
            }
            // The stock head and heads whose material data was removed have iron's base stats.
            return "iron";
        }
    }
}
