package com.teammoeg.frostedheart;

import java.util.function.Function;

import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class FHAttributes {
	public static final DeferredRegister<Attribute> REGISTER=DeferredRegister.create(ForgeRegistries.ATTRIBUTES, FHMain.MODID);
	public static final RegistryObject<Attribute> ENV_TEMPERATURE=register("env_temp", s->new RangedAttribute(s, 0, -1000, 10000000));
	public static RegistryObject<Attribute> register(String name,Function<String,Attribute> provider){
		return REGISTER.register(name, ()->provider.apply(name));
		
	}
}
