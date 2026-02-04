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

package com.teammoeg.frostedheart.content.world.dimensionalseed;

import java.nio.charset.StandardCharsets;

import com.google.common.hash.Hashing;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.dataholders.SpecialData;
import com.teammoeg.chorda.dataholders.SpecialDataHolder;
import com.teammoeg.frostedheart.FHMain;
import lombok.Getter;
import net.minecraft.world.level.Level;

public class DimensionalSeed  implements SpecialData{
	public static final Codec<DimensionalSeed> CODEC=RecordCodecBuilder.create(t->
	t.group(Codec.LONG.optionalFieldOf("seed",0L).forGetter(o->o.seed),
			Codec.BOOL.optionalFieldOf("hasSeed",false).forGetter(o->o.hasSeparateSeed),
			Codec.BOOL.optionalFieldOf("isInit",false).forGetter(o->o.isInitialized))
			.apply(t, DimensionalSeed::new));
	@Getter
	private long seed;
	private boolean hasSeparateSeed;
	private boolean isInitialized;
	public DimensionalSeed(SpecialDataHolder t) {
	}
	public DimensionalSeed() {
	}
	public DimensionalSeed(long seed) {
		super();
		this.seed = seed;
	}
	public void generateSeed(Level l,long originseed) {
		seed=Hashing.sha256().newHasher().putLong(originseed).putString(l.dimension().location().toString(), StandardCharsets.UTF_8).hash().asLong();
		hasSeparateSeed=true;
		FHMain.LOGGER.info("generated seed for level "+l.dimension().location().toString()+":"+seed);
	}
	public void tryInit(Level l,long originseed) {
		if(!isInitialized) {
			isInitialized=true;
			if(l.dimension().equals(Level.OVERWORLD)||l.dimension().equals(Level.NETHER)||l.dimension().equals(Level.END)) {
				hasSeparateSeed=false;
				FHMain.LOGGER.info("using vanilla seed for level "+l.dimension().location().toString()+":"+seed);
			}else this.generateSeed(l, originseed);
			
		}
	}
	public void setSeed(long seed) {
		this.seed=seed;
		hasSeparateSeed=true;
		FHMain.LOGGER.info("seed set:"+seed);
	}
	public boolean hasSeed() {
		return hasSeparateSeed;
	}

	public DimensionalSeed(long seed, boolean hasSeparateSeed, boolean isInitialized) {
		super();
		this.seed = seed;
		this.hasSeparateSeed = hasSeparateSeed;
		this.isInitialized = isInitialized;
	}

}
