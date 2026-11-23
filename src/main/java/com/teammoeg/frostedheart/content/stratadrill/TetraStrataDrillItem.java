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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Multimap;
import com.teammoeg.frostedheart.FHMain;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ToolActions;
import se.mickelus.tetra.data.DataManager;
import se.mickelus.tetra.items.modular.IModularItem;
import se.mickelus.tetra.module.BasicMajorModule;
import se.mickelus.tetra.module.ItemModule;
import se.mickelus.tetra.module.ItemModuleMajor;
import se.mickelus.tetra.module.ItemUpgradeRegistry;
import se.mickelus.tetra.module.ModuleRegistry;
import se.mickelus.tetra.module.data.EffectData;
import se.mickelus.tetra.module.data.ImprovementData;
import se.mickelus.tetra.module.data.ItemProperties;
import se.mickelus.tetra.module.data.ModuleData;
import se.mickelus.tetra.module.data.SynergyData;
import se.mickelus.tetra.module.data.ToolData;
import se.mickelus.tetra.module.data.VariantData;
import se.mickelus.tetra.properties.IToolProvider;
import se.mickelus.tetra.util.TierHelper;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/** Tetra workbench view of the same FH item and state; never used as the mining implementation. */
@ParametersAreNonnullByDefault
public final class TetraStrataDrillItem extends StrataDrillItem implements IModularItem, IToolProvider {
    private static final String HEAD_SLOT = "stratadrill/head";
    private static final String DRIVE_SLOT = "stratadrill/drive";
    private static final String SPIRAL = "stratadrill/spiral";
    private static final String WHEEL = "stratadrill/wheel";
    private static final String STEAM = "stratadrill/steam";
    private static final String DIESEL = "stratadrill/diesel";
    private static final String CUTTER_PREFIX = "stratadrill/cutting_edge/";
    private static final String[] MAJOR_SLOTS = {HEAD_SLOT, DRIVE_SLOT};
    private static final String[] MINOR_SLOTS = {};
    private static final SynergyData[] NO_SYNERGIES = {};
    private static final ImprovementData[] NO_IMPROVEMENTS = {};
    private static final ItemStack[] NO_SALVAGE = {};

    private final Cache<String, Multimap<Attribute, AttributeModifier>> attributes = CacheBuilder.newBuilder().maximumSize(64).build();
    private final Cache<String, EffectData> effects = CacheBuilder.newBuilder().maximumSize(64).build();
    private final Cache<String, ItemProperties> properties = CacheBuilder.newBuilder().maximumSize(64).build();

    public TetraStrataDrillItem() {
        DataManager.instance.moduleData.onReload(this::clearCaches);
    }

    /** Registered after Tetra creates its module registry, before loading the schematic data. */
    public static void registerHeadModule() {
        ModuleRegistry.instance.registerModuleType(FHMain.rl("stratadrill_head"), HeadModule::new);
        ModuleRegistry.instance.registerModuleType(FHMain.rl("stratadrill_drive"), DriveModule::new);
    }

    @Override
    public Item getItem() {
        return this;
    }

    @Override
    public void clearCaches() {
        attributes.invalidateAll();
        effects.invalidateAll();
        properties.invalidateAll();
    }

    @Override
    public String[] getMajorModuleKeys(ItemStack stack) {
        return MAJOR_SLOTS.clone();
    }

    @Override
    public String[] getMinorModuleKeys(ItemStack stack) {
        return MINOR_SLOTS;
    }

    @Override
    public String[] getRequiredModules(ItemStack stack) {
        return getMajorModuleKeys(stack);
    }

    @Nullable
    @Override
    public ItemModule getModuleFromSlot(ItemStack stack, String slot) {
        // A data pack can change workbench costs, but must use the native-backed module types.
        if (HEAD_SLOT.equals(slot) && ItemUpgradeRegistry.instance.getModule(slot) instanceof HeadModule head) return head;
        if (DRIVE_SLOT.equals(slot) && ItemUpgradeRegistry.instance.getModule(slot) instanceof DriveModule drive) return drive;
        return null;
    }

    @Override
    public ItemModuleMajor[] getMajorModules(ItemStack stack) {
        return new ItemModuleMajor[]{
                (ItemModuleMajor) getModuleFromSlot(stack, HEAD_SLOT),
                (ItemModuleMajor) getModuleFromSlot(stack, DRIVE_SLOT)
        };
    }

    @Override
    public Collection<ItemModule> getAllModules(ItemStack stack) {
        return Arrays.stream(getMajorModules(stack)).filter(Objects::nonNull).<ItemModule>map(module -> module).toList();
    }

    @Override
    public int getHoneBase(ItemStack stack) {
        return 450;
    }

    @Override
    public int getHoneIntegrityMultiplier(ItemStack stack) {
        return 0;
    }

    @Override
    public boolean canGainHoneProgress(ItemStack stack) {
        return false;
    }

    @Override
    public void setHoningProgress(ItemStack stack, int progress) {
        // S04 does not participate in Tetra honing.
    }

    @Override
    public SynergyData[] getAllSynergyData(ItemStack stack) {
        return NO_SYNERGIES;
    }

    @Override
    public Cache<String, Multimap<Attribute, AttributeModifier>> getAttributeModifierCache() {
        return attributes;
    }

    @Override
    public Cache<String, EffectData> getEffectDataCache() {
        return effects;
    }

    @Override
    public Cache<String, ItemProperties> getPropertyCache() {
        return properties;
    }

    @Override
    public String getDataCacheKey(ItemStack stack) {
        String cutterId = StrataDrillData.cutterId(stack);
        var cutter = StrataDrillMaterials.get(cutterId);
        // FH's catalog and Tetra's data can reload independently (or arrive in either order).
        return assembly(stack).name() + "/" + cutterId + "/" + cutter.id()
                + "/" + cutter.durability() + "/" + cutter.tier() + "/" + cutter.efficiency();
    }

    @Override
    public String getModelCacheKey(ItemStack stack, @Nullable LivingEntity entity) {
        return getDataCacheKey(stack);
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        // Select the FH base implementation, not IModularItem's competing Forge default.
        return super.getEnchantmentValue(stack);
    }

    @Override
    public void assemble(ItemStack stack, @Nullable Level level, float severity) {
        // Tetra calls this on its paid result copy. S04 has no settle/instability effects.
        // Do not write Tetra's independent cache identity onto the real FH stack.
        StrataDrillData.ensureIdentity(stack);
    }

    @Override
    public boolean canProvideTools(ItemStack stack) {
        return StrataDrillPower.canRun(stack, false);
    }

    @Override
    public ToolData getToolData(ItemStack stack) {
        // The Tetra UI reports the head's native rating in Tetra units (the old ×2.5 conversion).
        var cutter = StrataDrillMaterials.get(StrataDrillData.cutterId(stack));
        return pickaxeData(tetraTierIndex(cutter.tier()) + 1, StrataDrillItem.nominalHeadSpeed(stack) / 2.5F);
    }

    private static int tetraTierIndex(int nativeTier) {
        return TierHelper.getIndex(StrataDrillItem.harvestTier(nativeTier));
    }

    private static ToolData pickaxeData(int tier, float efficiency) {
        var data = new ToolData();
        data.levelMap.put(ToolActions.PICKAXE_DIG, (float) tier);
        data.efficiencyMap.put(ToolActions.PICKAXE_DIG, efficiency);
        return data;
    }

    private static StrataDrillAssembly assembly(ItemStack stack) {
        var value = StrataDrillAssembly.read(stack);
        return value != null ? value : StrataDrillAssembly.STEAM_SPIRAL;
    }

    /** Recheck a previously selected schematic immediately before Tetra charges materials. */
    public boolean allowsPartSwap(ItemStack stack, @Nullable String slot, String schematic) {
        var value = assembly(stack);
        return switch (schematic) {
            case SPIRAL -> HEAD_SLOT.equals(slot) && value.wheel() && getModuleFromSlot(stack, HEAD_SLOT) != null;
            case WHEEL -> HEAD_SLOT.equals(slot) && !value.wheel() && getModuleFromSlot(stack, HEAD_SLOT) != null;
            case STEAM -> DRIVE_SLOT.equals(slot) && !value.steam() && getModuleFromSlot(stack, DRIVE_SLOT) != null;
            case DIESEL -> DRIVE_SLOT.equals(slot) && value.steam() && getModuleFromSlot(stack, DRIVE_SLOT) != null;
            default -> true;
        };
    }

    /** An outcome is valid only if it is both a workbench recipe and a known FH cutter. */
    public boolean supportsCutter(ItemStack stack, String key, int level) {
        return level == 0 && key.startsWith(CUTTER_PREFIX) && StrataDrillMaterials.contains(key.substring(CUTTER_PREFIX.length()))
                && getModuleFromSlot(stack, HEAD_SLOT) instanceof ItemModuleMajor head
                && head.acceptsImprovementLevel(key, level);
    }

    private static final class HeadModule extends BasicMajorModule {
        private HeadModule(ResourceLocation id, ModuleData data) {
            super(id, data);
        }

        @Override
        public VariantData getVariantData(ItemStack stack) {
            return stack.getItem() instanceof TetraStrataDrillItem
                    ? getVariantData(assembly(stack).wheel() ? WHEEL : SPIRAL) : super.getVariantData(stack);
        }

        @Override
        public void addModule(ItemStack stack, String variant, @Nullable Player player) {
            if (!(stack.getItem() instanceof TetraStrataDrillItem)) {
                super.addModule(stack, variant, player);
                return;
            }
            var value = assembly(stack);
            if (WHEEL.equals(variant) || SPIRAL.equals(variant)) {
                boolean wheel = WHEEL.equals(variant);
                if (value.wheel() != wheel) {
                    StrataDrillData.setAssembly(stack, StrataDrillAssembly.of(value.steam(), wheel));
                    StrataDrillData.setCutter(stack, StrataDrillData.DEFAULT_CUTTER);
                }
            }
        }

        @Override
        public ItemStack[] removeModule(ItemStack stack, boolean upgrade) {
            // Both S04 slots are required. Tetra calls remove immediately before replacing the part on its copy.
            return stack.getItem() instanceof TetraStrataDrillItem ? NO_SALVAGE : super.removeModule(stack, upgrade);
        }

        @Override
        public void addImprovement(ItemStack stack, String key, int level) {
            if (!(stack.getItem() instanceof TetraStrataDrillItem drill)) {
                super.addImprovement(stack, key, level);
            } else if (drill.supportsCutter(stack, key, level)) {
                StrataDrillData.setCutter(stack, key.substring(CUTTER_PREFIX.length()));
            }
        }

        @Override
        public int getImprovementLevel(ItemStack stack, String key) {
            if (!(stack.getItem() instanceof TetraStrataDrillItem)) return super.getImprovementLevel(stack, key);
            return getImprovement(stack, key) != null ? 0 : -1;
        }

        @Nullable
        @Override
        public ImprovementData getImprovement(ItemStack stack, String key) {
            if (!(stack.getItem() instanceof TetraStrataDrillItem drill)) return super.getImprovement(stack, key);
            String material = StrataDrillData.cutterId(stack);
            if (StrataDrillData.DEFAULT_CUTTER.equals(material) || !key.equals(CUTTER_PREFIX + material)
                    || !drill.supportsCutter(stack, key, 0)) return null;
            var cutter = StrataDrillMaterials.get(material);
            var iron = StrataDrillMaterials.get(StrataDrillData.DEFAULT_CUTTER);
            var view = new ImprovementData();
            view.key = key;
            view.group = "stratadrill/cutting_edge";
            view.durability = cutter.durability() - iron.durability();
            view.tools = pickaxeData(tetraTierIndex(cutter.tier()) - tetraTierIndex(iron.tier()),
                    (cutter.efficiency() - iron.efficiency()) / 2.5F);
            return view;
        }

        @Override
        public ImprovementData[] getImprovements(ItemStack stack) {
            if (!(stack.getItem() instanceof TetraStrataDrillItem)) return super.getImprovements(stack);
            var cutter = getImprovement(stack, CUTTER_PREFIX + StrataDrillData.cutterId(stack));
            return cutter == null ? NO_IMPROVEMENTS : new ImprovementData[]{cutter};
        }

        @Override
        public ItemProperties getProperties(ItemStack stack) {
            var result = super.getProperties(stack);
            if (stack.getItem() instanceof TetraStrataDrillItem) result.durability = stack.getMaxDamage();
            return result;
        }

        @Override
        public ToolData getToolData(ItemStack stack) {
            return stack.getItem() instanceof TetraStrataDrillItem drill ? drill.getToolData(stack) : super.getToolData(stack);
        }

        @Override
        public String getName(ItemStack stack) {
            if (!(stack.getItem() instanceof TetraStrataDrillItem)) return super.getName(stack);
            Language language = Language.getInstance();
            String title = "tetra.variant." + getVariantData(stack).key + ".material_name";
            if (!language.has(title)) return super.getName(stack);

            String material = StrataDrillMaterials.get(StrataDrillData.cutterId(stack)).id();
            String prefix = "frostedheart.stratadrill.cutter." + material;
            if (!language.has(prefix)) prefix = "tetra.material." + material + ".prefix";
            return language.has(prefix) ? Component.translatable(title, Component.translatable(prefix)).getString()
                    : super.getName(stack);
        }
    }

    private static final class DriveModule extends BasicMajorModule {
        private DriveModule(ResourceLocation id, ModuleData data) {
            super(id, data);
        }

        @Override
        public VariantData getVariantData(ItemStack stack) {
            return stack.getItem() instanceof TetraStrataDrillItem
                    ? getVariantData(assembly(stack).steam() ? STEAM : DIESEL) : super.getVariantData(stack);
        }

        @Override
        public void addModule(ItemStack stack, String variant, @Nullable Player player) {
            if (!(stack.getItem() instanceof TetraStrataDrillItem)) {
                super.addModule(stack, variant, player);
                return;
            }
            var value = assembly(stack);
            if (STEAM.equals(variant) || DIESEL.equals(variant)) {
                boolean steam = STEAM.equals(variant);
                if (value.steam() != steam) StrataDrillData.setAssembly(stack, StrataDrillAssembly.of(steam, value.wheel()));
            }
        }

        @Override
        public ItemStack[] removeModule(ItemStack stack, boolean upgrade) {
            return stack.getItem() instanceof TetraStrataDrillItem ? NO_SALVAGE : super.removeModule(stack, upgrade);
        }
    }
}
