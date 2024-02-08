package com.teammoeg.frostedheart.util.io;

import java.util.function.Function;

import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;

public class ListMarshaller<T> implements Marshaller {
	private final Class<T> elmType;
	private final Function<Object,IList<T>> wrapper;
	private final Function<Integer,IList<T>> factory;


	public ListMarshaller(Class<T> elmType, Function<Object, IList<T>> wrapper, Function<Integer, IList<T>> factory) {
		super();
		this.elmType = elmType;
		this.wrapper = wrapper;
		this.factory = factory;
	}

	@Override
	public INBT toNBT(Object o) {
		ListNBT nbt=new ListNBT();
		IList<T> list=wrapper.apply(o);
		for(int i=0;i<list.size();i++) {
			nbt.add(SerializeUtil.serialize(list.get(i)));
		}
		return nbt;
	}

	@Override
	public Object fromNBT(INBT nbt) {
		
		if(nbt instanceof ListNBT) {
			IList<T> list=factory.apply(((ListNBT)nbt).size());
			for(INBT n:(ListNBT)nbt) {
				list.add(SerializeUtil.deserialize(elmType, n));
			}
			return list.getInstance();
		}
		return null;
	}

}
