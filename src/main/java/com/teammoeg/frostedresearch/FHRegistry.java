/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedresearch;

import com.teammoeg.chorda.io.RegistryListedMap;
import com.teammoeg.chorda.util.struct.OptionalLazy;

import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Our own registry type to reduce network and storage cost.
 *
 * @param <T> the generic type of registry
 * @author khjxiaogu
 */
public class FHRegistry<T extends FHRegisteredItem> implements Iterable<T> {
    /* private static final class RegisteredSupplier<T extends FHRegisteredItem> implements Supplier<T> {
         private final String key;
         private final Function<String, T> getter;

         public RegisteredSupplier(String key, Function<String, T> getter) {
             this.key = key;
             this.getter = getter;
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
             return getter == other.getter;
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

     }*/
    private ArrayList<T> items = new ArrayList<>();//registered objects
    private Map<String, Integer> rnames = new HashMap<>();//registry mappings
    private final Function<String, Supplier<T>> cacheGen = (n) -> () -> getByName(n);
    /*public Codec<Supplier<T>> SUPPLIER_CODEC=new CompressDifferCodec<Supplier<T>>(Codec.STRING.xmap(this::get,o->((RegisteredSupplier<T>)o).key),
    	Codec.INT.xmap(this::get, o->this.getIntId(((RegisteredSupplier<T>)o).key)));*/
    private ArrayList<String> rnamesl = new ArrayList<>();//reverse mappings
    private Map<String, Supplier<T>> cache = new HashMap<>();//object cache

/*    public static <T extends FHRegisteredItem> String serializeSupplier(Supplier<T> s) {
        if (s instanceof RegisteredSupplier) {
            return ((RegisteredSupplier<T>) s).key;
        }
        T r = s.get();
        if (r != null)
            return r.getId();
        return "";
    }
*/
/*    public void writeSupplier(FriendlyByteBuf pb, Supplier<T> s) {
    	 if (s instanceof RegisteredSupplier rs) {
    		 pb.writeVarInt(getIntId(rs.key));
             return;
         }
        if (s != null) {
            T t = s.get();
            if (t != null) {
                pb.writeVarInt(getIntId(t));
                return;
            }
        }
        pb.writeVarInt(-1);
    }
*/

    /**
     * Instantiates a new FHRegistry.<br>
     */
    public FHRegistry() {

    }

    /**
     * Get all non-null items.<br>
     * Remove all null(missing) items before return, should not used to calculate item numeric id.
     *
     * @return returns all non-null items
     */
    public List<T> all() {
        List<T> r = new ArrayList<>(items);
        r.removeIf(Objects::isNull);
        return r;
    }

    public void clear() {
        rnames.clear();
        rnamesl.clear();
        items.clear();
        cache.clear();
    }

    /**
     * Deserialize.
     *
     * @param load the load<br>
     */
    public void deserialize(ListTag load) {
        rnames.clear();
        rnamesl.clear();
        ArrayList<T> temp = new ArrayList<>(items);
        temp.removeIf(Objects::isNull);

        load.stream().map(Tag::getAsString).forEach(e -> rnamesl.add(e));
        for (int i = 0; i < rnamesl.size(); i++) {
            rnames.put(rnamesl.get(i), i);
        }
        items.clear();
        if (!temp.isEmpty()) {//reset registries
            ensure();
            for (T t : temp) {
                register(t);
            }
        }


    }

    /**
     * Ensure Capacity.
     */
    public void ensure() {
        items.ensureCapacity(rnamesl.size());
        while (items.size() < rnamesl.size())
            items.add(null);
    }

    public <E> List<E> toList(Map<String,E> data){
    	return new RegistryListedMap<String,E>(data,rnamesl.size()){

			@Override
			public String getKey(int id) {
				return FHRegistry.this.getStrId(id);
			}
    		
    	};
    }
    public <E> void fromList(List<E> data,BiConsumer<String,E> map){
    	int id=0;
    	for(E elm:data) {
    		final int curid=id;
    		if(elm!=null)
    			map.accept(getStrId(curid),elm);
    		id++;
    	}
    }
    @Override
	public String toString() {
		return "FHRegistry [items=" + items + "]";
	}

	/**
     * Get by numeric id.
     *
     * @param id the id<br>
     * @return item<br>
     */
    public T get(int id) {
        if (id < 0)
            return null;
        if (items.size() > id) {
            return items.get(id);
        }
        throw new IllegalArgumentException("Registry Id " + id + " does not exist!");
    }

    /**
     * Get a Supplier with buffer for item by name.<br>
     *
     * @param id the id<br>
     * @return returns Supplier of item
     */
    public T get(String id) {
        return get(getIntId(id));
    }

    public int getIntId(String obj) {
        return rnames.getOrDefault(obj, -1);
    }
    public String getStrId(int id) {
    	if(id<0||id>=rnamesl.size())
    		return null;
        return rnamesl.get(id);
    }
    public void replace(T research) {
        cache.remove(research.getId());
        register(research);
    }

    public int getIntId(T obj) {
        return getIntId(obj.getId());
    }

    /**
     * Get by numeric id.
     *
     * @param id the id<br>
     * @return by id<br>
     */
    /*public T getById(int id) {
        if (id < 0 || id >= items.size()) return null;
        return items.get(id);
    }*/

    /**
     * Get by name.
     *
     * @param lid the lid<br>
     * @return by name<br>
     */
    public T getByName(String lid) {
        int index = getIntId(lid);
        if (index != -1)
            return items.get(index);
        return null;
    }

    /**
     * Get count of items, including missing.
     *
     * @return size<br>
     */
    public int getSize() {
        return rnamesl.size();
    }

    /**
     * Get a LazyOptional for item by name.<br>
     *
     * @param id the id<br>
     * @return returns LazyOptional for name
     */
    public Supplier<T> lazyGet(String id) {
        return cache.computeIfAbsent(id, cacheGen);
    }

    /**
     * Prepare to reload.
     */
    public void prepareReload() {
        items.clear();
        cache.clear();
    }

    /*public Supplier<T> readSupplier(FriendlyByteBuf pb) {
        return this.get(pb.readVarInt());
    }*/

    /**
     * Register a new item.
     *
     * @param item the item<br>
     */
    public void register(T item) {
        String lid = item.getId();
        int index = getIntId(lid);

        ensure();
        if (index == -1) {
            rnames.put(item.getId(), rnamesl.size());
            // System.out.println(lid+" registered index"+rnamesl.size()+"");
            rnamesl.add(item.getId());
            items.add(item);
        } else {
            //System.out.println(lid+" re-registered index"+index+"");
            items.set(index, item);
        }
    }

    public void remove(T item) {
        String lid = item.getId();
        int index = getIntId(lid);
        ensure();
        if (index != -1) {
            items.set(index, null);
        }
    }

    public void runIfPresent(int id, Consumer<T> in) {
        if (items.size() >= id) {
            T t = items.get(id - 1);
            if (t != null)
                in.accept(t);
            return;
        }
    }

    public void runIfPresent(String id, Consumer<T> in) {
        T t=lazyGet(id).get();
        if(t!=null)
        	in.accept(t);
    }

    /**
     * Serialize.<br>
     *
     * @return returns serialize
     */
    public ListTag serialize() {
        ListTag cn = new ListTag();
        rnamesl.stream().map(StringTag::valueOf).forEach(cn::add);
        return cn;
    }

  /*  public Supplier<T> toSupplier(String s) {
        if (s != null && !s.isEmpty())
            return get(s);
        return () -> null;
    }*/

    @Override
    public Iterator<T> iterator() {
        return items.stream().filter(Objects::nonNull).iterator();
    }
}
