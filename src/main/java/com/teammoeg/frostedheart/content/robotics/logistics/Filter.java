package com.teammoeg.frostedheart.content.robotics.logistics;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.content.robotics.logistics.data.ItemKey;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.item.ItemStack;

public class Filter {
	public static final Codec<Filter> CODEC=RecordCodecBuilder.create(t->t.group(ItemKey.CODEC.fieldOf("key").forGetter(o->o.key),
		Codec.BOOL.optionalFieldOf("ignoreNBT",false).forGetter(o->o.ignoreNbt),
		Codec.INT.fieldOf("size").forGetter(o->o.size)).apply(t, Filter::new));
	@Getter
	@Setter
	ItemKey key;
	@Getter
	@Setter
	boolean ignoreNbt;
	@Setter
	@Getter
	int size;
	
	public Filter(ItemKey key, boolean ignoreNbt,int size) {
		super();
		this.key = key;
		this.ignoreNbt = ignoreNbt;
		this.size=size;
	}
	public boolean matches(ItemKey okey) {
		if(ignoreNbt)
			return okey.item==key.item;
		return key.equals(okey);
	}
	public boolean matches(ItemStack okey) {
		if(ignoreNbt)
			return okey.getItem()==key.item;
		return key.isSameItem(okey);
	}
}
