package com.teammoeg.chorda.math;

import java.util.Random;

import com.google.common.annotations.VisibleForTesting;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.RandomSupport;

/**
 * 基于Java标准{@link Random}的Minecraft {@link RandomSource}实现。
 * 提供与Minecraft世界生成兼容的随机数接口。
 * <p>
 * A Minecraft {@link RandomSource} implementation based on Java's standard {@link Random}.
 * Provides a random number interface compatible with Minecraft world generation.
 */
public class BaseRandomSource implements RandomSource {
	/**
	 * 基于位置的随机数工厂，用于根据世界坐标生成确定性的随机源。
	 * <p>
	 * Positional random factory used to generate deterministic random sources based on world coordinates.
	 */
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

	/**
	 * 使用默认随机种子构造。
	 * <p>
	 * Constructs with a default random seed.
	 */
	public BaseRandomSource() {
		this(new Random());
	}

	/**
	 * 使用指定的Random实例构造。
	 * <p>
	 * Constructs with the specified Random instance.
	 *
	 * @param rnd 底层随机数生成器 / the underlying random number generator
	 */
	public BaseRandomSource(Random rnd) {
		super();
		this.rnd = rnd;
	}

	/**
	 * 使用指定的长整型种子构造。
	 * <p>
	 * Constructs with the specified long seed.
	 *
	 * @param seed 随机种子 / the random seed
	 */
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
