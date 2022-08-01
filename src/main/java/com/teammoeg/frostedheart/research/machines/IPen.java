package com.teammoeg.frostedheart.research.machines;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public interface IPen {

    boolean canUse(@Nullable PlayerEntity e, ItemStack stack, int val);

    void doDamage(@Nullable PlayerEntity e, ItemStack stack, int val);

    default boolean damage(@Nullable PlayerEntity e, ItemStack stack, int val) {
        if (canUse(e, stack, val)) {
            doDamage(e, stack, val);
            return true;
        }
        return false;
    }

    int getLevel(ItemStack is, @Nullable PlayerEntity player);

    default boolean tryDamage(@Nullable PlayerEntity e, ItemStack stack, int val, Supplier<Boolean> onsuccess) {
        if (canUse(e, stack, val)) {
            if (onsuccess.get()) {
                doDamage(e, stack, val);
                return true;
            }
        }
        return false;
    }
}
