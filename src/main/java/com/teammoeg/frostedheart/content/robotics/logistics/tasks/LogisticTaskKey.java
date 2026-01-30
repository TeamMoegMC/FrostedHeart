package com.teammoeg.frostedheart.content.robotics.logistics.tasks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;

public record LogisticTaskKey(BlockPos pos,int num) {
	public final static Codec<LogisticTaskKey> CODEC=RecordCodecBuilder.create(t->t.group(
		BlockPos.CODEC.fieldOf("pos").forGetter(o->o.pos()),
		Codec.INT.fieldOf("slot").forGetter(o->o.num())
		).apply(t, LogisticTaskKey::new));
 
}
