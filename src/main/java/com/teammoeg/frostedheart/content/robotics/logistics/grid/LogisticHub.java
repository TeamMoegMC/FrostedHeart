package com.teammoeg.frostedheart.content.robotics.logistics.grid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.teammoeg.frostedheart.content.robotics.logistics.data.ItemKey;

import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import lombok.Getter;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LazyOptional;

public class LogisticHub implements IGridElement {
	
	private static class ItemData implements ItemCountProvider{
		@Override
		public String toString() {
			return "ItemData [countmap=" + countmap + ", totalCount=" + totalCount + "]";
		}
		Reference2IntOpenHashMap<LazyOptional<IGridElement>> countmap=new Reference2IntOpenHashMap<>();
		@Getter
		int totalCount;
		public void addElement(LazyOptional<IGridElement> elm,int count) {
		
			int old=countmap.put(elm, count);
			totalCount+=count-old;
		}
		public void modifyElement(LazyOptional<IGridElement> elm,int count) {
			int cnt=countmap.getInt(elm);
			cnt+=count;
			if(cnt<=0)
				countmap.removeInt(elm);
			else
				countmap.put(elm, cnt);
			totalCount+=count;
		}
		public void removeElement(LazyOptional<IGridElement> elm) {
			int cnt=countmap.removeInt(elm);
			totalCount-=cnt;
		}
		public ItemStack takeItem(ItemKey ik,int amount) {
			var it=countmap.reference2IntEntrySet().iterator();
			int totake=amount;
			while(it.hasNext()) {
				var keys=it.next();
				if(keys.getKey().isPresent()) {
					IGridElement grid=keys.getKey().resolve().get();
					int taken=grid.takeItem(ik, totake).getCount();
					if(taken>0) {
						totake-=taken;
						ItemCountProvider ref=grid.getAllItems().get(ik);
						int ncnt;
						if(ref==null||(ncnt=ref.getTotalCount())<=0) {
							it.remove();
							continue;
						}
						int old=keys.getIntValue();
						keys.setValue(ncnt);
						this.totalCount+=ncnt-old;
						break;
					}
				}
				if(totake<=0)break;
			}
			return ik.createStackWithSize(amount-totake);
		}
		public ItemStack takeItem(LazyOptional<IGridElement> element,ItemKey ik,int amount) {
			if(element.isPresent()) {
				IGridElement grid=element.resolve().get();
				ItemStack taken=grid.takeItem(ik, amount);
				if(taken.getCount()>0) {
					ItemCountProvider ref=grid.getAllItems().get(ik);
					int ncnt;
					if(ref==null||(ncnt=ref.getTotalCount())<=0) {
						countmap.removeInt(element);
						return taken;
					}
					int old=countmap.put(element, ncnt);
					this.totalCount+=ncnt-old;
					return taken;
				}
			}
			return ItemStack.EMPTY;
		}
		public GridAndAmount findLogisticGridForTake(ItemKey ik) {
			var it=countmap.reference2IntEntrySet().iterator();
			while(it.hasNext()) {
				var keys=it.next();
				if(keys.getKey().isPresent()) {
					return new GridAndAmount(keys.getKey(),keys.getIntValue());
					
				}
			}
			return null;
		}
		public GridAndAmount findLogisticGridForPlace(ItemKey ik,boolean fillEmpty) {
			for(var keys:countmap.reference2IntEntrySet()) {
				if(keys.getKey().isPresent()) {
					IGridElement grid=keys.getKey().resolve().get();
					if(!grid.fillable()) {
						continue;
					}
					//if(grid.getEmptySlotCount()>0) return new GridAndAmount(keys.getKey(),ik.getMaxStackSize());
					int maxsize=ik.item.getMaxStackSize(ik.getStack());
					int reminder=keys.getIntValue()%maxsize;
					if(reminder!=0)return new GridAndAmount(keys.getKey(),maxsize-reminder);
				}
			}
			if(fillEmpty) {
				for(it.unimi.dsi.fastutil.objects.Reference2IntMap.Entry<LazyOptional<IGridElement>> keys:countmap.reference2IntEntrySet()) {
					if(keys.getKey().isPresent()) {
						IGridElement grid=keys.getKey().resolve().get();
						if(!grid.fillable()) {
							continue;
						}
						if(grid.getEmptySlotCount()>0)
							return new GridAndAmount(keys.getKey(),ik.getMaxStackSize());
					}
				}
			}
			return null;
		}
		public ItemStack pushItem(LazyOptional<IGridElement> element,ItemKey ik,ItemStack remain) {
			if(element.isPresent()) {
				IGridElement grid=element.resolve().get();
				if(!grid.fillable()) {
					return remain;
				}
				int oldCount=remain.getCount();
				remain=grid.pushItem(ik,remain, false);
				int newCount=remain.getCount();
				if(oldCount!=newCount) {
					ItemCountProvider ref=grid.getAllItems().get(ik);
					int ncnt=0;
					if(ref!=null) {
						ncnt=ref.getTotalCount();
					}
					int old=countmap.put(element, ncnt);
					this.totalCount+=ncnt-old;
				}
			}
			return remain;
		}
		public ItemStack pushItem(ItemKey ik,ItemStack is,boolean fillEmpty) {
			ItemStack remain=is;
			for(var keys:countmap.reference2IntEntrySet()) {
				if(keys.getKey().isPresent()) {
					IGridElement grid=keys.getKey().resolve().get();
					if(!grid.fillable()) {
						continue;
					}
					int oldCount=remain.getCount();
					remain=grid.pushItem(ik,remain, false);
					int newCount=remain.getCount();
					if(oldCount!=newCount) {
						ItemCountProvider ref=grid.getAllItems().get(ik);
						int ncnt=0;
						if(ref!=null) {
							ncnt=ref.getTotalCount();
						}
						int old=keys.getIntValue();
						keys.setValue(ncnt);
						this.totalCount+=ncnt-old;
						break;
					}
					if(remain.isEmpty())
						return ItemStack.EMPTY;
				}
			}
			if(fillEmpty) {
				for(it.unimi.dsi.fastutil.objects.Reference2IntMap.Entry<LazyOptional<IGridElement>> keys:countmap.reference2IntEntrySet()) {
					if(keys.getKey().isPresent()) {
						IGridElement grid=keys.getKey().resolve().get();
						if(!grid.fillable()) {
							continue;
						}
						int oldCount=remain.getCount();
						remain=grid.pushItem(ik,remain, true);
						int newCount=remain.getCount();
						if(oldCount!=newCount) {
							ItemCountProvider ref=grid.getAllItems().get(ik);
							int ncnt=0;
							if(ref!=null) {
								ncnt=ref.getTotalCount();
							}
							int old=keys.getIntValue();
							keys.setValue(ncnt);
							this.totalCount+=ncnt-old;
							break;
						}
						if(remain.isEmpty())
							return ItemStack.EMPTY;
					}
				}
			}
			return remain;
		}
	}
	private static class GridStat{
		Set<ItemKey> items;
		int emptySlots;
	}
	boolean isChanged;
	int emptySlotCount;
	Map<ItemKey,ItemData> cachedData=new HashMap<>();
	Map<LazyOptional<IGridElement>,GridStat> gridRef=new IdentityHashMap<>();
	@Getter
	Level level;
	@Getter
	BlockPos pos;
	public LogisticHub(Level level, BlockPos pos) {
		super();
		this.level = level;
		this.pos = pos;
	}
	public void addElement(LazyOptional<IGridElement> cap) {
		if(cap.isPresent()) {
			IGridElement gridelm=cap.resolve().get();
			Map<ItemKey, ? extends ItemCountProvider> allitem=gridelm.getAllItems();
			GridStat set=gridRef.get(cap);
			Set<ItemKey> newSet=new HashSet<>(allitem.keySet());
			if(set!=null) {
				set.items.removeAll(newSet);
				for(ItemKey ik:set.items) {
					ItemData id=cachedData.get(ik);
					if(id!=null) {
						id.removeElement(cap);
					}
				}
				this.emptySlotCount-=set.emptySlots;
				set.items=newSet;
				set.emptySlots=gridelm.getEmptySlotCount();
				this.emptySlotCount+=set.emptySlots;
			}else {
				set=new GridStat();
				set.items=newSet;
				set.emptySlots=gridelm.getEmptySlotCount();
				this.emptySlotCount+=set.emptySlots;
				gridRef.put(cap, set);
			}
			for(Entry<ItemKey, ? extends ItemCountProvider> ik:allitem.entrySet()) {
				ItemData id=cachedData.get(ik.getKey());
				if(id==null) {
					id=new ItemData();
					cachedData.put(ik.getKey(), id);
				}
				id.addElement(cap, ik.getValue().getTotalCount());
			}
		}else {
			removeElement(cap);
		}
	}
	public void removeElement(LazyOptional<IGridElement> cap) {
		GridStat set=gridRef.remove(cap);
		if(set!=null) {
			for(ItemKey ik:set.items) {
				ItemData id=cachedData.get(ik);
				if(id!=null) {
					id.removeElement(cap);
				}
			}
			this.emptySlotCount-=set.emptySlots;
		}
		
	}
	@Override
	public int getEmptySlotCount() {
		return emptySlotCount;
	}
	public void revalidate() {
		
		this.cachedData.clear();
		List<LazyOptional<IGridElement>> caps=new ArrayList<>(this.gridRef.keySet());
		this.gridRef.clear();
		this.emptySlotCount=0;
		for(LazyOptional<IGridElement> i:caps) {
			if(i.isPresent()) {
				this.addElement(i);
			}
		}
	}
	@Override
	public ItemStack pushItem(ItemKey ik, ItemStack is,boolean fillEmpty) {
		ItemData id=cachedData.get(ik);
		
		ItemStack remain=is;
		if(id!=null) {
			remain=id.pushItem(ik, remain,fillEmpty);
		}
		if(remain.isEmpty()) {
			return ItemStack.EMPTY;
		}
		if(fillEmpty) {
			for(Entry<LazyOptional<IGridElement>, GridStat> entry:gridRef.entrySet()) {
				if(entry.getValue().emptySlots>0) {
					int oldcount=remain.getCount();
					remain=entry.getKey().resolve().get().pushItem(ik, remain, true);
					if(oldcount!=remain.getCount()) {
						this.addElement(entry.getKey());
					}
					if(remain.isEmpty())
						return ItemStack.EMPTY;
				}
				
			}
		}
		
		return remain;
	}

	@Override
	public ItemStack takeItem(ItemKey key, int amount) {
		ItemData id=cachedData.get(key);
		if(id!=null) {
			return id.takeItem(key, amount);
		}
		return ItemStack.EMPTY;
	}
	public ItemStack pushItem(LazyOptional<IGridElement> grid,ItemKey ik, ItemStack is) {
		ItemData id=cachedData.get(ik);
		ItemStack remain=is;
		if(id!=null) {
			remain=id.pushItem(grid,ik, remain);
		}
		if(remain.isEmpty()) {
			return ItemStack.EMPTY;
		}
		return remain;
	}
	public ItemStack takeItem(LazyOptional<IGridElement> grid,ItemKey key, int amount) {
		ItemData id=cachedData.get(key);
		if(id!=null) {
			return id.takeItem(grid,key, amount);
		}
		return ItemStack.EMPTY;
	}

	public GridAndAmount findGridForPlace(ItemKey ik, ItemStack is) {
		ItemData id=cachedData.get(ik);
		
		if(id!=null) {
			GridAndAmount remain=id.findLogisticGridForPlace(ik, true);
			if(remain!=null) {
				return remain;
			}
		}
		
		
		for(Entry<LazyOptional<IGridElement>, GridStat> entry:gridRef.entrySet()) {
			if(entry.getValue().emptySlots>0) {
				return new GridAndAmount(entry.getKey(),ik.getMaxStackSize());
			}
			
		}
		
		return null;
	}

	public GridAndAmount findGridForTake(ItemKey key) {
		ItemData id=cachedData.get(key);
		if(id!=null) {
			return id.findLogisticGridForTake(key);
		}
		return null;
	}

	@Override
	public Map<ItemKey, ? extends ItemCountProvider> getAllItems() {
		return cachedData;
	}
	@Override
	public String toString() {
		return "LogisticHub [emptySlotCount=" + emptySlotCount + ", cachedData=" + cachedData + ", gridRef=" + gridRef + "]";
	}
	@Override
	public boolean isChanged() {
		
		return false;
	}
	@Override
	public void tick() {
		List<LazyOptional<IGridElement>> sets=new ArrayList<>(gridRef.keySet());
		for(LazyOptional<IGridElement> cap:sets) {
			if(!cap.isPresent())
				this.removeElement(cap);
			else if(cap.resolve().get().consumeChange()) {
				this.addElement(cap);
				isChanged=true;
			}
		}
		
	}
	@Override
	public boolean consumeChange() {
		return false;
	}
	public static void main(String[] args) {
	      SharedConstants.tryDetectVersion();
	      SharedConstants.enableDataFixerOptimizations();
		Bootstrap.bootStrap();
		LogisticHub hub=new LogisticHub(null,null);
		LogisticChest chestA=new LogisticChest(null,null);
		LazyOptional<LogisticChest> cacap=LazyOptional.of(()->chestA);
		LogisticChest chestB=new LogisticChest(null,null);
		LazyOptional<LogisticChest> cbcap=LazyOptional.of(()->chestB);
		LogisticChest chestC=new LogisticChest(null,null);
		LazyOptional<LogisticChest> cccap=LazyOptional.of(()->chestC);
		hub.addElement(cacap.cast());
		hub.addElement(cbcap.cast());
		hub.addElement(cccap.cast());
		List<IGridElement> ige=new ArrayList<>(Arrays.asList(hub,chestA,chestB,chestC));
		List<IGridElement> igeo=new ArrayList<>(Arrays.asList(hub,chestA,chestB,chestC));
		Collections.shuffle(ige);
		
		chestA.insertItem(0, new ItemStack(Items.STONE,10), false);
		chestB.insertItem(0, new ItemStack(Items.BIRCH_PLANKS,30), false);
		chestB.insertItem(3, new ItemStack(Items.BIRCH_PLANKS,30), false);
		chestC.insertItem(0, new ItemStack(Items.BIRCH_PLANKS,15), false);
		for(IGridElement grid:ige) {
			grid.tick();
		}
		cccap.invalidate();
		for(IGridElement grid:igeo)
			System.out.println(grid);
		System.out.println(hub.takeItem(new ItemStack(Items.BIRCH_PLANKS,70)));
		for(IGridElement grid:ige) {
			grid.tick();
		}
		for(IGridElement grid:igeo)
			System.out.println(grid);
	}
	@Override
	public boolean fillable() {
		return true;
	}


}
