package com.teammoeg.frostedheart;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.teammoeg.frostedheart.base.capability.IFHCapability;
import com.teammoeg.frostedheart.base.capability.codec.FHCodecCapability;
import com.teammoeg.frostedheart.base.capability.nbt.FHNBTCapability;
import com.teammoeg.frostedheart.base.capability.nonpresistent.FHNPCapability;
import com.teammoeg.frostedheart.content.climate.WorldClimate;
import com.teammoeg.frostedheart.content.climate.heatdevice.chunkheatdata.ChunkHeatData;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData;
import com.teammoeg.frostedheart.content.foods.dailykitchen.WantedFoodCapability;
import com.teammoeg.frostedheart.content.research.inspire.EnergyCore;
import com.teammoeg.frostedheart.content.robotics.logistics.RobotChunk;
import com.teammoeg.frostedheart.content.scenario.runner.ScenarioConductor;
import com.teammoeg.frostedheart.content.steamenergy.capabilities.HeatEndpoint;
import com.teammoeg.frostedheart.content.waypoint.capability.WaypointCapability;
import com.teammoeg.frostedheart.content.town.ChunkTownResourceCapability;
import com.teammoeg.frostedheart.content.utility.DeathInventoryData;
import com.teammoeg.frostedheart.util.io.NBTSerializable;

import net.minecraftforge.common.util.NonNullSupplier;

public class FHCapabilities {
	private static List<IFHCapability> capabilities=new ArrayList<>();
	public static final FHNBTCapability<WorldClimate> CLIMATE_DATA=register(WorldClimate.class);
	public static final FHNBTCapability<DeathInventoryData> DEATH_INV=register(DeathInventoryData.class);
	public static final FHNBTCapability<PlayerTemperatureData> PLAYER_TEMP=register(PlayerTemperatureData.class);
	public static final FHNBTCapability<EnergyCore> ENERGY=register(EnergyCore.class);
	public static final FHNBTCapability<ScenarioConductor> SCENARIO=register(ScenarioConductor.class);
	public static final FHCodecCapability<ChunkHeatData> CHUNK_HEAT=register(ChunkHeatData.class,ChunkHeatData.CODEC);
	public static final FHNBTCapability<HeatEndpoint> HEAT_EP=register(HeatEndpoint.class);
	public static final FHNBTCapability<WantedFoodCapability> WANTED_FOOD=register(WantedFoodCapability.class);
	public static final FHNBTCapability<ChunkTownResourceCapability> CHUNK_TOWN_RESOURCE=register(ChunkTownResourceCapability.class);
	public static final FHNPCapability<RobotChunk> ROBOTIC_LOGISTIC_CHUNK=registerNotPresist(RobotChunk.class);
	public static final FHNBTCapability<WaypointCapability> WAYPOINT=register(WaypointCapability.class);

	public static void setup() {
		for(IFHCapability cap:capabilities)
			cap.register();
	}
	/**
	 * register capability with class, using no-arg constructor as default factory
	 * <p>
	 * */
	public static <T extends NBTSerializable> FHNBTCapability<T> register(Class<T> capClass){
		Constructor<T> ctor;
		try {
			try {	
				ctor=capClass.getConstructor();
			}catch(NoSuchMethodException ex) {
				try {
					ctor=capClass.getDeclaredConstructor();
				} catch (NoSuchMethodException e) {
					throw new IllegalArgumentException("No no-arg constructor found for capability "+capClass.getSimpleName());
				}
			}
		}catch(SecurityException ex) {
			throw new RuntimeException("Can not access capability "+capClass.getSimpleName());
		}
		ctor.setAccessible(true);
		final Constructor<T> fctor=ctor;
		return register(capClass,()->{
			try {
				return fctor.newInstance();
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new RuntimeException("Can not create capability "+capClass.getSimpleName(), e);
			}
		});
	}
	/**
	 * register capability with class, using no-arg constructor as default factory
	 * <p>
	 * */
	public static <T> FHNPCapability<T> registerNotPresist(Class<T> capClass){
		Constructor<T> ctor;
		try {
			try {	
				ctor=capClass.getConstructor();
			}catch(NoSuchMethodException ex) {
				try {
					ctor=capClass.getDeclaredConstructor();
				} catch (NoSuchMethodException e) {
					throw new IllegalArgumentException("No no-arg constructor found for capability "+capClass.getSimpleName());
				}
			}
		}catch(SecurityException ex) {
			throw new RuntimeException("Can not access capability "+capClass.getSimpleName());
		}
		ctor.setAccessible(true);
		final Constructor<T> fctor=ctor;
		return registerNotPresist(capClass,()->{
			try {
				return fctor.newInstance();
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new RuntimeException("Can not create capability "+capClass.getSimpleName(), e);
			}
		});
	}
	/**
	 * register capability with class, using no-arg constructor as default factory
	 * <p>
	 * */
	public static <T> FHCodecCapability<T> register(Class<T> capClass,Codec<T> codec){
		Constructor<T> ctor;
		try {
			try {	
				ctor=capClass.getConstructor();
			}catch(NoSuchMethodException ex) {
				try {
					ctor=capClass.getDeclaredConstructor();
				} catch (NoSuchMethodException e) {
					throw new IllegalArgumentException("No no-arg constructor found for capability "+capClass.getSimpleName());
				}
			}
		}catch(SecurityException ex) {
			throw new RuntimeException("Can not access capability "+capClass.getSimpleName());
		}
		ctor.setAccessible(true);
		final Constructor<T> fctor=ctor;
		return register(capClass,()->{
			try {
				return fctor.newInstance();
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new RuntimeException("Can not create capability "+capClass.getSimpleName(), e);
			}
		},codec);
	}

	public static <T extends NBTSerializable> FHNBTCapability<T> register(Class<T> capClass,NonNullSupplier<T> sup){
		FHNBTCapability<T> cap=new FHNBTCapability<>(capClass,sup);
		capabilities.add(cap);
		return cap;
	}
	public static <T> FHNPCapability<T> registerNotPresist(Class<T> capClass,NonNullSupplier<T> sup){
		FHNPCapability<T> cap=new FHNPCapability<>(capClass,sup);
		capabilities.add(cap);
		return cap;
	}
	public static <T> FHCodecCapability<T> register(Class<T> capClass,NonNullSupplier<T> sup,Codec<T> codec){
		FHCodecCapability<T> cap=new FHCodecCapability<>(capClass,sup,codec);
		capabilities.add(cap);
		return cap;
	}
}
