package com.teammoeg.frostedheart.content.robotics.logistics.data;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;

import com.teammoeg.frostedheart.content.robotics.logistics.Filter;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class Index {

    public static record Coord(BlockPos pos,int slot) implements Comparable<Coord> {
        @Override
        public int compareTo(Coord o) {
        	int c=pos.compareTo(o.pos);
        	if(c!=0)
        		return c;
            return Integer.compare(slot, o.slot);
        }

    }

    /**
     * 内容 (a, b)
     */
    public static class Content{
        public final Item item;
        private ItemStack itemStack;

        public ItemStack getStack() {
			return itemStack;
		}
		public Content(Item item, ItemStack itemStack) {
            this.item = item;
            this.itemStack = itemStack.copy();
        }
		public Content(ItemStack itemStack) {
            this.item = itemStack.getItem();
            this.itemStack = itemStack.copy();
        }
        public void setStack(ItemStack stack) {
        	this.itemStack=stack.copy();
        }
    }

    public static class GridData{
        public final Set<Coord> item=new HashSet<>();
        public int priority;
		public GridData(int priority) {
			super();
			this.priority = priority;
		}
		public void add(Coord coord) {
			item.add(coord);
		}

    }

    // 坐标 -> 内容
    private final Map<Coord, Content> primary = new HashMap<>();

    // a -> 坐标集合
    private final Map<Item, Set<Coord>> byItem = new HashMap<>();
    

    private final Map<BlockPos, GridData> byPos = new HashMap<>();
    public void clear() {
    	primary.clear();
    	byItem.clear();
    	byPos.clear();
    }
    /**
     * 增加或修改坐标内容。
     * 如果坐标已存在，则覆盖旧内容。
     */
    public Content put(Coord coord, Content newContent) {
        Content old = primary.put(coord, newContent);
        byPos.computeIfAbsent(coord.pos(), k -> new GridData(0)).add(coord);
        if (old == null) {
            byItem.computeIfAbsent(newContent.item, k -> new HashSet<>()).add(coord);
        } else if (old.item!=newContent.item) {
            removeFromByItem(old.item, coord);
            byItem.computeIfAbsent(newContent.item, k -> new HashSet<>()).add(coord);
        }

        return old;
    }
    public void setPriority(BlockPos pos, int priority) {
    	GridData data=byPos.get(pos);
    	if(data!=null) {
    		data.priority=priority;
    	}
    }
    /**
     * 根据坐标删除。
     */
    public Content remove(Coord coord) {
        Content old = primary.remove(coord);
        if (old != null) {
            removeFromByItem(old.item, coord);
        }
        return old;
    }
    public void remove(BlockPos pos) {
    	GridData coords=byPos.remove(pos);
    	if(coords!=null) {
    		for(Coord coord:coords.item) {
    			remove(coord);
    		}
    	}
    }
    /**
     * 根据坐标获取内容。
     */
    public Content get(Coord coord) {
        return primary.get(coord);
    }

    private void removeFromByItem(Item a, Coord coord) {
        Set<Coord> set = byItem.get(a);
        if (set != null) {
            set.remove(coord);
            if (set.isEmpty()) {
                byItem.remove(a);
            }
        }
    }

    /**
     * 根据 Item 查找所有坐标，返回结果按 Coord 自然顺序排序。
     */
    public List<Coord> findByItem(Item a,@Nullable Comparator<Coord> comparator) {
        Set<Coord> coords = byItem.get(a);
        if (coords == null || coords.isEmpty()) {
            return Collections.emptyList();
        }

        List<Coord> result = new ArrayList<>();
        for (Coord coord : coords) {
            Content content = primary.get(coord);
            if (content != null && Objects.equals(content.item, a)) {
                insertSorted(result, coord, comparator);
            }
        }
        return result;
    }

    /**
     * 根据 ItemKey 查找坐标列表。
     * 返回的坐标列表在插入过程中保持按 Coord 自然顺序排序。
     */
    public List<Coord> findByItemKey(
    	ItemKey key,@Nullable BlockPos from,@Nullable Comparator<Coord> comparator) {
        Set<Coord> coords = byItem.get(key.item);
        if (coords == null || coords.isEmpty()) {
            return Collections.emptyList();
        }
        List<Coord> result = new ArrayList<>();
        for (Coord coord : coords) {
        	if(from==null||!coord.pos().equals(from)) {
	            Content content = primary.get(coord);
	            if (content != null && key.isSameItem(content.itemStack)) {
	                insertSorted(result, coord, comparator);
	            }
        	}
        }
        return result;
    }

    /**
     * 根据 ItemKey 查找坐标列表。
     * 返回的坐标列表在插入过程中保持按 Coord 自然顺序排序。
     */
    public List<Coord> findByFilter(
    	Filter key,@Nullable BlockPos from,@Nullable Comparator<Coord> comparator) {
        Set<Coord> coords = byItem.get(key.getItem());
        if (coords == null || coords.isEmpty()) {
            return Collections.emptyList();
        }
        List<Coord> result = new ArrayList<>();
        for (Coord coord : coords) {
        	if(from==null||!coord.pos().equals(from)) {
	            Content content = primary.get(coord);
	            if (content != null && key.matches(content.getStack())) {
	                insertSorted(result, coord, comparator);
	            }
        	}
        }
        return result;
    }
    private void insertSorted(List<Coord> list, Coord coord,Comparator<Coord> comp) {
        int idx;
        if(comp==null)
        	idx = Collections.binarySearch(list, coord);
        else
        	idx = Collections.binarySearch(list, coord, comp);
        if (idx < 0) {
            list.add(-idx - 1, coord);
        }
        // idx >= 0 表示已存在，不重复插入
    }
    /**
     * 可选：查看当前所有坐标和内容快照。
     */
    public Map<Coord, Content> snapshot() {
        return new HashMap<>(primary);
    }

}