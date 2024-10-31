package com.teammoeg.frostedheart.base.item.rankine.alloys;


import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public interface IAlloySpecialItem extends IAlloyItem {

    List<AlloyModifier.ModifierType> STATS = Collections.emptyList();

    void initStats(ItemStack stack, Map<ElementRecipe, Integer> elementMap, @Nullable OldAlloyingRecipe alloyRecipe, @Nullable AlloyModifierRecipe alloyModifier);

    default AlloyModifier getModifierForStat(AlloyModifierRecipe modifierRecipe, AlloyModifier.ModifierType type) {
        if (modifierRecipe == null) {
            return null;
        }
        for (AlloyModifier mod : modifierRecipe.getModifiers()) {
            if (mod.getType().equals(type)) {
                return mod;
            }
        }
        return null;
    }

    // Used for applying an alloy modifier after a tool has already been created (ex. Smithing Table)
    /*
    default void applyAlloyModifier(ItemStack stack, AlloyModifierRecipe modifier) {
        if (stack.getTag() != null && (!stack.getTag().contains("AlloyModifiers") || !stack.getTag().getCompound("AlloyModifiers").contains(modifier.getName()))) {
            if (!stack.getTag().contains("AlloyModifiers")) {
                stack.getTag().put("AlloyModifiers",new CompoundNBT());
            }
            switch (modifier.getType()) {
                case DURABILITY:
                    stack.getTag().getCompound("StoredAlloy").putInt("durability", (int) modifier.returnModification(this.getAlloyDurability(stack)));
                    stack.getTag().getCompound("AlloyModifiers").putInt(modifier.getName(), (int) modifier.getValue());
                    return;
                case HARVEST_LEVEL:
                    stack.getTag().getCompound("StoredAlloy").putInt("harvestLevel", (int) modifier.returnModification(this.getAlloyDurability(stack)));
                    stack.getTag().getCompound("AlloyModifiers").putInt(modifier.getName(), (int) modifier.getValue());
                    return;
                case ENCHANTABILITY:
                    stack.getTag().getCompound("StoredAlloy").putInt("enchantability", (int) modifier.returnModification(this.getAlloyDurability(stack)));
                    stack.getTag().getCompound("AlloyModifiers").putInt(modifier.getName(), (int) modifier.getValue());
                    return;
                case MINING_SPEED:
                    stack.getTag().getCompound("StoredAlloy").putFloat("miningSpeed", modifier.returnModification(this.getAlloyDurability(stack)));
                    stack.getTag().getCompound("AlloyModifiers").putFloat(modifier.getName(), modifier.getValue());
                    return;
                case ATTACK_DAMAGE:
                    stack.getTag().getCompound("StoredAlloy").putFloat("attackDamage", modifier.returnModification(this.getAlloyDurability(stack)));
                    stack.getTag().getCompound("AlloyModifiers").putFloat(modifier.getName(), modifier.getValue());
                    return;
                case ATTACK_SPEED:
                    stack.getTag().getCompound("StoredAlloy").putFloat("attackSpeed", modifier.returnModification(this.getAlloyDurability(stack)));
                    stack.getTag().getCompound("AlloyModifiers").putFloat(modifier.getName(), modifier.getValue());
                    return;
                case CORROSION_RESISTANCE:
                    stack.getTag().getCompound("StoredAlloy").putFloat("corrResist", modifier.returnModification(this.getAlloyDurability(stack)));
                    stack.getTag().getCompound("AlloyModifiers").putFloat(modifier.getName(), modifier.getValue());
                    return;
                case HEAT_RESISTANCE:
                    stack.getTag().getCompound("StoredAlloy").putFloat("heatResist", modifier.returnModification(this.getAlloyDurability(stack)));
                    stack.getTag().getCompound("AlloyModifiers").putFloat(modifier.getName(), modifier.getValue());
                    return;
                case TOUGHNESS:
                    stack.getTag().getCompound("StoredAlloy").putFloat("toughness", modifier.returnModification(this.getAlloyDurability(stack)));
                    stack.getTag().getCompound("AlloyModifiers").putFloat(modifier.getName(), modifier.getValue());
            }

        }

    }*/

    @Override
    default boolean isAlloyInit(ItemStack stack) {
        return stack.getTag() != null && (!stack.getTag().getCompound("StoredAlloy").isEmpty() || !stack.getTag().getCompound("StoredAlloyStats").isEmpty());
    }

    default boolean needsRefresh(ItemStack stack) {
        return stack.getTag() != null && (!stack.getTag().getCompound("StoredAlloy").isEmpty() || !stack.getTag().getCompound("StoredAlloyStats").isEmpty()) && stack.getTag().getBoolean("RegenerateAlloy");
    }

    default void setRefresh(ItemStack stack) {
        if (stack.getTag() != null && (!stack.getTag().getCompound("StoredAlloy").isEmpty() || !stack.getTag().getCompound("StoredAlloyStats").isEmpty())) {
            stack.getTag().putBoolean("RegenerateAlloy",true);
        }
    }

    List<AlloyModifier.ModifierType> getDefaultStats();
}
