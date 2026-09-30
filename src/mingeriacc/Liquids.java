package mingeriacc;

/**
 * Water and lava. Each tile holds 0..255 units of one liquid. Liquid falls
 * down, then evens out sideways; lava moves only every few steps. Where water
 * and lava meet, the lava turns into obsidian. Only tiles that may still move
 * are kept in the work lists, so settled lakes cost nothing.
 */
final class Liquids {
    static final int WATER = 0, LAVA = 1;
    static final int FULL = 255;

    private final World w;
    private final boolean[] queued;
    private int[] water = new int[1024], lava = new int[1024];
    private int nWater, nLava;
    private int[] work = new int[1024];
    /** Set when water and lava met (the game plays a hiss). */
    int hissX = -1, hissY;

    Liquids(World w) {
        this.w = w;
        queued = new boolean[w.w * w.h];
    }

    private int amt(int i) {
        return w.liquid[i] & 0xff;
    }

    /** Puts a tile with liquid into the work list. */
    void activate(int x, int y) {
        if (!w.inside(x, y)) return;
        int i = x + y * w.w;
        if (queued[i] || w.liquid[i] == 0) return;
        queued[i] = true;
        if (w.lqType[i] == LAVA) {
            if (nLava == lava.length) lava = java.util.Arrays.copyOf(lava, nLava * 2);
            lava[nLava++] = i;
        } else {
            if (nWater == water.length) water = java.util.Arrays.copyOf(water, nWater * 2);
            water[nWater++] = i;
        }
    }

    void activateAround(int x, int y) {
        activate(x, y);
        activate(x - 1, y);
        activate(x + 1, y);
        activate(x, y - 1);
        activate(x, y + 1);
    }

    /** Activates every tile holding liquid (after loading or world generation). */
    void activateAll() {
        for (int y = 0; y < w.h; y++)
            for (int x = 0; x < w.w; x++) if (w.liquid[x + y * w.w] != 0) activate(x, y);
    }

    /** Sets liquid in a tile (0 removes it). */
    void set(int x, int y, int amount, int type) {
        if (!w.inside(x, y)) return;
        int i = x + y * w.w;
        w.liquid[i] = (byte) Math.max(0, Math.min(FULL, amount));
        w.lqType[i] = (byte) type;
        activateAround(x, y);
    }

    boolean busy() {
        return nWater > 0 || nLava > 0;
    }

    /** One simulation step. budget = most tiles handled (the rest wait for later steps). */
    void step(int budget, boolean lavaTurn) {
        stepList(true, budget);
        if (lavaTurn) stepList(false, budget / 2);
    }

    private void stepList(boolean isWater, int budget) {
        int n = isWater ? nWater : nLava;
        if (n == 0) return;
        int take = Math.min(n, budget);
        int[] list = isWater ? water : lava;
        if (work.length < take) work = new int[Math.max(take, work.length * 2)];
        // take from the end so newly added tiles wait until the next step
        System.arraycopy(list, n - take, work, 0, take);
        if (isWater) nWater -= take;
        else nLava -= take;
        for (int k = 0; k < take; k++) queued[work[k]] = false;
        for (int k = 0; k < take; k++) flow(work[k]);
    }

    private boolean open(int x, int y) {
        return w.inside(x, y) && !Tiles.SOLID[w.tile(x, y)];
    }

    /** Water meets lava: the lava tile becomes obsidian. */
    private void harden(int lavaIndex) {
        int x = lavaIndex % w.w, y = lavaIndex / w.w;
        boolean enough = amt(lavaIndex) >= 32;
        w.liquid[lavaIndex] = 0;
        if (enough && Tiles.PLACE_OVER[w.tile(x, y)]) w.set(x, y, Tiles.OBSIDIAN);
        else activateAround(x, y);
        hissX = x;
        hissY = y;
    }

    private void flow(int i) {
        int a = amt(i);
        if (a == 0) return;
        int x = i % w.w, y = i / w.w;
        int type = w.lqType[i];
        boolean changed = false;

        // fall
        if (open(x, y + 1)) {
            int b = i + w.w;
            int ab = amt(b);
            if (ab > 0 && w.lqType[b] != type) {
                harden(type == LAVA ? i : b);
                if (type == LAVA) return;
                changed = true;
            } else if (ab < FULL) {
                int move = Math.min(a, FULL - ab);
                w.liquid[b] = (byte) (ab + move);
                w.lqType[b] = (byte) type;
                a -= move;
                changed = true;
                activate(x, y + 1);
            }
        }

        // spread sideways when resting on something
        if (a > 0) {
            boolean lOpen = open(x - 1, y), rOpen = open(x + 1, y);
            int li = i - 1, ri = i + 1;
            if (lOpen && amt(li) > 0 && w.lqType[li] != type) {
                harden(type == LAVA ? i : li);
                if (type == LAVA) return;
                lOpen = false;
            }
            if (rOpen && amt(ri) > 0 && w.lqType[ri] != type) {
                harden(type == LAVA ? i : ri);
                if (type == LAVA) return;
                rOpen = false;
            }
            int sum = a, n = 1;
            int la = lOpen ? amt(li) : 0, ra = rOpen ? amt(ri) : 0;
            if (lOpen) { sum += la; n++; }
            if (rOpen) { sum += ra; n++; }
            if (n > 1) {
                int avg = sum / n, rem = sum - avg * n;
                int tol = type == LAVA ? 4 : 1;
                boolean differs = (lOpen && Math.abs(la - avg) > tol) || (rOpen && Math.abs(ra - avg) > tol)
                        || Math.abs(a - avg) > tol;
                if (differs) {
                    a = avg + (rem > 0 ? 1 : 0);
                    rem = Math.max(0, rem - 1);
                    if (lOpen) {
                        w.liquid[li] = (byte) (avg + (rem > 0 ? 1 : 0));
                        w.lqType[li] = (byte) type;
                        rem = Math.max(0, rem - 1);
                        activate(x - 1, y);
                    }
                    if (rOpen) {
                        w.liquid[ri] = (byte) (avg + (rem > 0 ? 1 : 0));
                        w.lqType[ri] = (byte) type;
                        activate(x + 1, y);
                    }
                    changed = true;
                }
            }
            // thin films dry up
            if (a > 0 && a < 3 && !open(x, y + 1)) {
                a = 0;
                changed = true;
            }
        }

        if (changed) {
            w.liquid[i] = (byte) a;
            activateAround(x, y);
        }
    }

    /** Runs the simulation until everything has settled (used by world generation). */
    void settle(int maxSteps) {
        activateAll();
        for (int s = 0; s < maxSteps && busy(); s++) step(Integer.MAX_VALUE, true);
    }
}
