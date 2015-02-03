package com.google.refine.util;

/**
 * Created by hejian on 2/3/15.
 */
public class MathUtils {
    public static long power(int base, int exp) {
        long power = 1;
        for (int i = 0; i < exp; ++i)
            power *= base;
        return power;
    }

    public static boolean isSubMask(long mask1, long mask2) {
        return (mask1 & mask2) == mask1;
    }
}
