package com.teammoeg.frostedheart.content.climate;

import java.util.UUID;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.teammoeg.frostedheart.FHAttributes;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.data.ArmorTempData;
import com.teammoeg.frostedheart.util.EquipmentCuriosSlotType;

import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.AttributeModifier.Operation;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;

public class ArmorTempCurios implements ICurio {
	ArmorTempData data;
	public ArmorTempCurios(ArmorTempData data) {
		this.data=data;
	}

	@Override
	public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext slotContext, UUID uuid) {
		Multimap<Attribute, AttributeModifier> mm=HashMultimap.create();
        if(data!=null) {
        	EquipmentCuriosSlotType slot=EquipmentCuriosSlotType.fromCurios(slotContext.getIdentifier());
        	if(data.getInsulation()!=0)
        		mm.put(FHAttributes.INSULATION.get(), new AttributeModifier(uuid,slot.getKey(), data.getInsulation(), Operation.ADDITION));
        	if(data.getColdProof()!=0)
        		mm.put(FHAttributes.WIND_PROOF.get(), new AttributeModifier(uuid,slot.getKey(), data.getColdProof() , Operation.ADDITION));
        	if(data.getHeatProof()!=0)
        		mm.put(FHAttributes.HEAT_PROOF.get(), new AttributeModifier(uuid,slot.getKey(), data.getHeatProof() , Operation.ADDITION));
        }
		return ICurio.super.getAttributeModifiers(slotContext, uuid);
	}

}
