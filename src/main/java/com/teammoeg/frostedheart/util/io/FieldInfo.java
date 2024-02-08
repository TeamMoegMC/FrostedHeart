package com.teammoeg.frostedheart.util.io;

import java.lang.reflect.Field;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;

public class FieldInfo {
	Field field;
	String name;
	public FieldInfo(Field f) {
		field=f;
		SerializeName anno=f.getAnnotation(SerializeName.class);
		if(anno!=null)
			name=anno.value();
		else
			name=f.getName();
	}
	public void load(CompoundNBT data,Object o) {
		if(data.contains(name)) {
			INBT nbt=data.get(name);
			try {
				field.set(o,SerializeUtil.deserialize(field.getType(), nbt));
			} catch (IllegalArgumentException | IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	public void save(CompoundNBT data,Object o) {
		try {
			Object f=field.get(o);
			INBT nbt=SerializeUtil.serialize(f);
			if(nbt!=null)
			data.put(name,nbt);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
