package com.teammoeg.frostedheart.content.utility.heatervest;

import org.jetbrains.annotations.NotNull;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public class HeaterVestExtension implements IClientItemExtensions {
	public static HumanoidModel<?> MODEL;
	@Override
	public @NotNull HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
		return MODEL;
	}

	public HeaterVestExtension() {

	}

}
