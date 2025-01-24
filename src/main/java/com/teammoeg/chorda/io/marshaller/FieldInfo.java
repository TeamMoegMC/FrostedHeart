package com.teammoeg.chorda.io.marshaller;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import org.spongepowered.asm.mixin.injection.struct.InjectorGroupInfo.Map;

import com.mojang.serialization.Codec;
import com.teammoeg.chorda.io.SerializeName;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

public class FieldInfo {
	public Field field;
	public String name;
	public Codec<?> codecCache;
	public FieldInfo(Field f) {
		field=f;
		SerializeName anno=f.getAnnotation(SerializeName.class);
		if(anno!=null)
			name=anno.value();
		else
			name=f.getName();
	}
	public Codec<?> getCodec(){
		if(codecCache==null)
			codecCache=createCodec();
		return codecCache;
	}
	private Codec<?> createCodec(){
		Type type= field.getGenericType();
		if(List.class.isAssignableFrom(field.getType())) {
			Type[] types=((ParameterizedType)type).getActualTypeArguments();
			return Codec.list(MarshallUtil.getOrCreateCodec((Class<?>)types[0]));
		}else if(Map.class.isAssignableFrom(field.getType())) {
			Type[] types=((ParameterizedType)type).getActualTypeArguments();
			return Codec.unboundedMap(MarshallUtil.getOrCreateCodec((Class<?>)types[0]), MarshallUtil.getOrCreateCodec((Class<?>)types[1]));
		}
		return MarshallUtil.getOrCreateCodec(field.getType());
	}
	public void load(CompoundTag data,Object o) {
		if(data.contains(name)) {
			Tag nbt=data.get(name);
			try {
				field.set(o,MarshallUtil.deserialize(field.getType(), nbt));
			} catch (IllegalArgumentException | IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	public void save(CompoundTag data,Object o) {
		try {
			Object f=field.get(o);
			Tag nbt=MarshallUtil.serialize(f);
			if(nbt!=null)
			data.put(name,nbt);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
