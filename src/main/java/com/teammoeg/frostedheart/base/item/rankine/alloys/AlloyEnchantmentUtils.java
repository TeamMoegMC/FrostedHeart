package com.teammoeg.frostedheart.base.item.rankine.alloys;

import blusunrize.immersiveengineering.common.items.HammerItem;
import com.teammoeg.frostedheart.base.item.rankine.KnifeItem;
import com.teammoeg.frostedheart.base.item.rankine.SpearItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public class AlloyEnchantmentUtils {

    public static List<Enchantment> getAlloyEnchantments(OldAlloyingRecipe recipe, ItemStack stack, Level worldIn) {
        List<Enchantment> enchantments = new ArrayList<>();
        List<String> alloyEnchants = recipe.getEnchantments();
        List<String> alloyEnchantTypes = recipe.getEnchantmentTypes();
        for (int i = 0; i < alloyEnchants.size(); i++) {
            Enchantment en;
            if (alloyEnchants.get(i).equals("rankine:random")) {
                List<EnchantmentInstance> dat = EnchantmentHelper.selectEnchantment(worldIn.getRandom(),stack,worldIn.getRandom().nextInt(40),true);
                if (!dat.isEmpty()) {
                    en = dat.get(0).enchantment;
                } else {
                    en = Enchantments.UNBREAKING;
                }
            } else {
                en = ForgeRegistries.ENCHANTMENTS.getValue(new ResourceLocation(alloyEnchants.get(i)));
            }
            if (en != null && matchesEnchantmentType(en,stack, EnchantmentTypes.valueOf(alloyEnchantTypes.get(i)))) {
                if (!enchantments.contains(en)) {
                    enchantments.add(en);
                }
            }
        }

        return enchantments;
    }

    public static List<Enchantment> getElementEnchantments(List<ElementRecipe> elements, List<Integer> percents, ItemStack stack, Level worldIn) {
        List<Enchantment> enchantments = new ArrayList<>();
        List<Float> enchantmentTotals = new ArrayList<>();
        for (int e = 0; e < elements.size(); e++) {
            ElementRecipe element = elements.get(e);
            int percent = percents.get(e);
            List<String> elementEnchants = element.getEnchantments();
            List<String> elementEnchantTypes = element.getEnchantmentTypes();
            List<Float> elementEnchantFactors = element.getEnchantmentFactors();

            for (int i = 0; i < elementEnchants.size(); i++) {
                Enchantment en;
                if (elementEnchants.get(i).equals("rankine:random")) {
                    List<EnchantmentInstance> dat = EnchantmentHelper.selectEnchantment(worldIn.getRandom(),stack,1,true);
                    if (!dat.isEmpty()) {
                        en = dat.get(0).enchantment;
                    } else {
                        en = Enchantments.UNBREAKING;
                    }
                } else {
                    en = ForgeRegistries.ENCHANTMENTS.getValue(new ResourceLocation(elementEnchants.get(i)));
                }
                if (en != null && matchesEnchantmentType(en,stack, EnchantmentTypes.valueOf(elementEnchantTypes.get(i)))) {
                    if (!enchantments.contains(en)) {
                        enchantments.add(en);
                        enchantmentTotals.add(0f);
                    }
                    int index = enchantments.indexOf(en);
                    enchantmentTotals.set(index,enchantmentTotals.get(index) + elementEnchantFactors.get(i)*percent); // bound -1 to 1 in recipe class
                }
            }
        }

        List<Enchantment> finalEnchantments = new ArrayList<>();
        for (int i = 0; i < enchantments.size(); i++) {
            if (enchantmentTotals.get(i) >= 1) {
                finalEnchantments.add(enchantments.get(i));
            }
        }

        return finalEnchantments;
    }

    public static boolean matchesEnchantmentType(Enchantment enchantment, ItemStack stack, EnchantmentTypes type) {
        switch (type) {
            case ALL:
                return enchantment.canEnchant(stack);
            case TOOLS:
                return enchantment.canEnchant(stack) && (stack.getItem() instanceof DiggerItem || stack.getItem() instanceof SwordItem);
            case ARMOR:
                return enchantment.canEnchant(stack) && (stack.getItem() instanceof ArmorItem);
            case PICKAXE:
                return enchantment.canEnchant(stack) && (stack.getItem() instanceof PickaxeItem);
            case AXE:
                return enchantment.canEnchant(stack) && (stack.getItem() instanceof AxeItem);
            case SPEAR:
                return enchantment.canEnchant(stack) && (stack.getItem() instanceof SpearItem);
            case SHOVEL:
                return enchantment.canEnchant(stack) && (stack.getItem() instanceof ShovelItem);
            case HOE:
                return enchantment.canEnchant(stack) && (stack.getItem() instanceof HoeItem);
            case SWORD:
                return enchantment.canEnchant(stack) && (stack.getItem() instanceof SwordItem && !(stack.getItem() instanceof KnifeItem));
            case HAMMER:
                return enchantment.canEnchant(stack) && (stack.getItem() instanceof HammerItem);
            case KNIFE:
                return enchantment.canEnchant(stack) && (stack.getItem() instanceof KnifeItem);
//            case CROWBAR:
//                return enchantment.canEnchant(stack) && (stack.getItem() instanceof CrowbarItem);
            case HELMET:
                return enchantment.canEnchant(stack) && (stack.getItem() instanceof ArmorItem) && ((ArmorItem) stack.getItem()).getEquipmentSlot() == EquipmentSlot.HEAD;
            case CHESTPLATE:
                return enchantment.canEnchant(stack) && (stack.getItem() instanceof ArmorItem) && ((ArmorItem) stack.getItem()).getEquipmentSlot() == EquipmentSlot.CHEST;
            case LEGGINGS:
                return enchantment.canEnchant(stack) && (stack.getItem() instanceof ArmorItem) && ((ArmorItem) stack.getItem()).getEquipmentSlot() == EquipmentSlot.LEGS;
            case BOOTS:
                return enchantment.canEnchant(stack) && (stack.getItem() instanceof ArmorItem) && ((ArmorItem) stack.getItem()).getEquipmentSlot() == EquipmentSlot.FEET;
            case SHIELD:
                return enchantment.canEnchant(stack) && (stack.getItem() instanceof ShieldItem);
            case FISHING_ROD:
                return enchantment.canEnchant(stack) && (stack.getItem() instanceof FishingRodItem);
            case BOW:
                return enchantment.canEnchant(stack) && (stack.getItem() instanceof BowItem);
//            case BLUNDERBUSS:
//                return enchantment.canEnchant(stack) && (stack.getItem() instanceof BlunderbussItem);
            default:
                return enchantment.canEnchant(stack);
        }
    }

    public enum EnchantmentTypes {
        ALL,
        TOOLS,
        ARMOR,
        PICKAXE,
        AXE,
        SPEAR,
        SHOVEL,
        HOE,
        SWORD,
        HAMMER,
        KNIFE,
        CROWBAR,
        HELMET,
        CHESTPLATE,
        LEGGINGS,
        BOOTS,
        SHIELD,
        FISHING_ROD,
        BOW,
        BLUNDERBUSS

    }
}
