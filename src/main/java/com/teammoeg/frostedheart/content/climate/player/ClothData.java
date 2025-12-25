package com.teammoeg.frostedheart.content.climate.player;

import java.util.Collection;

import com.google.common.collect.Multimap;
import com.teammoeg.frostedheart.bootstrap.common.FHAttributes;
import com.teammoeg.frostedheart.content.climate.data.ArmorTempData;

import lombok.Getter;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.item.ItemStack;

/**
 * Virtual cloth for calculation
 * */
public class ClothData {
	@Getter
	double insulation;
	@Getter
	double fluidResist;

	public ClothData(Multimap<Attribute, AttributeModifier> attributeStack) {
		super();
		this.insulation = sumAttributes(attributeStack.get(FHAttributes.INSULATION.get()));
		this.fluidResist = sumAttributesPercentage(attributeStack.get(FHAttributes.WIND_PROOF.get()));
	}
	public ClothData() {
		
	}
	public ClothData(ArmorTempData data) {
		this.insulation=data.getInsulation();
		this.fluidResist=data.getFluidResistance();
	}
    public static double sumAttributes(Collection<AttributeModifier> attribute) {
        double base = 0;
        double mbase = 1;
        double mtotal = 1;
        for (AttributeModifier attrib : attribute) {
            if (attrib.getOperation() == Operation.ADDITION)
                base += attrib.getAmount();
            if (attrib.getOperation() == Operation.MULTIPLY_BASE)
                mbase *= attrib.getAmount();
            if (attrib.getOperation() == Operation.MULTIPLY_TOTAL)
                mtotal *= 1 + attrib.getAmount();
        }
        return base * mbase * mtotal;
    }

    public static double sumAttributesPercentage(Collection<AttributeModifier> attribute) {
        double base = 0;
        for (AttributeModifier attrib : attribute) {
            if (attrib.getOperation() == Operation.MULTIPLY_TOTAL)
                base += attrib.getAmount();
        }
        return base;
    }
	public ClothData(double insulation, double fluidResist) {
		super();
		this.insulation = insulation;
		this.fluidResist = fluidResist;
	}
}
