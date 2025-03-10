/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.climate.tooltips;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import com.simibubi.create.foundation.item.TooltipModifier;
import com.teammoeg.chorda.lang.Components;
import com.teammoeg.chorda.lang.LangBuilder;
import com.teammoeg.chorda.util.Lang;
import com.teammoeg.frostedheart.bootstrap.common.FHAttributes;
import com.teammoeg.frostedheart.content.climate.data.ArmorTempData;
import com.teammoeg.frostedheart.content.climate.player.BodyPartData;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData.BodyPart;
import com.teammoeg.frostedheart.util.client.KeyControlledDesc;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

public class ArmorTempStats implements TooltipModifier {
	protected final Item item;

	public ArmorTempStats(Item item) {
		this.item = item;
	}

	@Nullable
	public static ArmorTempStats create(Item item) {

		return new ArmorTempStats(item);
	}

	@Override
	public void modify(ItemTooltipEvent context) {

		if (isInsulation(context)) {

			KeyControlledDesc desc = new KeyControlledDesc(() -> getStats(context.getItemStack(), context.getEntity()),
					GLFW.GLFW_KEY_S, "S", "holdForTemperature");
			List<Component> tooltip = context.getToolTip();
			tooltip.add(Components.immutableEmpty());
			tooltip.addAll(desc.getCurrentLines());
		}
	}

	public static boolean isInsulation(ItemTooltipEvent context) {
		for (EquipmentSlot es : EquipmentSlot.values()) {
			if (!context.getItemStack().getAttributeModifiers(es).get(FHAttributes.INSULATION.get()).isEmpty()) {
				return true;
			}
			/*
			 * if(!context.getItemStack().getAttributeModifiers(es).get(FHAttributes.
			 * HEAT_PROOF.get()).isEmpty()) {
			 * return true;
			 * }
			 */
			if (!context.getItemStack().getAttributeModifiers(es).get(FHAttributes.WIND_PROOF.get()).isEmpty()) {
				return true;
			}
		}
		return ArmorTempData.cacheList.containsKey(context.getItemStack().getItem());
	}
	static final Function<Double,List<BodyPart>> func=t->new ArrayList<>();

	public static List<Component> getStats(ItemStack stack, Player player) {
		Map<Double, List<BodyPart>> InsulationBySlot = new HashMap<>();
		Map<Double, List<BodyPart>> WindProofBySlot = new HashMap<>();
		for (BodyPart es : BodyPart.values()) {
			ArmorTempData at=ArmorTempData.getData(stack, es);
			boolean hasInsulation=false;
			boolean hasWindProof=false;
			if(es.slot.isArmor()) {
				Collection<AttributeModifier> insattrs=stack.getAttributeModifiers(es.slot).get(FHAttributes.INSULATION.get());
				if(!insattrs.isEmpty()) {
					InsulationBySlot.computeIfAbsent(BodyPartData.sumAttributes(insattrs), func).add(es);
					hasInsulation=true;
				}
				Collection<AttributeModifier> wpattrs=stack.getAttributeModifiers(es.slot).get(FHAttributes.WIND_PROOF.get());
				if(!wpattrs.isEmpty()) {
					WindProofBySlot.computeIfAbsent(BodyPartData.sumAttributesPercentage(insattrs), func).add(es);
					hasWindProof=true;
				}
			}
			if(at!=null) {
				if(!hasInsulation) {
					InsulationBySlot.computeIfAbsent((double) at.insulation(), func).add(es);
				}
				if(!hasWindProof) {
					WindProofBySlot.computeIfAbsent((double) at.wind_proof(), func).add(es);
				}
			}

		}
		if(!InsulationBySlot.isEmpty()) {
			for(Entry<Double, List<BodyPart>> i:InsulationBySlot.entrySet()) {
				LangBuilder lb=Lang.builder();
				boolean isFirst=true;
				for(BodyPart bp:i.getValue()) {
					if(isFirst) {
						isFirst=false;
					}else
						lb.translate("tooltip", "armor_temp_split");
					lb.add(bp.getName());
				}
				
				Lang.tooltip("armor_temp_slots",lb);
			}
		}
		return null;
	}
}
