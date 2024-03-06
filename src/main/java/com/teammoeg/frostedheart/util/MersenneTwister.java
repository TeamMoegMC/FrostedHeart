package com.teammoeg.frostedheart.util;
public class MersenneTwister extends RandomBase{

    /** Serializable version identifier. */
    private static final long serialVersionUID = 8661194735290153518L;

    /** Size of the bytes pool. */
    private static final int   N     = 624;

    /** Period second parameter. */
    private static final int   M     = 397;

    /** X * MATRIX_A for X = {0, 1}. */
    private static final int[] MAG01 = { 0x0, 0x9908b0df };

    /** Bytes pool. */
    private int[] mt;

    /** Current index in the bytes pool. */
    private int   mti;

    /** Creates a new random number generator.
     * <p>The instance is initialized using the current time plus the
     * system identity hash code of this instance as the seed.</p>
     */
    public MersenneTwister() {
        mt = new int[N];
        setSeed(System.currentTimeMillis() + System.identityHashCode(this));
    }

    /** Creates a new random number generator using a single int seed.
     * @param seed the initial seed (32 bits integer)
     */
    public MersenneTwister(int seed) {
        mt = new int[N];
        setSeed(seed);
    }

    /** Creates a new random number generator using an int array seed.
     * @param seed the initial seed (32 bits integers array), if null
     * the seed of the generator will be related to the current time
     */
    public MersenneTwister(int[] seed) {
        mt = new int[N];
        setSeed(seed);
    }

    /** Creates a new random number generator using a single long seed.
     * @param seed the initial seed (64 bits integer)
     */
    public MersenneTwister(long seed) {
        mt = new int[N];
        setSeed(seed);
    }

    /** Reinitialize the generator as if just built with the given int seed.
     * <p>The state of the generator is exactly the same as a new
     * generator built with the same seed.</p>
     * @param seed the initial seed (32 bits integer)
     */
    @Override
    public void setSeed(int seed) {
        // we use a long masked by 0xffffffffL as a poor man unsigned int
        long longMT = seed;
        // NB: unlike original C code, we are working with java longs, the cast below makes masking unnecessary
        mt[0]= (int) longMT;
        for (mti = 1; mti < N; ++mti) {
            // See Knuth TAOCP Vol2. 3rd Ed. P.106 for multiplier.
            // initializer from the 2002-01-09 C version by Makoto Matsumoto
            longMT = (1812433253l * (longMT ^ (longMT >> 30)) + mti) & 0xffffffffL;
            mt[mti]= (int) longMT;
        }

        clear(); // Clear normal deviate cache
    }

    /** Reinitialize the generator as if just built with the given int array seed.
     * <p>The state of the generator is exactly the same as a new
     * generator built with the same seed.</p>
     * @param seed the initial seed (32 bits integers array), if null
     * the seed of the generator will be the current system time plus the
     * system identity hash code of this instance
     */
    @Override
    public void setSeed(int[] seed) {

        if (seed == null) {
            setSeed(System.currentTimeMillis() + System.identityHashCode(this));
            return;
        }

        setSeed(19650218);
        int i = 1;
        int j = 0;

        for (int k = Math.max(N, seed.length); k != 0; k--) {
            long l0 = (mt[i] & 0x7fffffffl)   | ((mt[i]   < 0) ? 0x80000000l : 0x0l);
            long l1 = (mt[i-1] & 0x7fffffffl) | ((mt[i-1] < 0) ? 0x80000000l : 0x0l);
            long l  = (l0 ^ ((l1 ^ (l1 >> 30)) * 1664525l)) + seed[j] + j; // non linear
            mt[i]   = (int) (l & 0xffffffffl);
            i++; j++;
            if (i >= N) {
                mt[0] = mt[N - 1];
                i = 1;
            }
            if (j >= seed.length) {
                j = 0;
            }
        }

        for (int k = N - 1; k != 0; k--) {
            long l0 = (mt[i] & 0x7fffffffl)   | ((mt[i]   < 0) ? 0x80000000l : 0x0l);
            long l1 = (mt[i-1] & 0x7fffffffl) | ((mt[i-1] < 0) ? 0x80000000l : 0x0l);
            long l  = (l0 ^ ((l1 ^ (l1 >> 30)) * 1566083941l)) - i; // non linear
            mt[i]   = (int) (l & 0xffffffffL);
            i++;
            if (i >= N) {
                mt[0] = mt[N - 1];
                i = 1;
            }
        }

        mt[0] = 0x80000000; // MSB is 1; assuring non-zero initial array

        clear(); // Clear normal deviate cache

    }

    /** Reinitialize the generator as if just built with the given long seed.
     * <p>The state of the generator is exactly the same as a new
     * generator built with the same seed.</p>
     * @param seed the initial seed (64 bits integer)
     */
    @Override
    public void setSeed(long seed) {
        setSeed(new int[] { (int) (seed >>> 32), (int) (seed & 0xffffffffl) });
    }

    /** Generate next pseudorandom number.
     * <p>This method is the core generation algorithm. It is used by all the
     * public generation methods for the various primitive types {@link
     * #nextBoolean()}, {@link #nextBytes(byte[])}, {@link #nextDouble()},
     * {@link #nextFloat()}, {@link #nextGaussian()}, {@link #nextInt()},
     * {@link #next(int)} and {@link #nextLong()}.</p>
     * @param bits number of random bits to produce
     * @return random bits generated
     */
    @Override
    protected int next(int bits) {

        int y;

        if (mti >= N) { // generate N words at one time
            int mtNext = mt[0];
            for (int k = 0; k < N - M; ++k) {
                int mtCurr = mtNext;
                mtNext = mt[k + 1];
                y = (mtCurr & 0x80000000) | (mtNext & 0x7fffffff);
                mt[k] = mt[k + M] ^ (y >>> 1) ^ MAG01[y & 0x1];
            }
            for (int k = N - M; k < N - 1; ++k) {
                int mtCurr = mtNext;
                mtNext = mt[k + 1];
                y = (mtCurr & 0x80000000) | (mtNext & 0x7fffffff);
                mt[k] = mt[k + (M - N)] ^ (y >>> 1) ^ MAG01[y & 0x1];
            }
            y = (mtNext & 0x80000000) | (mt[0] & 0x7fffffff);
            mt[N - 1] = mt[M - 1] ^ (y >>> 1) ^ MAG01[y & 0x1];

            mti = 0;
        }

        y = mt[mti++];

        // tempering
        y ^=  y >>> 11;
        y ^= (y <<   7) & 0x9d2c5680;
        y ^= (y <<  15) & 0xefc60000;
        y ^=  y >>> 18;

        return y >>> (32 - bits);

    }

}