package com.teammoeg.frostedheart.base.item.rankine.alloys;


import com.teammoeg.frostedheart.base.item.rankine.init.Config;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public interface IAlloyTool extends IAlloyTieredItem {

    List<AlloyModifier.ModifierType> STATS = Arrays.asList(AlloyModifier.ModifierType.DURABILITY, AlloyModifier.ModifierType.ENCHANTABILITY, AlloyModifier.ModifierType.HARVEST_LEVEL,
            AlloyModifier.ModifierType.ATTACK_DAMAGE, AlloyModifier.ModifierType.ATTACK_SPEED, AlloyModifier.ModifierType.MINING_SPEED,
            AlloyModifier.ModifierType.CORROSION_RESISTANCE, AlloyModifier.ModifierType.HEAT_RESISTANCE, AlloyModifier.ModifierType.TOUGHNESS);

    @Override
    default List<AlloyModifier.ModifierType> getDefaultStats() {
        return STATS;
    }

    @Override
    default void initStats(ItemStack stack, Map<ElementRecipe, Integer> elementMap, @Nullable OldAlloyingRecipe alloyRecipe, @Nullable AlloyModifierRecipe alloyModifier) {
        CompoundTag listnbt = new CompoundTag();
        listnbt.putInt("durability",createValueForDurability(elementMap,alloyRecipe,getModifierForStat(alloyModifier, AlloyModifier.ModifierType.DURABILITY)));
        listnbt.putFloat("miningSpeed",createValueForMiningSpeed(elementMap,alloyRecipe,getModifierForStat(alloyModifier, AlloyModifier.ModifierType.MINING_SPEED)));
        listnbt.putInt("harvestLevel",createValueForHarvestLevel(elementMap,alloyRecipe,getModifierForStat(alloyModifier, AlloyModifier.ModifierType.HARVEST_LEVEL)));
        listnbt.putInt("enchantability",createValueForEnchantability(elementMap,alloyRecipe,getModifierForStat(alloyModifier, AlloyModifier.ModifierType.ENCHANTABILITY)));
        listnbt.putFloat("attackDamage",createValueForAttackDamage(elementMap,alloyRecipe,getModifierForStat(alloyModifier, AlloyModifier.ModifierType.ATTACK_DAMAGE)) + createValueForHarvestLevel(elementMap,alloyRecipe,getModifierForStat(alloyModifier, AlloyModifier.ModifierType.HARVEST_LEVEL)));
        listnbt.putFloat("attackSpeed",createValueForAttackSpeed(elementMap,alloyRecipe,getModifierForStat(alloyModifier, AlloyModifier.ModifierType.ATTACK_SPEED)));
        listnbt.putFloat("corrResist",createValueForCorrosionResistance(elementMap,alloyRecipe,getModifierForStat(alloyModifier, AlloyModifier.ModifierType.CORROSION_RESISTANCE)));
        listnbt.putFloat("heatResist",createValueForHeatResistance(elementMap,alloyRecipe,getModifierForStat(alloyModifier, AlloyModifier.ModifierType.HEAT_RESISTANCE)));
        listnbt.putFloat("toughness",createValueForToughness(elementMap,alloyRecipe,getModifierForStat(alloyModifier, AlloyModifier.ModifierType.TOUGHNESS)));
        stack.getOrCreateTag().put("StoredAlloyStats", listnbt);
    }

    @Override
    default void addAlloyInformation(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
        DecimalFormat df = Util.make(new DecimalFormat("##.#"), (p_234699_0_) -> {
            p_234699_0_.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ROOT));
        });
        if (this.isAlloyInit(stack)) {
            if (!Screen.hasShiftDown()) {
                tooltip.add((Component.literal("Hold shift for details...")).withStyle(ChatFormatting.GRAY));
            }
            if (Screen.hasShiftDown()) {
                if (IAlloyItem.getAlloyComposition(stack).isEmpty()) {
                    tooltip.add((Component.literal("Any Composition").withStyle(ChatFormatting.GOLD)));
                } else {
                    tooltip.add((Component.literal("Composition: " + IAlloyItem.getAlloyComposition(stack)).withStyle(ChatFormatting.GOLD)));
                }

                if (!IAlloyItem.getAlloyModifiers(stack).isEmpty()) {
                    tooltip.add((Component.literal("Modifier: " + (IAlloyItem.getAlloyModifiers(stack).getCompound(0).getString("modifierName"))).withStyle(ChatFormatting.AQUA)));
                }

                if (!this.needsRefresh(stack)) {

                    tooltip.add((Component.literal("Durability: " + (getAlloyDurability(stack) - stack.getDamageValue()) + "/" + getAlloyDurability(stack))).withStyle(ChatFormatting.DARK_GREEN));
                    tooltip.add((Component.literal("Tier: " + (getAlloyHarvestLevel(stack) + " (" + getAlloyTier(stack).toString() + ")"))).withStyle(ChatFormatting.GRAY));
                    tooltip.add((Component.literal("Mining Speed: " + df.format(getAlloyMiningSpeed(stack)))).withStyle(ChatFormatting.GRAY));
                    tooltip.add((Component.literal("Enchantability: " + getAlloyEnchantability(stack))).withStyle(ChatFormatting.GRAY));
                    if (Config.ALLOYS.ALLOY_CORROSION.get()) {
                        tooltip.add((Component.literal("Corrosion Resistance: " + (df.format(getCorrResist(stack) * 100)) + "%")).withStyle(ChatFormatting.GRAY));
                    }
                    if (Config.ALLOYS.ALLOY_HEAT.get()) {
                        tooltip.add((Component.literal("Heat Resistance: " + (df.format(getHeatResist(stack) * 100)) + "%")).withStyle(ChatFormatting.GRAY));
                    }
                    if (Config.ALLOYS.ALLOY_TOUGHNESS.get()) {
                        tooltip.add((Component.literal("Toughness: " + (df.format(getToughness(stack) * 100)) + "%")).withStyle(ChatFormatting.GRAY));
                    }
                }
            }
        }
    }

}
