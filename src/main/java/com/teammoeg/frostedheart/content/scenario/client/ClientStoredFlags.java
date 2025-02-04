package com.teammoeg.frostedheart.content.scenario.client;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.dataholders.SpecialData;
import com.teammoeg.chorda.dataholders.SpecialDataHolder;
import com.teammoeg.chorda.io.CodecUtil;

import lombok.Getter;
import net.minecraft.nbt.CompoundTag;

public class ClientStoredFlags implements SpecialData{
	public static final Codec<ClientStoredFlags> CODEC=RecordCodecBuilder.create(t->
	t.group(CompoundTag.CODEC.fieldOf("flags").forGetter(o->o.data))
			.apply(t, ClientStoredFlags::new));
	@Getter
	CompoundTag data;

	public ClientStoredFlags(CompoundTag data) {
		super();
		this.data = data;
	}
	public ClientStoredFlags(SpecialDataHolder holder) {
		super();
		this.data = new CompoundTag();
	}
}
