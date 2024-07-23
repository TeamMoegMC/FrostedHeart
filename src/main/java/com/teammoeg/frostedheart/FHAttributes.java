package com.teammoeg.frostedheart;

import java.util.function.Function;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.fmllegacy.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class FHAttributes {
	public static final DeferredRegister<Attribute> REGISTER=DeferredRegister.create(ForgeRegistries.ATTRIBUTES, FHMain.MODID);
	public static final RegistryObject<Attribute> ENV_TEMPERATURE=register("env_temp", s->new RangedAttribute(s, 0, -1000, 10000000));
	public static final RegistryObject<Attribute> EFFECTIVE_TEMPERATURE=register("eff_temp", s->new RangedAttribute(s, 0, -1000, 10000000));
	public static final RegistryObject<Attribute> INSULATION=register("insulation", s->new RangedAttribute(s, 0, -1000, 10000000));
	public static final RegistryObject<Attribute> WIND_PROOF=register("windproof", s->new RangedAttribute(s, 0, -1000, 10000000));
	public static final RegistryObject<Attribute> HEAT_PROOF=register("heatproof", s->new RangedAttribute(s, 0, -1000, 10000000));
	public static RegistryObject<Attribute> register(String name,Function<String,Attribute> provider){
		return REGISTER.register(name, ()->provider.apply("attribute."+FHMain.MODID+"."+name));
		
	}
}
