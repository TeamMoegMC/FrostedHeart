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

package com.teammoeg.chorda.capability;

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
/**
 * Forge能力系统的中央注册表。
 * 管理所有能力类型的注册，支持基于反射的无参构造器实例化，以及手动提供工厂方法的注册方式。
 * 支持NBT序列化、Codec序列化和非持久化三种能力类型的注册。
 * <p>
 * Central registry for the Forge capability system.
 * Manages registration of all capability types, supporting reflection-based no-arg constructor
 * instantiation as well as manual factory method registration.
 * Supports three capability type registrations: NBT serialized, Codec serialized, and transient (non-persistent).
 */
@Mod.EventBusSubscriber(modid = Chorda.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CapabilityRegistry {
	/** 所有已注册的能力类型列表 / List of all registered capability types */
	private static List<CapabilityType> capabilities=new ArrayList<>();
	/** 无参构造器工厂，用于通过反射创建能力实例 / No-arg constructor factory for creating capability instances via reflection */
	private static final NoArgConstructorFactory capTypeFactory=new NoArgConstructorFactory();
	/**
	 * 私有构造器，防止实例化此工具类。
	 * <p>
	 * Private constructor to prevent instantiation of this utility class.
	 */
	private CapabilityRegistry() {
	}
	/**
	 * 使用无参构造器作为默认工厂，注册一个基于NBT序列化的能力。
	 * 通过反射查找目标类的无参构造器来创建实例。
	 * <p>
	 * Register an NBT-serialized capability using the no-arg constructor as the default factory.
	 * Uses reflection to find the target class's no-arg constructor for instance creation.
	 *
	 * @param capClass 能力类的Class对象 / The Class object of the capability
	 * @param <T> 能力类型，必须实现NBTSerializable / The capability type, must implement NBTSerializable
	 * @return 注册后的NBT能力类型 / The registered NBT capability type
	 * @throws IllegalArgumentException 如果找不到无参构造器 / if no no-arg constructor is found
	 * @throws RuntimeException 如果构造器调用失败 / if the constructor invocation fails
	 */
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
	 * 使用无参构造器作为默认工厂，注册一个基于Codec序列化的能力。
	 * 通过反射查找目标类的无参构造器来创建实例。
	 * <p>
	 * Register a Codec-serialized capability using the no-arg constructor as the default factory.
	 * Uses reflection to find the target class's no-arg constructor for instance creation.
	 *
	 * @param capClass 能力类的Class对象 / The Class object of the capability
	 * @param codec 用于序列化/反序列化的Codec / The Codec used for serialization/deserialization
	 * @param <T> 能力类型 / The capability type
	 * @return 注册后的Codec能力类型 / The registered Codec capability type
	 * @throws IllegalArgumentException 如果找不到无参构造器 / if no no-arg constructor is found
	 * @throws RuntimeException 如果构造器调用失败 / if the constructor invocation fails
	 */
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
	 * 使用提供的工厂方法注册一个基于NBT序列化的能力。
	 * <p>
	 * Register an NBT-serialized capability with a provided factory for initialization.
	 *
	 * @param capClass 能力类的Class对象 / The Class object of the capability
	 * @param factory 用于创建能力实例的工厂 / The factory used to create capability instances
	 * @param <T> 能力类型，必须实现NBTSerializable / The capability type, must implement NBTSerializable
	 * @return 注册后的NBT能力类型 / The registered NBT capability type
	 */
	public static <T extends NBTSerializable> NBTCapabilityType<T> register(Class<T> capClass, NonNullSupplier<T> factory){
		NBTCapabilityType<T> cap=new NBTCapabilityType<>(capClass,factory);
		capabilities.add(cap);
		return cap;
	}
	/**
	 * 注册一个非持久化（瞬态）的能力类型。
	 * 该能力不会被序列化保存，仅在运行时存在。
	 * <p>
	 * Register a non-persistent (transient) capability type.
	 * This capability will not be serialized or saved, and only exists at runtime.
	 *
	 * @param capClass 能力类的Class对象 / The Class object of the capability
	 * @param <T> 能力类型 / The capability type
	 * @return 注册后的瞬态能力类型 / The registered transient capability type
	 */
	public static <T> TransientCapability<T> registerTransient(Class<T> capClass){
		TransientCapability<T> cap=new TransientCapability<>(capClass);
		capabilities.add(cap);
		return cap;
	}
	/**
	 * 使用提供的工厂方法和Codec注册一个基于Codec序列化的能力。
	 * <p>
	 * Register a Codec-serialized capability with a provided factory for initialization
	 * and a provided Codec for serialization.
	 *
	 * @param capClass 能力类的Class对象 / The Class object of the capability
	 * @param factory 用于创建能力实例的工厂 / The factory used to create capability instances
	 * @param codec 用于序列化/反序列化的Codec / The Codec used for serialization/deserialization
	 * @param <T> 能力类型 / The capability type
	 * @return 注册后的Codec能力类型 / The registered Codec capability type
	 */
	public static <T> CodecCapabilityType<T> register(Class<T> capClass, NonNullSupplier<T> factory, Codec<T> codec){
		CodecCapabilityType<T> cap=new CodecCapabilityType<>(capClass,factory,codec);
		capabilities.add(cap);
		return cap;
	}
	/**
	 * 处理Forge能力注册事件，将所有已注册的能力类型注册到Forge能力系统中。
	 * <p>
	 * Handles the Forge capability registration event, registering all stored capability types
	 * with the Forge capability system.
	 *
	 * @param ev Forge能力注册事件 / The Forge capability registration event
	 */
	@SubscribeEvent
	public static void onRegister(RegisterCapabilitiesEvent ev) {
		
		for(CapabilityType cap:capabilities) {
			ev.register(cap.getCapClass());
			cap.register();
		}
	}
}
