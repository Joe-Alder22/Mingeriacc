package mingeriacc;

import java.util.Random;

/** Seeded Perlin noise for world generation and backgrounds. */
final class Noise {
    private final int[] perm = new int[512];

    Noise(long seed) {
        Random rnd = new Random(seed);
        int[] p = new int[256];
        for (int i = 0; i < 256; i++) p[i] = i;
        for (int i = 255; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            int t = p[i]; p[i] = p[j]; p[j] = t;
        }
        for (int i = 0; i < 512; i++) perm[i] = p[i & 255];
    }

    private static double fade(double t) { return t * t * t * (t * (t * 6 - 15) + 10); }

    private static double grad(int h, double x, double y) {
        switch (h & 7) {
            case 0: return x + y;
            case 1: return -x + y;
            case 2: return x - y;
            case 3: return -x - y;
            case 4: return x;
            case 5: return -x;
            case 6: return y;
            default: return -y;
        }
    }

    /** 2D noise, values roughly -1..1. */
    double noise(double x, double y) {
        int xi = (int) Math.floor(x), yi = (int) Math.floor(y);
        double xf = x - xi, yf = y - yi;
        xi &= 255; yi &= 255;
        double u = fade(xf), v = fade(yf);
        int aa = perm[perm[xi] + yi], ab = perm[perm[xi] + yi + 1];
        int ba = perm[perm[xi + 1] + yi], bb = perm[perm[xi + 1] + yi + 1];
        double x1 = lerp(grad(aa, xf, yf), grad(ba, xf - 1, yf), u);
        double x2 = lerp(grad(ab, xf, yf - 1), grad(bb, xf - 1, yf - 1), u);
        return lerp(x1, x2, v);
    }

    /** 1D noise (a slice of the 2D noise). */
    double noise(double x) {
        return noise(x, 0.5);
    }

    /** Fractal noise, octaves summed. Values roughly -1..1. */
    double fractal(double x, double y, int octaves) {
        double sum = 0, amp = 1, norm = 0, f = 1;
        for (int i = 0; i < octaves; i++) {
            sum += noise(x * f + i * 17.3, y * f + i * 31.7) * amp;
            norm += amp;
            amp *= 0.5;
            f *= 2;
        }
        return sum / norm;
    }

    double fractal(double x, int octaves) {
        return fractal(x, 0.5, octaves);
    }

    private static double lerp(double a, double b, double t) { return a + (b - a) * t; }

    /** Fast integer hash (e.g. for tile texture variants). */
    static int hash(int x, int y) {
        int h = x * 374761393 + y * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        return h ^ (h >>> 16);
    }
}
