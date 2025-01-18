package com.teammoeg.frostedheart.bootstrap.common;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.chorda.capability.CCapabilityType;
import com.teammoeg.chorda.capability.codec.CCodecCapabilityType;
import com.teammoeg.chorda.capability.nbt.CNBTCapabilityType;
import com.teammoeg.chorda.capability.nonpresistent.CTransientCapability;
import com.teammoeg.frostedheart.content.climate.WorldClimate;
import com.teammoeg.frostedheart.content.climate.heatdevice.chunkheatdata.ChunkHeatData;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData;
import com.teammoeg.frostedheart.content.health.capability.NutritionCapability;
import com.teammoeg.frostedheart.content.health.dailykitchen.WantedFoodCapability;
import com.teammoeg.frostedheart.content.research.inspire.EnergyCore;
import com.teammoeg.frostedheart.content.robotics.logistics.RobotChunk;
import com.teammoeg.frostedheart.content.scenario.runner.ScenarioConductor;
import com.teammoeg.frostedheart.content.steamenergy.HeatEndpoint;
import com.teammoeg.frostedheart.content.steamenergy.capabilities.HeatStorageCapability;
import com.teammoeg.frostedheart.content.town.ChunkTownResourceCapability;
import com.teammoeg.frostedheart.content.utility.DeathInventoryData;
import com.teammoeg.frostedheart.content.water.capability.WaterLevelCapability;
import com.teammoeg.frostedheart.content.waypoint.capability.WaypointCapability;
import com.teammoeg.chorda.util.io.NBTSerializable;

import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.NonNullSupplier;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class FHCapabilities {
	private static List<CCapabilityType> capabilities=new ArrayList<>();
	public static final CNBTCapabilityType<WorldClimate> CLIMATE_DATA=register(WorldClimate.class);
	public static final CNBTCapabilityType<DeathInventoryData> DEATH_INV=register(DeathInventoryData.class);
	public static final CNBTCapabilityType<PlayerTemperatureData> PLAYER_TEMP=register(PlayerTemperatureData.class);
	public static final CNBTCapabilityType<EnergyCore> ENERGY=register(EnergyCore.class);
	public static final CNBTCapabilityType<ScenarioConductor> SCENARIO=register(ScenarioConductor.class);
	public static final CCodecCapabilityType<ChunkHeatData> CHUNK_HEAT=register(ChunkHeatData.class,ChunkHeatData.CODEC);
	public static final CNBTCapabilityType<HeatEndpoint> HEAT_EP=register(HeatEndpoint.class);
	public static final CTransientCapability<HeatStorageCapability> ITEM_HEAT=registerTransient(HeatStorageCapability.class);

	public static final CNBTCapabilityType<WantedFoodCapability> WANTED_FOOD=register(WantedFoodCapability.class);
	public static final CNBTCapabilityType<ChunkTownResourceCapability> CHUNK_TOWN_RESOURCE=register(ChunkTownResourceCapability.class);
	public static final CTransientCapability<RobotChunk> ROBOTIC_LOGISTIC_CHUNK=registerTransient(RobotChunk.class);
	public static final CNBTCapabilityType<WaypointCapability> WAYPOINT=register(WaypointCapability.class);
	public static final CNBTCapabilityType<WaterLevelCapability> PLAYER_WATER_LEVEL = register(WaterLevelCapability.class);
	public static final CNBTCapabilityType<NutritionCapability> PLAYER_NUTRITION = register(NutritionCapability.class);
	public static void setup() {
	
	}
	/**
	 * register capability with class, using no-arg constructor as default factory
	 * <p>
	 * */
	public static <T extends NBTSerializable> CNBTCapabilityType<T> register(Class<T> capClass){
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
	public static <T> CCodecCapabilityType<T> register(Class<T> capClass, Codec<T> codec){
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
	/**
	 * register capability with class, with provided factory in initialization
	 * */
	public static <T extends NBTSerializable> CNBTCapabilityType<T> register(Class<T> capClass, NonNullSupplier<T> factory){
		CNBTCapabilityType<T> cap=new CNBTCapabilityType<>(capClass,factory);
		capabilities.add(cap);
		return cap;
	}
	/**
	 * register Non persistent capability with class
	 * */
	public static <T> CTransientCapability<T> registerTransient(Class<T> capClass){
		CTransientCapability<T> cap=new CTransientCapability<>(capClass);
		capabilities.add(cap);
		return cap;
	}
	/**
	 * register capability with class, with provided factory in initialization, and provided codec in serialization
	 * */
	public static <T> CCodecCapabilityType<T> register(Class<T> capClass, NonNullSupplier<T> factory, Codec<T> codec){
		CCodecCapabilityType<T> cap=new CCodecCapabilityType<>(capClass,factory,codec);
		capabilities.add(cap);
		return cap;
	}
	@SubscribeEvent
	public static void onRegister(RegisterCapabilitiesEvent ev) {
		
		for(CCapabilityType cap:capabilities) {
			ev.register(cap.getCapClass());
			cap.register();
		}
	}
}
