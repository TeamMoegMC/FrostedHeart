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

import com.teammoeg.chorda.util.CDistHelper;
import com.teammoeg.chorda.util.CUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import lombok.Getter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class TemperatureThreadingPool {
	Object2ObjectOpenHashMap<UUID,Future<SurroundingTemperatureSimulator.SimulationResult>> resultMap;
	/**
	 * 每个玩家上次提交模拟时的输入快照（仅主线程访问，无需同步）。
	 * <p>
	 * 该模拟是完全确定性的：rnd 种子 = BlockPos.asLong(玩家坐标) ^ (gameTime&gt;&gt;6)，
	 * 速度方向表为静态常量 ⟹ 结果由 (level, 精确坐标, 种子窗口, 区域方块) 唯一决定。
	 * 坐标与种子窗口均未变时重算结果与上次逐 bit 相同，直接跳过整个
	 * （含主线程上的区块快照复制在内的）提交过程。
	 * 玩家静止而区域内方块被改动是最坏情况：旧结果最多沿用 64 tick（种子轮换强制失效），
	 * 相对 167s 的体温时间常数无感。
	 */
	private final Map<UUID, LastInput> lastInputs = new Object2ObjectOpenHashMap<>();
	ExecutorService scheduler;
	/**
	 * Used to benchmark temperature calculation performance
	 * */
	@Getter
	int tasksRemain;

	private static final class LastInput {
		final ServerLevel level;
		final double x, y, z;
		final long seedWindow;

		LastInput(ServerLevel level, double x, double y, double z, long seedWindow) {
			this.level = level;
			this.x = x;
			this.y = y;
			this.z = z;
			this.seedWindow = seedWindow;
		}
	}
	public TemperatureThreadingPool(int threadNum) {
		if(threadNum!=0) {
			scheduler=Executors.newFixedThreadPool(threadNum, CUtils.makeThreadFactory("block-temperature-calculation", true));
			resultMap=new Object2ObjectOpenHashMap<>();
		}
	}
	public boolean tryCommitWork(ServerPlayer player) {
		//System.out.println("committing work for "+player);
		double x=player.getX();
		double y=player.getEyeY()-0.7;
		double z=player.getZ();
		ServerLevel level=player.serverLevel();
		long seedWindow=level.getGameTime()>>6;
		UUID id=player.getUUID();

		// 输入未变则跳过：模拟是确定性的（见 lastInputs 注释），
		// 重算结果与 PlayerTemperatureData 中现有值逐 bit 相同，无需任何工作。
		LastInput last=lastInputs.get(id);
		if(last!=null&&last.level==level&&last.seedWindow==seedWindow
				&&last.x==x&&last.y==y&&last.z==z) {
			return true;
		}

		if(scheduler==null) {

			SurroundingTemperatureSimulator sts=new SurroundingTemperatureSimulator(level,player.getX(),player.getEyeY(),player.getZ(),false);
			submitPlayerData(player, sts.getBlockTemperatureAndWind(x, y, z));
			lastInputs.put(id, new LastInput(level, x, y, z, seedWindow));
			return true;
		}else if(!resultMap.containsKey(id)){
			//System.out.println("committing work for "+player.getName().getString());

			SurroundingTemperatureSimulator sts=new SurroundingTemperatureSimulator(level,player.getX(),player.getEyeY(),player.getZ(),true);
			resultMap.put(id, scheduler.submit(()->sts.getBlockTemperatureAndWind(x, y, z)));
			lastInputs.put(id, new LastInput(level, x, y, z, seedWindow));
			return true;
		}
		return false;
	}
	public void tick() {
		if(resultMap!=null) {
			int tasksRemain=0;
			for(ObjectIterator<Object2ObjectMap.Entry<UUID, Future<SurroundingTemperatureSimulator.SimulationResult>>> it = resultMap.object2ObjectEntrySet().iterator(); it.hasNext();) {
				Object2ObjectMap.Entry<UUID, Future<SurroundingTemperatureSimulator.SimulationResult>> entry=it.next();
				if(entry.getValue().isDone()) {
					// fastutil 的 MapEntry 是索引式：remove() 后索引置 -1 不可再访问，须先取键值
					UUID id = entry.getKey();
					Future<SurroundingTemperatureSimulator.SimulationResult> done = entry.getValue();
					it.remove();

					ServerPlayer player=CDistHelper.getServer().getPlayerList().getPlayer(id);
					//System.out.println("work has done for"+player.getName().getString());
					if(player!=null) {
						try {
							this.submitPlayerData(player, done.get());
						} catch (InterruptedException e) {//this error should not happen
							e.printStackTrace();
						} catch (ExecutionException e) {//internal calculation cause exception, we should throw it to cause crash
							throw new RuntimeException(e.getCause());
						}
					}else {
						// 玩家已离线：清理输入快照，避免缓慢泄漏
						lastInputs.remove(id);
					}
				}else tasksRemain++;
			}
			this.tasksRemain=tasksRemain;
		}
	}
	public void close() {
		//no need to wait till they shutdown as they are daemon
//		scheduler.shutdown();

		if (scheduler != null) {
			SurroundingTemperatureSimulator.cleanup();
			scheduler.shutdown();
		} else {
			SurroundingTemperatureSimulator.cleanup();
		}
	}
	private void submitPlayerData(ServerPlayer player, SurroundingTemperatureSimulator.SimulationResult result) {
		//System.out.println(result);
		PlayerTemperatureData.getCapability(player).ifPresent(t->{
			t.blockTemp=result.blockTemp();
			t.windStrengh=result.windStrength();
		});
	}
}
