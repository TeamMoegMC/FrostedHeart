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

package com.teammoeg.frostedheart.content.robotics.logistics;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.teammoeg.chorda.io.CodecUtil;
import com.teammoeg.chorda.io.NBTSerializable;
import com.teammoeg.chorda.util.CUtils;
import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.LazyOptional;

public class RobotChunk implements NBTSerializable{
	Set<BlockPos> networks = new HashSet<>();
    public LazyOptional<LogisticNetwork> getNetworkFor(Level world,BlockPos actual) {
    	var it=networks.iterator();
    	while(it.hasNext()) {
    		BlockPos pos=it.next();
    		BlockEntity core = CUtils.getExistingTileEntity(world, pos);
    		//System.out.println(core);
    		if(core!=null)
    			return FHCapabilities.LOGISTIC.getCapability(core);
    		else
    			it.remove();
    	}
    	return LazyOptional.empty();
    };
    
    public RobotChunk(List<BlockPos> networks) {
		super();
		this.networks.addAll(networks);
	}

	public void register(BlockPos pos) {
    	networks.add(pos);
    }
    public void release(BlockPos pos) {
    	networks.remove(pos);
    }
    
	@Override
	public void save(CompoundTag nbt, boolean isPacket) {
		nbt.put("networks", CodecUtil.toNBTList(networks,BlockPos.CODEC));
		
	}
	@Override
	public void load(CompoundTag nbt, boolean isPacket) {
		networks.addAll(CodecUtil.fromNBTList(nbt.getList("networks", Tag.TAG_COMPOUND), BlockPos.CODEC));
	}

	public RobotChunk() {
		super();
	}
}
