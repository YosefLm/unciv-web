package org.teavm.classlib.java.util.concurrent;

import org.teavm.classlib.java.util.TRandom;

/** Web-only replacement for TeaVM's seed-rejecting ThreadLocalRandom. */
public final class TThreadLocalRandom extends TRandom {
    private static final TThreadLocalRandom CURRENT = new TThreadLocalRandom();

    private TThreadLocalRandom() {
        super();
    }

    public static TThreadLocalRandom current() {
        return CURRENT;
    }

    @Override
    public int nextInt() {
        return (int) (nextDouble() * 4294967296.0 - 2147483648.0);
    }

    public int nextInt(int bound) {
        if (bound <= 0) throw new IllegalArgumentException();
        return (int) (nextDouble() * bound);
    }

    @Override
    public long nextLong() {
        return ((long) nextInt() << 32) | (nextInt() & 0xffffffffL);
    }

    @Override
    public float nextFloat() {
        return (float) nextDouble();
    }

    @Override
    public double nextDouble() {
        return Math.random();
    }

    @Override
    public void setSeed(long seed) {
        // Browser randomness is intentionally not seedable.
    }
}
