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

package com.teammoeg.frostedresearch.insight;

import com.teammoeg.frostedresearch.FRConfig;
import com.teammoeg.frostedresearch.ResearchHooks;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
/**
 * For checking player satisfies certain insight triggers
 * */
@Mod.EventBusSubscriber
public class InsightHandler {
	
	private static final double DEG_7=Math.toRadians(7);
	private static final double PI2=Math.PI * 2;
	@SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.player instanceof ServerPlayer serverPlayer&&event.phase==TickEvent.Phase.END) {
        	if(serverPlayer.tickCount%20==0) {//check per 1 sec as the area is large
        		double x=serverPlayer.getX(),y=serverPlayer.getZ();
        		double rho=Math.sqrt(x*x+y*y);
        		double theta = Math.atan2(x, y);
        		int n=Mth.floor(rho/FRConfig.SERVER.rangeInsight.get());
        		double sec_per_ring=2*n+1;
        		double seg_angle=2*Math.PI/sec_per_ring;
        		
        		theta=(DEG_7*n+theta);
        		theta=theta - PI2 * Math.floor((theta + Math.PI) / PI2)+Math.PI;
        		int m=Mth.floor(theta/seg_angle);
        		int index=n*n+m;
        		//System.out.println(index);
        		ResearchHooks.onAreaVisited(serverPlayer, index);
        	}
           
        }
    }
}
