package com.teammoeg.frostedheart.research;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.teammoeg.frostedheart.util.LazyOptional;

import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.network.PacketBuffer;

/**
 * Our own registry type to reduce network and storage cost.
 *
 * @author khjxiaogu
 * @param <T> the generic type of registry
 */
public class FHRegistry<T extends FHRegisteredItem> {
	private ArrayList<T> items=new ArrayList<>();//registered objects
	private Map<String,Integer> rnames=new HashMap<>();//registry mappings
	private ArrayList<String> rnamesl=new ArrayList<>();//reverse mappings
	private Map<String,LazyOptional<T>> cache=new HashMap<>();//object cache
	private final Function<String,LazyOptional<T>> cacheGen=(n)->LazyOptional.of(()->getByName(n));
	private static final class RegisteredSupplier<T extends FHRegisteredItem> implements Supplier<T>{
		private final String key;
		private final Function<String,T> getter;
		public RegisteredSupplier(String key, Function<String, T> getter) {
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
		@SuppressWarnings("rawtypes")
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
			//System.out.println("trying to register "+item.getLId());
			String lid=item.getLId();
			int index=rnames.getOrDefault(lid,-1);
			ensure();
			if(index==-1) {
				//System.out.println("new entry");
				item.setRId(rnamesl.size()+1);
				
				rnames.put(item.getLId(),rnamesl.size());
				rnamesl.add(item.getLId());
				items.add(item);
				
			}else {
				//System.out.println("existed!");
				item.setRId(index+1);
				items.set(index,item);
			}
		}
	}
	public void remove(T item) {
		if(item.getRId()!=0) {
			String lid=item.getLId();
			int index=rnames.getOrDefault(lid,-1);
			ensure();
			if(index!=-1&&index+1==item.getRId()){
				items.set(index,null);
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
		int index=rnames.getOrDefault(lid,-1);
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
		if(id==0)
			return ()->null;
		if(rnamesl.size()>=id) {
			String name=rnamesl.get(id-1);
			if(name!=null)
				return get(name);
		}
		return ()->getById(id);
	}
	public void runIfPresent(String id,Consumer<T> in) {
		lazyGet(id).ifPresent(t->in.accept(t));
	}
	public void runIfPresent(int id,Consumer<T> in) {
		if(items.size()>=id) {
			T t=items.get(id-1);
			if(t!=null)
				in.accept(t);
			return;
		}
		return;
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
		items.ensureCapacity(rnamesl.size());
		while(items.size()<rnamesl.size())
			items.add(null);
	}
	
	/**
	 * Serialize.<br>
	 *
	 * @return returns serialize
	 */
	public ListNBT serialize() {
		ListNBT cn=new ListNBT();
		rnamesl.stream().map(StringNBT::valueOf).forEach(e->cn.add(e));
		return cn;
	}
	
	/**
	 * Deserialize.
	 *
	 * @param load the load<br>
	 */
	public void deserialize(ListNBT load) {
		rnames.clear();
		rnamesl.clear();
		ArrayList<T> temp=new ArrayList<>(items);
		temp.removeIf(Objects::isNull);
		load.stream().map(INBT::getString).forEach(e->rnamesl.add(e));
		for(int i=0;i<rnamesl.size();i++) {
			rnames.put(rnamesl.get(i),i);
		}
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
		return rnamesl.size();
	}
	public Supplier<T> toSupplier(String s){
		if(s!=null&&!s.isEmpty())
		return get(s);
		return ()->null;
	}
	public static<T extends FHRegisteredItem> String serializeSupplier(Supplier<T> s) {
		if(s instanceof RegisteredSupplier) {
			return ((RegisteredSupplier<T>)s).key;
		}
		T r=s.get();
		if(r!=null)
			return r.getLId();
		return "";
	}
	public static<T extends FHRegisteredItem> void writeSupplier(PacketBuffer pb,Supplier<T> s) {
		if(s!=null) {
			T t=s.get();
			if(t!=null) {
				pb.writeVarInt(t.getRId());
				return;
			}
		}
		pb.writeVarInt(0);
		return;
	}
	public Supplier<T> readSupplier(PacketBuffer pb) {
		return this.get(pb.readVarInt());
	}
}
