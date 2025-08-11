package com.jake.sharedmemory;

/**
 * Utility class for Euler's Totient function computations.
 */
public class Totient {

    /**
     * Compute the greatest common divisor (GCD) of x and y using the Euclidean algorithm.
     */
    public static long gcd(long x, long y) {
        while (y != 0) {
            long t = x % y;
            x = y;
            y = t;
        }
        return Math.abs(x);
    }

    /**
     * Compute Euler's Totient function φ(n).
     * Counts the integers less than n that are coprime with n.
     */
    public static long euler(long n) {
        long count = 0;
        for (long i = 1; i < n; i++) {
            if (gcd(n, i) == 1) count++;
        }
        return count;
    }
}
