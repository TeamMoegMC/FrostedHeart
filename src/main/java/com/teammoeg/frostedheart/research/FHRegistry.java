package com.teammoeg.frostedheart.research;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import com.teammoeg.frostedheart.util.LazyOptional;

import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;

/**
 * Class FHRegistry.
 *
 * @author khjxiaogu
 * @param <T> the generic type of registry
 */
public abstract class FHRegistry<T extends FHRegisteredItem> {
	private ArrayList<T> items=new ArrayList<>();//registered objects
	private ArrayList<String> rnames=new ArrayList<>();//registry mappings
	private Map<String,LazyOptional<T>> cache=new HashMap<>();//object cache
	private final Function<String,LazyOptional<T>> cacheGen=(n)->LazyOptional.of(()->getByName(n));
	private static final class RegisteredSupplier<T extends FHRegisteredItem,K> implements Supplier<T>{
		private final K key;
		private final Function<K,T> getter;
		public RegisteredSupplier(K key, Function<K, T> getter) {
			this.key = key;
			this.getter = getter;
		}
		@Override
		public T get() {
			return getter.apply(key);
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((key == null) ? 0 : key.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			RegisteredSupplier other = (RegisteredSupplier) obj;
			if (key == null) {
				if (other.key != null)
					return false;
			} else if (!key.equals(other.key))
				return false;
			return getter==other.getter;
		}
		
	}
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
				item.setRId(index+1);
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
	private Function<String,T> strLazyGetter=x->lazyGet(x).orElse(null);
	/**
	 * Get a Supplier with buffer for item by name.<br>
	 *
	 * @param id the id<br>
	 * @return returns Supplier of item
	 */
	public Supplier<T> get(String id) {
		return new RegisteredSupplier<>(id,strLazyGetter);
	}
	public Supplier<T> get(int id) {
		if(rnames.size()>=id) {
			String name=rnames.get(id-1);
			if(name!=null)
				return get(name);
		}
		throw new IllegalStateException("Cannot get data by id before initialize");
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
		ArrayList<T> temp=new ArrayList<>(items);
		load.stream().map(INBT::getString).forEach(e->rnames.add(e));
		if(!temp.isEmpty()) {//reset registries
			items.clear();
			ensure();
			for(T t:temp) {
				t.setRId(0);
				register(t);
			}
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
