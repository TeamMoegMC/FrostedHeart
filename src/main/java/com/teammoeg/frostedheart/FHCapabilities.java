package com.teammoeg.frostedheart;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import com.teammoeg.frostedheart.base.capability.FHCapability;
import com.teammoeg.frostedheart.content.climate.WorldClimate;
import com.teammoeg.frostedheart.content.climate.chunkheatdata.ChunkHeatData;
import com.teammoeg.frostedheart.content.climate.data.DeathInventoryData;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData;
import com.teammoeg.frostedheart.content.foods.dailykitchen.WantedFoodCapability;
import com.teammoeg.frostedheart.content.research.inspire.EnergyCore;
import com.teammoeg.frostedheart.content.scenario.runner.ScenarioConductor;
import com.teammoeg.frostedheart.content.steamenergy.capabilities.HeatEndpoint;
import com.teammoeg.frostedheart.util.io.NBTSerializable;

import net.minecraftforge.common.util.NonNullSupplier;

public class FHCapabilities {
	private static List<FHCapability<?>> capabilities=new ArrayList<>();
	public static final FHCapability<WorldClimate> CLIMATE_DATA=register(WorldClimate.class);
	public static final FHCapability<DeathInventoryData> DEATH_INV=register(DeathInventoryData.class);
	public static final FHCapability<PlayerTemperatureData> PLAYER_TEMP=register(PlayerTemperatureData.class);
	public static final FHCapability<EnergyCore> ENERGY=register(EnergyCore.class);
	public static final FHCapability<ScenarioConductor> SCENARIO=register(ScenarioConductor.class);
	public static final FHCapability<ChunkHeatData> CHUNK_HEAT=register(ChunkHeatData.class);
	public static final FHCapability<HeatEndpoint> HEAT_EP=register(HeatEndpoint.class);
	public static final FHCapability<WantedFoodCapability> WANTED_FOOD=register(WantedFoodCapability.class);
	
	public static void setup() {
		for(FHCapability<?> cap:capabilities)
			cap.register();
	}
	/**
	 * register capability with class, using no-arg constructor as default factory
	 * <p>
	 * */
	public static <T extends NBTSerializable> FHCapability<T> register(Class<T> capClass){
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
		FHCapability<T> cap=new FHCapability<>(capClass,()->{
			try {
				return fctor.newInstance();
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new RuntimeException("Can not create capability "+capClass.getSimpleName(), e);
			}
		});
		capabilities.add(cap);
		return cap;
	}

	public static <T extends NBTSerializable> FHCapability<T> register(Class<T> capClass,NonNullSupplier<T> sup){
		FHCapability<T> cap=new FHCapability<>(capClass,sup);
		capabilities.add(cap);
		return cap;
	}
}
