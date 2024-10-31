package com.teammoeg.frostedheart.base.item.rankine.alloys;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public interface IAlloyProjectile extends IAlloySpecialItem {

    @Override
    default void initStats(ItemStack stack, Map<ElementRecipe, Integer> elementMap, @Nullable OldAlloyingRecipe alloyRecipe, @Nullable AlloyModifierRecipe alloyModifier) {
        CompoundTag listnbt = new CompoundTag();
        listnbt.putFloat("projectileDamage",createValueForProjectileDamage(elementMap,alloyRecipe,getModifierForStat(alloyModifier, AlloyModifier.ModifierType.ATTACK_DAMAGE)));
        stack.getOrCreateTag().put("StoredAlloyStats", listnbt);
    }

    @Override
    default void addAlloyInformation(ItemStack stack, @javax.annotation.Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
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

                if (!this.needsRefresh(stack)) {
                    tooltip.add((Component.literal("Damage: " + (df.format(getAlloyArrowDamage(stack))))).withStyle(ChatFormatting.GRAY));
                }
            }
        }
    }

    @Override
    default List<AlloyModifier.ModifierType> getDefaultStats() {
        return null;
    }

    default float getAlloyArrowDamage(ItemStack stack)
    {
        if (stack.getTag() != null) {
            return stack.getTag().getCompound("StoredAlloyStats").getFloat("projectileDamage");
        } else {
            return 1;
        }
    }

    default float createValueForProjectileDamage(Map<ElementRecipe,Integer> elementMap, OldAlloyingRecipe alloy, @javax.annotation.Nullable AlloyModifier modifier)
    {
        float dmgmin = 0;
        float dmgmax = 0;
        for (Map.Entry<ElementRecipe,Integer> set : elementMap.entrySet()) {
            float dmg = set.getKey().getDamage(set.getValue());
            dmgmin = Math.min(dmg,dmgmin);
            dmgmax = Math.max(dmg,dmgmax);
        }
        if (alloy != null) {
            float dmg = alloy.getBonusDamage();
            dmgmin = Math.min(dmgmin,dmgmin+dmg);
            dmgmax = Math.max(dmgmax,dmgmax+dmg);
        }
        float dmg = dmgmax + dmgmin;
        if (modifier != null) {
            dmg = Math.max(0,modifier.returnModification(dmg));
        }

        int hlmin = 0;
        int hlmax = 0;
        for (Map.Entry<ElementRecipe,Integer> set : elementMap.entrySet()) {
            int hl = set.getKey().getMiningLevel(set.getValue());
            hlmin = Math.min(hl,hlmin);
            hlmax = Math.max(hl,hlmax);
        }
        if (alloy != null) {
            int hl = alloy.getBonusMiningLevel();
            hlmin = Math.min(hlmin,hlmin+hl);
            hlmax = Math.max(hlmax,hlmax+hl);
        }
        int hl = Math.max(hlmax + hlmin,0);
        if (modifier != null) {
            hl =  (int) Math.max(0,modifier.returnModification(hl));
        }

        return 2f + 0.5f*hl + 0.5f*dmg;
    }

/*

    default float createValueForProjectileVelocity()
    {
        int hl = calcMiningLevel(elements,percents);
        float as = calcAttackSpeed(elements,percents);
        float ms = calcMiningSpeed(elements,percents);

        return 2f + 0.5f*hl + 0.5f*dmg;
    }*/
}
