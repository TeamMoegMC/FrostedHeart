package com.teammoeg.chorda.capability;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.mojang.serialization.Codec;
import com.teammoeg.chorda.Chorda;
import com.teammoeg.chorda.asm.NoArgConstructorFactory;
import com.teammoeg.chorda.capability.types.CapabilityType;
import com.teammoeg.chorda.capability.types.codec.CodecCapabilityType;
import com.teammoeg.chorda.capability.types.nbt.NBTCapabilityType;
import com.teammoeg.chorda.capability.types.nonpresistent.TransientCapability;
import com.teammoeg.chorda.io.NBTSerializable;

import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.NonNullSupplier;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid = Chorda.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CapabilityRegistry {
	private static List<CapabilityType> capabilities=new ArrayList<>();
	private static final NoArgConstructorFactory capTypeFactory=new NoArgConstructorFactory();
	private CapabilityRegistry() {
	}
	/**
	 * register capability with class, using no-arg constructor as default factory
	 * <p>
	 * */
	public static <T extends NBTSerializable> NBTCapabilityType<T> register(Class<T> capClass){
		Supplier<T> supp;
		try {
			supp=capTypeFactory.create(capClass);
			
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException("No no-arg constructor found for capability "+capClass.getSimpleName());
		} catch (InvocationTargetException e) {

			throw new RuntimeException("Could not register capability "+capClass.getSimpleName(),e.getCause());
		}
		return register(capClass,()->supp.get());
	}
	/**
	 * register capability with class, using no-arg constructor as default factory
	 * <p>
	 * */
	public static <T> CodecCapabilityType<T> register(Class<T> capClass, Codec<T> codec){
		Supplier<T> supp;
		try {
			supp=capTypeFactory.create(capClass);
			
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException("No no-arg constructor found for capability "+capClass.getSimpleName());
		} catch (InvocationTargetException e) {

			throw new RuntimeException("Could not register capability "+capClass.getSimpleName(),e.getCause());
		}
		return register(capClass,()->supp.get(),codec);
	}
	/**
	 * register capability with class, with provided factory in initialization
	 * */
	public static <T extends NBTSerializable> NBTCapabilityType<T> register(Class<T> capClass, NonNullSupplier<T> factory){
		NBTCapabilityType<T> cap=new NBTCapabilityType<>(capClass,factory);
		capabilities.add(cap);
		return cap;
	}
	/**
	 * register Non persistent capability with class
	 * */
	public static <T> TransientCapability<T> registerTransient(Class<T> capClass){
		TransientCapability<T> cap=new TransientCapability<>(capClass);
		capabilities.add(cap);
		return cap;
	}
	/**
	 * register capability with class, with provided factory in initialization, and provided codec in serialization
	 * */
	public static <T> CodecCapabilityType<T> register(Class<T> capClass, NonNullSupplier<T> factory, Codec<T> codec){
		CodecCapabilityType<T> cap=new CodecCapabilityType<>(capClass,factory,codec);
		capabilities.add(cap);
		return cap;
	}
	@SubscribeEvent
	public static void onRegister(RegisterCapabilitiesEvent ev) {
		
		for(CapabilityType cap:capabilities) {
			ev.register(cap.getCapClass());
			cap.register();
		}
	}
}
