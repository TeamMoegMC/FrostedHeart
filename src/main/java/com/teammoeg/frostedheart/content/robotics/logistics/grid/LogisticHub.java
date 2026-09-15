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

package com.teammoeg.frostedheart.content.robotics.logistics.grid;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.teammoeg.frostedheart.content.robotics.logistics.Filter;
import com.teammoeg.frostedheart.content.robotics.logistics.data.Index;
import com.teammoeg.frostedheart.content.robotics.logistics.data.Index.Content;
import com.teammoeg.frostedheart.content.robotics.logistics.data.Index.Coord;
import com.teammoeg.frostedheart.content.robotics.logistics.data.ItemKey;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LazyOptional;

public class LogisticHub {
	public void set(Coord coord, ItemStack stack) {
		cachedIndex.put(coord, new Content(stack));
	}
	public void set(Coord coord, ItemKey stack) {
		cachedIndex.put(coord, new Content(stack.item,ItemStack.EMPTY));
	}
	public Content remove(Coord coord) {
		return cachedIndex.remove(coord);
	}
	public void remove(BlockPos pos) {
		cachedIndex.remove(pos);
	}
	public static record GridData(BlockPos pos,LazyOptional<IGridElement> cap){
	}
	boolean valid=true;
	public boolean isValid() {
		return valid;
	}
	Index cachedIndex=new Index();
	Map<BlockPos,GridData> gridByPos=new HashMap<>();
	Map<LazyOptional<IGridElement>,GridData> gridByRef=new HashMap<>();
	@Getter
	Level level;
	@Getter
	BlockPos pos;
	public LogisticHub(Level level, BlockPos pos) {
		super();
		this.level = level;
		this.pos = pos;
	}
	public void addElement(LazyOptional<IGridElement> cap,boolean refresh) {
		if(!refresh&&gridByRef.containsKey(cap)) {
			return;
		}
		if(cap.isPresent()) {
			IGridElement gridelm=cap.orElse(null);
			gridByPos.put(gridelm.getPos(),new GridData(gridelm.getPos(),cap));
			gridelm.registerSlots(this);
		}else {
			removeElement(cap);
		}
	}
	public void removeElement(LazyOptional<IGridElement> cap) {
		GridData data;
		if(cap.isPresent()) {
			data=gridByPos.remove(cap.orElse(null).getPos());
		}else {
			data=gridByRef.remove(cap);
		}
		cachedIndex.remove(data.pos());
	}
	public void revalidate() {
		cachedIndex.clear();
		gridByRef.clear();
		gridByPos.values().removeIf(t->!t.cap().isPresent());
		
		for(GridData i:gridByPos.values()) {
			gridByRef.put(i.cap(), i);
			IGridElement gridelm=i.cap().orElse(null);
			if(gridelm!=null) {
				gridelm.revalidate();
				gridelm.registerSlots(this);
			}
		}
	}
	
	public void invalidate() {
		valid=false;
	}

	public GridAndAmount findGridForPlace(ItemKey key, ItemStack is,BlockPos source) {
		List<Coord> id=cachedIndex.findByItemKey(key, source, Comparator.comparingInt(t->source.distManhattan(t.pos())));
		
		if(!id.isEmpty()) {
			for(Coord coord:id) {
				Content content=cachedIndex.get(coord);
				if(content!=null&&content.getStack().getCount()<content.getStack().getMaxStackSize()) {
					return new GridAndAmount(coord,content.getStack().getMaxStackSize()-content.getStack().getCount());
				}
			}
		}
		return null;
	}

	public GridAndAmount findGridForTake(ItemKey key,BlockPos source) {
		List<Coord> id=cachedIndex.findByItemKey(key, source, Comparator.comparingInt(t->source.distManhattan(t.pos())));
		if(!id.isEmpty()) {
			for(Coord coord:id) {
				Content content=cachedIndex.get(coord);
				if(content!=null&&content.getStack().getCount()>0) {
					return new GridAndAmount(coord, content.getStack().getCount());
				}
			}
		}
		return null;
	}

	public GridAndAmount findGridForTake(Filter key,BlockPos source) {
		List<Coord> id=cachedIndex.findByFilter(key, source, Comparator.comparingInt(t->source.distManhattan(t.pos())));
		if(!id.isEmpty()) {
			for(Coord coord:id) {
				Content content=cachedIndex.get(coord);
				if(content!=null&&content.getStack().getCount()>0) {
					return new GridAndAmount(coord, content.getStack().getCount());
				}
			}
		}
		return null;
	}
	public LazyOptional<IGridElement> getByPos(BlockPos pos){

		GridData result=gridByPos.get(pos);
		if(result!=null) {
			if(result.cap().isPresent())
				return result.cap();
			else
				gridByPos.remove(pos);
		}
		return LazyOptional.empty();
	}

}
