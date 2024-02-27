package com.teammoeg.frostedheart.util.io;

import java.io.ObjectStreamClass;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
public class ClassInfo implements Marshaller{
	static final Map<Class<?>,ClassInfo> clss=new HashMap<>();
	ClassInfo superClass;
	List<FieldInfo> infos=new ArrayList<>();
	Class<?> cls;
	Supplier<Object> factory;
	static Method nis;
	static {
		try {
			nis=ObjectStreamClass.class.getDeclaredMethod("newInstance");
			nis.setAccessible(true);
		} catch (NoSuchMethodException | SecurityException e) {
			nis=null;
			
			e.printStackTrace();
			System.out.println("Cannot make class with reflect, would use empty constructor");
		}
	}
	public static ClassInfo valueOf(Class<?> cls) {
		if(cls==Object.class)return null;
		return clss.computeIfAbsent(cls, ClassInfo::new);
		
	}
	
	private ClassInfo(Class<?> cls) {
		super();
		this.cls = cls;
		ObjectStreamClass osc=ObjectStreamClass.lookupAny(cls);
		if(nis!=null) {
			factory=()->{
				try {
					return nis.invoke(osc);
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					e.printStackTrace();
				}
				return null;
			};
		}else {
			try {
				Constructor<?> ctor=cls.getConstructor();
				ctor.setAccessible(true);
				factory=()->{
					try {
						return ctor.newInstance();
					} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
							| InvocationTargetException e) {
						e.printStackTrace();
					}
					return null;
				};
			} catch (NoSuchMethodException | SecurityException e) {
				e.printStackTrace();
				throw new RuntimeException("Can not serialize a class without empty constructor");
			}
		}
		superClass=valueOf(cls.getSuperclass());
		for(Field f:cls.getDeclaredFields()) {
			if(!Modifier.isTransient(f.getModifiers())&&!Modifier.isStatic(f.getModifiers())) {
				f.setAccessible(true);
				infos.add(new FieldInfo(f));
			}
		}
	}
	@Override
	public INBT toNBT(Object o) {
		CompoundNBT cnbt=new CompoundNBT();
		for(FieldInfo fi:infos) {
			fi.save(cnbt, o);
		}
		return cnbt;
	}
	@Override
	public Object fromNBT(INBT nbt) {

		if(nbt instanceof CompoundNBT) {
			CompoundNBT cnbt=(CompoundNBT) nbt;
			Object o=factory.get();
			if(o!=null) {
				loadNBT(o,cnbt);
			}
			
		}

		return null;
	}
	public void loadNBT(Object o,CompoundNBT nbt) {
		if(superClass!=null)
			superClass.loadNBT(o, nbt);
		for(FieldInfo fi:infos) {
			fi.load(nbt, o);
		}

	}
}
