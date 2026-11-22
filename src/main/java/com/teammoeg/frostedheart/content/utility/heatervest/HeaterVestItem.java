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

package com.teammoeg.frostedheart.content.utility.heatervest;

import com.teammoeg.chorda.capability.CapabilityDispatchBuilder;
import com.teammoeg.chorda.creativeTab.CreativeTabItemHelper;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.client.FHTabs;
import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.content.climate.player.BodyHeatingCapability;
import com.teammoeg.frostedheart.content.climate.player.HeatingDeviceContext;
import com.teammoeg.frostedheart.content.climate.player.HeatingDeviceSlot;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData.BodyPart;
import com.teammoeg.frostedheart.content.steamenergy.capabilities.HeatStorageCapability;
import com.teammoeg.frostedheart.item.FHBaseItem;
import com.teammoeg.frostedheart.util.Lang;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * Heater Vest: wear it to warm yourself from the coldness.
 * 加温背心：穿戴抵御寒冷
 */
public class HeaterVestItem extends FHBaseItem {
    public static final String NBT_HEATER_VEST = FHMain.MODID + "heater_vest";
    private static final String ENERGY_KEY="steam";
    private static final int STEAM_CAPACITY = 30_000;
    private static final int FULL_POWER_SECONDS = 3_600;
    private static final int CHARGER_POINTS_PER_UNIT = 288;
    private static final double STANDBY_HEAT = 0.05D;
    private static final float MAX_POWER_W = 275.0F;
    private static final double HEAT_PER_STORED_UNIT_W =
            MAX_POWER_W / ((double) STEAM_CAPACITY / FULL_POWER_SECONDS - STANDBY_HEAT);
    private static final float CONTROL_GAIN_W_PER_K = 55.0F;
    public HeaterVestItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> list, TooltipFlag flag) {
        String stored = FHCapabilities.ITEM_HEAT.getCapability(stack).map(t->{
			return t.getEnergyStored();
		}).orElse(0F) + "/" + this.getMaxEnergyStored(stack);
        list.add(Lang.translateTooltip("charger.dock_supply").withStyle(ChatFormatting.GRAY));
        list.add(Lang.translateTooltip("steam_stored", stored).withStyle(ChatFormatting.GOLD));
    }


    @Override
	public void fillItemCategory(CreativeTabItemHelper helper) {
		if(helper.isType(FHTabs.TOOLS)) {
        	helper.accept(new ItemStack(this));
            ItemStack is = new ItemStack(this);
            FHCapabilities.ITEM_HEAT.getCapability(is).ifPresent(t->t.receiveEnergy(STEAM_CAPACITY, false));
            helper.accept(is);
	        
		}
	}

	@OnlyIn(Dist.CLIENT)
    @Override
	public void initializeClient(Consumer<IClientItemExtensions> consumer) {
		super.initializeClient(consumer);
		consumer.accept(new HeaterVestExtension());
	}

	@Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return FHMain.rl("textures/models/heater_vest.png").toString();
    }

    @Nullable
    @Override
    public EquipmentSlot getEquipmentSlot(ItemStack stack) {
        return EquipmentSlot.CHEST;
    }

    public int getMaxEnergyStored(ItemStack container) {
        return STEAM_CAPACITY;
    }


	@Override
	public ICapabilityProvider initCapabilities(ItemStack stack,CompoundTag nbt) {
		return CapabilityDispatchBuilder.builder()
				.add(FHCapabilities.ITEM_HEAT,
                        () -> new HeatStorageCapability(stack, STEAM_CAPACITY, CHARGER_POINTS_PER_UNIT))
				.add(FHCapabilities.EQUIPMENT_HEATING, ()->new BodyHeatingCapability() {

					@Override
					public void tickHeating(HeatingDeviceSlot slot, ItemStack stack, HeatingDeviceContext data) {
						if (slot.isHand()) return;
						double elapsedSeconds = data.getElapsedSeconds();
						if (!(elapsedSeconds > 0.0D)) return;
						double requestedPowerW = Math.min(MAX_POWER_W,
								data.remainingHeatingDemandW(BodyPart.TORSO, CONTROL_GAIN_W_PER_K));
						if (!(requestedPowerW > 0D)) return;
						HeatStorageCapability storage = FHCapabilities.ITEM_HEAT.getCapability(stack).orElse(null);
						if (storage == null) return;

						double requestedUnits = (STANDBY_HEAT + requestedPowerW / HEAT_PER_STORED_UNIT_W)
								* elapsedSeconds;
						double extracted = storage.extractPreciseEnergy(requestedUnits);
						data.addPower(BodyPart.TORSO, Math.min(requestedPowerW,
								Math.max(0D, extracted / elapsedSeconds - STANDBY_HEAT)
										* HEAT_PER_STORED_UNIT_W));
					}

					@Override
					public boolean isControlled() {
						return true;
					}

					@Override
					public float getMaxPowerW(ItemStack stack) {
						return FHCapabilities.ITEM_HEAT.getCapability(stack)
								.map(value -> (float) Math.min(MAX_POWER_W, Math.max(0D,
										value.getEnergyStored() - STANDBY_HEAT) * HEAT_PER_STORED_UNIT_W))
								.orElse(0F);
					}

					@Override
					public float getMinPowerW(ItemStack stack) {
						return 0;
					}
					
				})
				.build();
		
	}


}
