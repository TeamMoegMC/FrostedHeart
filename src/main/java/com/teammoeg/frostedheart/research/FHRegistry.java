package com.teammoeg.frostedheart.research;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraftforge.common.util.LazyOptional;

/**
 * Class FHRegistry.
 *
 * @author khjxiaogu
 * @param <T> the generic type of registry
 */
public abstract class FHRegistry<T extends FHRegisteredItem> {
	private ArrayList<T> items=new ArrayList<>();//registered objects
	private List<String> rnames=new ArrayList<>();//registry mappings
	private Map<String,LazyOptional<T>> cache=new HashMap<>();//object cache
	private final Function<String,LazyOptional<T>> cacheGen=(n)->LazyOptional.of(()->getByName(n));
	
	/**
	 * Instantiates a new FHRegistry.<br>
	 */
	public FHRegistry() {
		
	}
	
	/**
	 * Register a new item.
	 *
	 * @param item the item<br>
	 */
	public void register(T item) {
		if(item.getRId()==0) {
			String lid=item.getLId();
			int index=rnames.indexOf(lid);
			ensure();
			if(index==-1) {
				item.setRId(rnames.size()+1);
				items.add(item);
				rnames.add(item.getLId());
			}else {
				items.set(index,item);
			}
		}
	}
	
	/**
	 * Prepare to reload.
	 */
	public void prepareReload() {
		items.clear();
		cache.clear();
	}
	
	/**
	 * Get by numeric id.
	 *
	 * @param id the id<br>
	 * @return by id<br>
	 */
	public T getById(int id) {
		return items.get(id-1);
	}
	
	/**
	 * Get by name.
	 *
	 * @param lid the lid<br>
	 * @return by name<br>
	 */
	public T getByName(String lid){
		int index=rnames.indexOf(lid);
		if(index!=-1)
			return items.get(index);
		return null;
	}
	
	/**
	 * Get a LazyOptional for item by name.<br>
	 *
	 * @param id the id<br>
	 * @return returns LazyOptional for name
	 */
	public LazyOptional<T> lazyGet(String id) {
		return cache.computeIfAbsent(id,cacheGen);
	}
	
	/**
	 * Get a Supplier with buffer for item by name.<br>
	 *
	 * @param id the id<br>
	 * @return returns Supplier of item
	 */
	public Supplier<T> get(String id) {
		return ()->lazyGet(id).orElse(null);
	}
	public Supplier<T> get(int id) {
		if(rnames.size()>id) {
			String name=rnames.get(id-1);
			if(name!=null)
				return ()->lazyGet(name).orElse(null);
		}
		return ()->items.get(id-1);
	}
	/**
	 * Get all non-null items.<br>
	 * Remove all null(missing) items before return, should not used to calculate item numeric id.
	 * @return returns all non-null items
	 */
	public List<T> all(){
		List<T> r=new ArrayList<>(items);
		r.removeIf(e->e==null);
		return r;
	}
	
	/**
	 * Ensure Capacity.
	 */
	public void ensure() {
		items.ensureCapacity(rnames.size());
		while(items.size()<rnames.size())
			items.add(null);
	}
	
	/**
	 * Serialize.<br>
	 *
	 * @return returns serialize
	 */
	public ListNBT serialize() {
		ListNBT cn=new ListNBT();
		rnames.stream().map(StringNBT::valueOf).forEach(e->cn.add(e));
		return cn;
	}
	
	/**
	 * Deserialize.
	 *
	 * @param load the load<br>
	 */
	public void deserialize(ListNBT load) {
		rnames.clear();
		load.stream().map(INBT::getString).forEach(e->rnames.add(e));
		if(!items.isEmpty()) {//reset registries
			ArrayList<T> temp=new ArrayList<>(items);
			items.clear();
			ensure();
			for(T t:temp)
				register(t);
		}
		
	}
	
	/**
	 * Get count of items, including missing.
	 *
	 * @return size<br>
	 */
	public int getSize() {
		return rnames.size();
	}
}
