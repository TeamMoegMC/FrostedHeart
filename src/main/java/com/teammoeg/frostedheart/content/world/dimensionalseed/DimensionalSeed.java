package com.teammoeg.frostedheart.content.world.dimensionalseed;

import java.nio.charset.StandardCharsets;

import com.google.common.hash.Hashing;
import com.teammoeg.chorda.io.NBTSerializable;
import com.teammoeg.frostedheart.FHMain;

import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeManager;

public class DimensionalSeed implements NBTSerializable {
	@Getter
	private long seed;
	private boolean hasSeparateSeed;
	private boolean isInitialized;
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
				this.seed=originseed;
				hasSeparateSeed=true;
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
	@Override
	public void save(CompoundTag nbt, boolean isPacket) {
		nbt.putLong("seed", seed);
		nbt.putBoolean("hasSeed", hasSeparateSeed);
		nbt.putBoolean("init", isInitialized);
		
	}
	@Override
	public void load(CompoundTag nbt, boolean isPacket) {
		seed=nbt.getLong("seed");
		hasSeparateSeed=nbt.getBoolean("hasSeed");
		isInitialized=nbt.getBoolean("init");
	}

}
