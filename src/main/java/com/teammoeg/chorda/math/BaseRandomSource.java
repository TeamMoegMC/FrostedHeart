package com.teammoeg.chorda.math;

import java.util.Random;

import com.google.common.annotations.VisibleForTesting;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.RandomSupport;

public class BaseRandomSource implements RandomSource {
	public static class BaseRandomFactory implements PositionalRandomFactory {
		private final long seed;

		public BaseRandomFactory(long pSeed) {
			this.seed = pSeed;
		}

		public RandomSource at(int pX, int pY, int pZ) {
			return new BaseRandomSource(seed ^ Mth.getSeed(pX, pY, pZ));
		}

		public RandomSource fromHashOf(String pName) {
			RandomSupport.Seed128bit randomsupport$seed128bit = RandomSupport.seedFromHashOf(pName);
			return new BaseRandomSource(randomsupport$seed128bit.seedHi() ^ randomsupport$seed128bit.seedLo() ^ seed);
		}

		@VisibleForTesting
		public void parityConfigString(StringBuilder pBuilder) {
			pBuilder.append("seed: ").append(this.seed);
		}
	}

	Random rnd;

	public BaseRandomSource() {
		this(new Random());
	}

	public BaseRandomSource(Random rnd) {
		super();
		this.rnd = rnd;
	}

	public BaseRandomSource(long seed) {
		this(new Random(seed));
	}

	@Override
	public RandomSource fork() {

		return new BaseRandomSource(rnd.nextLong());
	}

	@Override
	public PositionalRandomFactory forkPositional() {

		return new BaseRandomFactory(rnd.nextLong());
	}

	@Override
	public void setSeed(long pSeed) {
		rnd.setSeed(pSeed);
	}

	@Override
	public double nextGaussian() {
		return rnd.nextGaussian();
	}

	@Override
	public int nextInt() {

		return rnd.nextInt();
	}

	@Override
	public int nextInt(int pBound) {
		return rnd.nextInt(pBound);
	}

	@Override
	public long nextLong() {

		return rnd.nextLong();
	}

	@Override
	public boolean nextBoolean() {

		return rnd.nextBoolean();
	}

	@Override
	public float nextFloat() {

		return rnd.nextFloat();
	}

	@Override
	public double nextDouble() {

		return rnd.nextDouble();
	}

}
