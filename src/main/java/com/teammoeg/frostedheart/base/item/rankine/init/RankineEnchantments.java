package com.teammoeg.frostedheart.base.item.rankine.init;


import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.base.item.rankine.enchantment.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class RankineEnchantments {

    public static final DeferredRegister<Enchantment> ENCHANTMENTS = DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, FHMain.MODID);

    static final EquipmentSlot[] ARMOR_SLOTS = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    static final EquipmentSlot[] HAND_SLOTS = new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};

    public static final RegistryObject<Enchantment> POISON_ASPECT = ENCHANTMENTS.register("poison_aspect", () -> new PoisonAspectEnchantment(Enchantment.Rarity.RARE, EquipmentSlot.MAINHAND));
    public static final RegistryObject<Enchantment> GRAFTING = ENCHANTMENTS.register("grafting", () -> new GraftingEnchantment(Enchantment.Rarity.RARE, EquipmentSlot.MAINHAND));
    public static final RegistryObject<Enchantment> ENDPOINT = ENCHANTMENTS.register("endpoint", () -> new EndpointEnchantment(Enchantment.Rarity.VERY_RARE, EquipmentSlot.MAINHAND));
    public static final RegistryObject<Enchantment> IMPACT = ENCHANTMENTS.register("impact", () -> new ImpactEnchantment(Enchantment.Rarity.UNCOMMON, EquipmentSlot.MAINHAND));
    public static final RegistryObject<Enchantment> SHAPE_MEMORY = ENCHANTMENTS.register("shape_memory", () -> new ShapeMemoryEnchantment(Enchantment.Rarity.VERY_RARE, EquipmentSlot.MAINHAND));
}
