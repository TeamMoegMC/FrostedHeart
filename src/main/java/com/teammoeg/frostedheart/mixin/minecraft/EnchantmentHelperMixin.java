package com.teammoeg.frostedheart.mixin.minecraft;

import com.google.common.collect.Lists;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Deprecated
@Mixin(EnchantmentHelper.class)
public class EnchantmentHelperMixin {
    @Inject(method = "getEnchantmentDatas", at = @At(value = "HEAD"), cancellable = true)
    private static void cancelSurviveMixinIn(int level, ItemStack stack, boolean allowTreasure, CallbackInfoReturnable<List<EnchantmentData>> cir) {
        List<EnchantmentData> list = Lists.newArrayList();
        Item item = stack.getItem();
        boolean flag = stack.getItem() == Items.BOOK;

        for (Enchantment enchantment : Registry.ENCHANTMENT) {
            ResourceLocation key = enchantment.getRegistryName();
            if (key != null && key.getNamespace().equals("survive")) {
                String path = key.getPath();
                if (path.equals("warming") || path.equals("cooling") || path.equals("adjusted_warming") || path.equals("adjusted_cooling") || path.equals("featherweight") || path.equals("weightless")) {
                    continue;
                }
            }

            if ((!enchantment.isTreasureEnchantment() || allowTreasure) && enchantment.canGenerateInLoot() && (enchantment.canApplyAtEnchantingTable(stack) || (flag && enchantment.isAllowedOnBooks()))) {
                for(int i = enchantment.getMaxLevel(); i > enchantment.getMinLevel() - 1; --i) {
                    if (level >= enchantment.getMinEnchantability(i) && level <= enchantment.getMaxEnchantability(i)) {
                        list.add(new EnchantmentData(enchantment, i));
                        break;
                    }
                }
            }
        }
        cir.setReturnValue(list);
        cir.cancel();
    }
}
