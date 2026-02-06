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

package com.teammoeg.frostedheart.content.climate.player;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.teammoeg.chorda.util.CDistHelper;
import com.teammoeg.chorda.util.CUtils;
import com.teammoeg.frostedheart.content.climate.player.SurroundingTemperatureSimulator.SimulationResult;

import lombok.Getter;
import net.minecraft.server.level.ServerPlayer;

public class TemperatureThreadingPool {
	Map<UUID,Future<SimulationResult>> resultMap;
	ExecutorService scheduler;
	/**
	 * Used to benchmark temperature calculation performance
	 * */
	@Getter
	int tasksRemain;
	public TemperatureThreadingPool(int threadNum) {
		if(threadNum!=0) {
			scheduler=Executors.newFixedThreadPool(threadNum, CUtils.makeThreadFactory("block-temperature-calculation", true));
			resultMap=new HashMap<>();
		}
	}
	public boolean tryCommitWork(ServerPlayer player) {
		//System.out.println("committing work for "+player);
		double x=player.getX();
		double y=player.getEyeY()-0.7;
		double z=player.getZ();
		if(scheduler==null) {
			SurroundingTemperatureSimulator sts=new SurroundingTemperatureSimulator(player.serverLevel(),player.getX(),player.getEyeY(),player.getZ(),false);
			submitPlayerData(player, sts.getBlockTemperatureAndWind(x, y, z));
			return true;
		}else if(!resultMap.containsKey(player.getUUID())){
			//System.out.println("committing work for "+player.getName().getString());
			SurroundingTemperatureSimulator sts=new SurroundingTemperatureSimulator(player.serverLevel(),player.getX(),player.getEyeY(),player.getZ(),true);
			resultMap.put(player.getUUID(), scheduler.submit(()->sts.getBlockTemperatureAndWind(x, y, z)));
			return true;
		}
		return false;
	}
	public void tick() {
		if(resultMap!=null) {
			int tasksRemain=0;
			for(Iterator<Entry<UUID, Future<SimulationResult>>> it=resultMap.entrySet().iterator();it.hasNext();) {
				Entry<UUID, Future<SimulationResult>> entry=it.next();
				if(entry.getValue().isDone()) {
					it.remove();
					
					ServerPlayer player=CDistHelper.getServer().getPlayerList().getPlayer(entry.getKey());
					//System.out.println("work has done for"+player.getName().getString());
					if(player!=null) {
						try {
							this.submitPlayerData(player, entry.getValue().get());
						} catch (InterruptedException e) {//this error should not happen
							e.printStackTrace();
						} catch (ExecutionException e) {//internal calculation cause exception, we should throw it to cause crash
							throw new RuntimeException(e.getCause());
						}
					}
				}else tasksRemain++;
			}
			this.tasksRemain=tasksRemain;
		}
	}
	public void close() {
		//no need to wait till they shutdown as they are daemon
		scheduler.shutdown();
		
	}
	private void submitPlayerData(ServerPlayer player,SimulationResult result) {
		//System.out.println(result);
		PlayerTemperatureData.getCapability(player).ifPresent(t->{
			t.blockTemp=result.blockTemp();
			t.windStrengh=result.windStrengh();
		});
	}
}
