package com.teammoeg.frostedheart;

import java.util.UUID;
import java.util.function.Function;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class FHAttributes {
	public static final DeferredRegister<Attribute> REGISTER=DeferredRegister.create(ForgeRegistries.ATTRIBUTES, FHMain.MODID);
	public static final RegistryObject<Attribute> ENV_TEMPERATURE=register("env_temp", s->new RangedAttribute(s, 0, -1000, 10000000));
	public static final RegistryObject<Attribute> EFFECTIVE_TEMPERATURE=register("eff_temp", s->new RangedAttribute(s, 0, -1000, 10000000));
	public static final RegistryObject<Attribute> INSULATION=register("insulation", s->new RangedAttribute(s, 0, -1000, 10000000));
	public static final RegistryObject<Attribute> WIND_PROOF=register("windproof", s->new RangedAttribute(s, 0, -1000, 10000000));
	public static final RegistryObject<Attribute> HEAT_PROOF=register("heatproof", s->new RangedAttribute(s, 0, -1000, 10000000));

	public static final AttributeModifier SNOW_DRIFTER = new AttributeModifier(UUID.fromString("3c4a1c57-ed5a-482e-946e-eb0b00fe5fb0"), "frostedheart:snowshoes", 0.05, AttributeModifier.Operation.ADDITION);
	public static final AttributeModifier SPEED_SKATER = new AttributeModifier(UUID.fromString("3c4a1c57-ed5a-482e-946e-eb0b00fe5fb1"), "frostedheart:speed_skater", 0.1, AttributeModifier.Operation.ADDITION);
	public static final AttributeModifier HOT_FOOD = new AttributeModifier(UUID.fromString("3c4a1c57-ed5a-482e-946e-eb0b00fe5fb2"), "frostedheart:hot_food", 20F, AttributeModifier.Operation.ADDITION);
	public static final AttributeModifier COLD_FOOD = new AttributeModifier(UUID.fromString("3c4a1c57-ed5a-482e-946e-eb0b00fe5fb3"), "frostedheart:cold_food", -5F, AttributeModifier.Operation.ADDITION);
	public static RegistryObject<Attribute> register(String name,Function<String,Attribute> provider){
		return REGISTER.register(name, ()->provider.apply("attribute."+FHMain.MODID+"."+name));
		
	}
}
