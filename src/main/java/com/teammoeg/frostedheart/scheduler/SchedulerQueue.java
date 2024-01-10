package com.teammoeg.frostedheart.scheduler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.teammoeg.frostedheart.FHConfig;

import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

public class SchedulerQueue {
	static Map<RegistryKey<World>,SchedulerQueue> queues=new HashMap<>();
	ArrayList<ScheduledData> tasks=new ArrayList<>();

	int lastpos;
	double tasksPerTick=FHConfig.SERVER.taskPerTick.get();
	public void add(BlockPos pos) {
		for(int i=0;i<tasks.size();i++) {
			if(tasks.get(i).pos.equals(pos)) {
				return;
			}
		}
		tasks.add(new ScheduledData(pos));
	}
	public static void add(TileEntity te) {
		queues.computeIfAbsent(te.getWorld().getDimensionKey(), e->new SchedulerQueue())
		.add(te.getPos());
		
	}
	public void remove(BlockPos pos) {
		for(int i=0;i<tasks.size();i++) {
			if(tasks.get(i).pos.equals(pos)) {
				if(i<lastpos) {
					lastpos--;
				}
				tasks.remove(i);
				return;
			}
		}
	}
	public void tick(ServerWorld world) {
		//count count of tasks
		int taskNum=(int) tasksPerTick;
		double fracNum=MathHelper.frac(tasksPerTick);
		if(world.getRandom().nextDouble()<fracNum) {
			taskNum++;
		}
		if(tasks.isEmpty())return;
		//run tasks
		int curpos=lastpos;
		while(taskNum>0) {
			ScheduledData data=tasks.get(curpos);
			TileEntity te=Utils.getExistingTileEntity(world,data.pos);
			if((te instanceof IScheduledTaskTE)) {
				((IScheduledTaskTE) te).executeTask();
			}else {
				data.forRemoval=true;
			}
			taskNum--;
			curpos++;
			if(curpos>=tasks.size()) {
				curpos=0;
			}
			if(curpos==lastpos)break;
		}
		lastpos=curpos;
		//remove invalid tasks
		for(int i=0;i<tasks.size();i++) {
			if(tasks.get(i).forRemoval) {
				if(i<lastpos) {
					lastpos--;
				}
				tasks.remove(i);
				i--;
			}
		}
	}
	public static void tickAll(ServerWorld serverWorld) {
		SchedulerQueue q=queues.get(serverWorld.getDimensionKey());
		if(q!=null)
			q.tick(serverWorld);
	}
}
