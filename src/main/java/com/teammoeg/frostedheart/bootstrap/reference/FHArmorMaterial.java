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

package com.teammoeg.frostedheart.bootstrap.reference;

import java.util.function.Supplier;

import com.teammoeg.chorda.Chorda;
import com.teammoeg.chorda.CompatModule;
import com.teammoeg.chorda.util.CRegistryHelper;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHItems;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem.Type;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.fml.ModList;

public enum FHArmorMaterial implements ArmorMaterial {
    HIDE("hide", 5, new int[]{1, 2, 3, 1}, 5, SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 0.0F, () -> Ingredient.of(FHItems.raw_hide.get())),
    RABBIT("rabbit", 3, new int[]{1, 2, 3, 1}, 5, SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 0.0F, () -> Ingredient.of(Items.RABBIT_HIDE)),
    FOX("fox", 4, new int[]{1, 2, 3, 1}, 5, SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 0.0F, () -> Ingredient.of(FHItems.fox_hide.get())),
    WOLF("wolf", 5, new int[]{1, 2, 3, 1}, 5, SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 0.0F, () -> Ingredient.of(FHItems.wolf_hide.get())),
    POLAR_BEAR("polar_bear", 6, new int[]{1, 2, 3, 1}, 5, SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 0.0F, () -> Ingredient.of(FHItems.polar_bear_hide.get())),
    HAY("hay", 4, new int[]{1, 1, 1, 1}, 5, SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 0.0F, () -> Ingredient.of(CompatModule.isCharcoalPitLoaded() ? CRegistryHelper.getItem(new ResourceLocation("charcoal_pit", "straw")) : Items.WHEAT)),
    WOOL("wool", 6, new int[]{1, 2, 3, 1}, 5, SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 0.0F, () -> Ingredient.of(Items.WHITE_WOOL)),
    WEATHER("weather", 15, new int[]{2, 5, 6, 2}, 9, SoundEvents.ARMOR_EQUIP_IRON, 0.0F, 0.0F, () -> Ingredient.of(Items.IRON_INGOT)),
    SPACESUIT("spacesuit",15, new int[]{2, 5, 6, 2}, 9, SoundEvents.ARMOR_EQUIP_IRON, 0.0F, 0.0F, () -> Ingredient.of());

    private static final int[] MAX_DAMAGE_ARRAY = new int[]{13, 15, 16, 11};
    private final String name;
    private final int maxDamageFactor;
    private final int[] damageReductionAmountArray;
    private final int enchantability;
    private final SoundEvent soundEvent;
    private final float toughness;
    private final float knockbackResistance;
    private final Lazy<Ingredient> repairMaterial;

    FHArmorMaterial(String name, int maxDamageFactor, int[] damageReductionAmountArray, int enchantability, SoundEvent soundEvent, float toughness, float knockbackResistance, Supplier<Ingredient> repairMaterial) {
        this.name = name;
        this.maxDamageFactor = maxDamageFactor;
        this.damageReductionAmountArray = damageReductionAmountArray;
        this.enchantability = enchantability;
        this.soundEvent = soundEvent;
        this.toughness = toughness;
        this.knockbackResistance = knockbackResistance;
        this.repairMaterial = Lazy.of(repairMaterial);
    }

    public int getDefenseForSlot(EquipmentSlot slotIn) {
        return this.damageReductionAmountArray[slotIn.getIndex()];
    }

    public int getDurabilityForSlot(EquipmentSlot slotIn) {
        return MAX_DAMAGE_ARRAY[slotIn.getIndex()] * this.maxDamageFactor;
    }

    public int getEnchantmentValue() {
        return this.enchantability;
    }

    public float getKnockbackResistance() {
        return this.knockbackResistance;
    }

    @OnlyIn(Dist.CLIENT)
    public String getName() {
        return FHMain.MODID + ":" + this.name;
    }

    public Ingredient getRepairIngredient() {
        return this.repairMaterial.get();
    }

    public SoundEvent getEquipSound() {
        return this.soundEvent;
    }

    public float getToughness() {
        return this.toughness;
    }

	@Override
	public int getDurabilityForType(Type pType) {
		 return getDurabilityForSlot(pType.getSlot());
	}

	@Override
	public int getDefenseForType(Type pType) {
		return getDefenseForSlot(pType.getSlot());
	}
}