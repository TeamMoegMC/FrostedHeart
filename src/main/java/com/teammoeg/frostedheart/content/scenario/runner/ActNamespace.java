package com.teammoeg.frostedheart.content.scenario.runner;

import java.util.List;
import java.util.Objects;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.FriendlyByteBuf;

public record ActNamespace(String chapter,String act) {
	public static final Codec<ActNamespace> CODEC=RecordCodecBuilder.create(t->t.group(
		Codec.STRING.fieldOf("chapter").forGetter(o->o.chapter),
		Codec.STRING.fieldOf("act").forGetter(o->o.act)).apply(t, ActNamespace::new));
	public static final Codec<List<ActNamespace>> LIST_CODEC=Codec.list(CODEC);
    public ActNamespace() {
		this("","");
	}
    
	@Override
	public int hashCode() {
		return Objects.hash(act, chapter);
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		ActNamespace other = (ActNamespace) obj;
		return Objects.equals(act, other.act) && Objects.equals(chapter, other.chapter);
	}
	public boolean isAct() {
		return act!=null&&chapter!=null;
	}
}
